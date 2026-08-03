package com.opus.dispute.management.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class ScheduledIngestionTask {

    private final ClaimIngestionService claimIngestionService;
    private final PostIngestionPipelineService postIngestionPipelineService;
    private final AppConfigService configService;

    private TaskScheduler taskScheduler;
    private final List<ScheduledFuture<?>> scheduledJobs = new ArrayList<>();

    private volatile boolean currentEnabled = false;
    private volatile String currentFrequency = "";

    private static final DateTimeFormatter LOG_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    public ScheduledIngestionTask(ClaimIngestionService claimIngestionService,
                                   PostIngestionPipelineService postIngestionPipelineService,
                                   AppConfigService configService) {
        this.claimIngestionService = claimIngestionService;
        this.postIngestionPipelineService = postIngestionPipelineService;
        this.configService = configService;
    }

    @PostConstruct
    public void init() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("auto-sync-");
        scheduler.initialize();
        this.taskScheduler = scheduler;

        applySchedule();
        log.info("ScheduledIngestionTask initialized");
    }

    @PreDestroy
    public void shutdown() {
        cancelAll();
        if (taskScheduler instanceof ThreadPoolTaskScheduler tpts) {
            tpts.shutdown();
        }
    }

    public synchronized void reschedule() {
        applySchedule();
    }

    public synchronized ScheduleInfo getScheduleInfo() {
        boolean enabled = configService.isAutoSyncEnabled();
        String frequency = configService.getAutoSyncFrequency();

        ScheduleInfo info = new ScheduleInfo();
        info.enabled = enabled;
        info.frequency = frequency;
        info.activeJobs = scheduledJobs.size();

        if (enabled) {
            if ("twice_daily".equals(frequency)) {
                info.nextRuns = List.of("00:00 IST (midnight)", "12:00 IST (noon)");
                info.description = "Cases are fetched twice daily at midnight (00:00 IST) and noon (12:00 IST). " +
                        "Each run covers from the last successful fetch to the current time with no gaps.";
            } else {
                info.nextRuns = List.of("00:00 IST (midnight)");
                info.description = "Cases are fetched once daily at midnight (00:00 IST). " +
                        "Each run covers from the last successful fetch to the current time with no gaps.";
            }
        } else {
            info.nextRuns = List.of();
            info.description = "Automatic sync is disabled. Use the 'Sync from Mastercard' button on the dashboard for manual fetching.";
        }

        return info;
    }

    private void applySchedule() {
        boolean enabled = configService.isAutoSyncEnabled();
        String frequency = configService.getAutoSyncFrequency();

        if (enabled == currentEnabled && frequency.equals(currentFrequency)) {
            log.debug("Auto-sync schedule unchanged (enabled={}, frequency={}), skipping reschedule", enabled, frequency);
            return;
        }

        cancelAll();
        currentEnabled = enabled;
        currentFrequency = frequency;

        if (!enabled) {
            log.info("Auto-sync disabled — all scheduled ingestion jobs cancelled");
            return;
        }

        scheduledJobs.add(taskScheduler.schedule(this::runIngestion, new CronTrigger("0 0 0 * * *", java.util.TimeZone.getTimeZone(IST_ZONE))));
        log.info("Scheduled auto-sync at midnight (00:00 IST / 18:30 UTC)");

        if ("twice_daily".equals(frequency)) {
            scheduledJobs.add(taskScheduler.schedule(this::runIngestion, new CronTrigger("0 0 12 * * *", java.util.TimeZone.getTimeZone(IST_ZONE))));
            log.info("Scheduled auto-sync at noon (12:00 IST / 06:30 UTC)");
        }

        log.info("Auto-sync schedule applied: frequency={}, jobs={}", frequency, scheduledJobs.size());
    }

    private void cancelAll() {
        for (ScheduledFuture<?> job : scheduledJobs) {
            job.cancel(false);
        }
        scheduledJobs.clear();
    }

    private void runIngestion() {
        if (!configService.isAutoSyncEnabled()) {
            log.info("Auto-sync was disabled between schedule and execution, skipping");
            return;
        }

        LocalDateTime startTime = LocalDateTime.now();
        log.info("Auto-sync ingestion starting at {}", startTime.format(LOG_FORMAT));

        try {
            ClaimIngestionService.IngestionResult result = claimIngestionService.ingestScheduled();

            log.info("Auto-sync ingestion complete at {}: {} new, {} updated, {} skipped, {} capped, {} errors",
                    LocalDateTime.now().format(LOG_FORMAT),
                    result.newClaims, result.updatedClaims, result.skipped, result.capped, result.errors.size());

            if (!result.errors.isEmpty()) {
                result.errors.forEach(e -> log.warn("Auto-sync ingestion error: {}", e));
            }

            if (!result.newDisputeIds.isEmpty()) {
                postIngestionPipelineService.runPostIngestionHook(result.newDisputeIds);
                log.info("Post-ingestion pipeline triggered for {} new disputes", result.newDisputeIds.size());
            }
        } catch (Exception e) {
            log.error("Auto-sync ingestion failed at {}", LocalDateTime.now().format(LOG_FORMAT), e);
        }
    }

    public static class ScheduleInfo {
        public boolean enabled;
        public String frequency;
        public int activeJobs;
        public List<String> nextRuns;
        public String description;
    }
}
