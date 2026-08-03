package com.opus.dispute.management.service;

import com.opus.dispute.management.service.agent.CaseSummarizerAgent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SummarizationQueueService {

    private static final int MAX_QUEUE_CAPACITY = 500;
    private static final int MAX_TRACKED_JOBS = 2000;

    private final CaseSummarizerAgent caseSummarizerAgent;
    private final LinkedBlockingQueue<Long> queue = new LinkedBlockingQueue<>(MAX_QUEUE_CAPACITY);
    private final ConcurrentHashMap<Long, JobStatus> jobStatuses = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "summarization-worker");
        t.setDaemon(true);
        return t;
    });

    public SummarizationQueueService(CaseSummarizerAgent caseSummarizerAgent) {
        this.caseSummarizerAgent = caseSummarizerAgent;
        executor.submit(this::processLoop);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public JobStatus enqueue(Long disputeId) {
        JobStatus result = jobStatuses.compute(disputeId, (id, existing) -> {
            if (existing != null && ("QUEUED".equals(existing.getStatus()) || "PROCESSING".equals(existing.getStatus()))) {
                return existing;
            }
            return new JobStatus(id, "QUEUED", null, null, Instant.now());
        });

        if ("QUEUED".equals(result.getStatus()) && result.getStartedAt() == null) {
            boolean offered = queue.offer(disputeId);
            if (!offered) {
                result.setStatus("FAILED");
                result.setError("Queue is full (capacity: " + MAX_QUEUE_CAPACITY + "). Try again later.");
                result.setCompletedAt(Instant.now());
                log.warn("Queue full, rejected dispute {}", disputeId);
            } else {
                log.info("Enqueued dispute {} for summarization. Queue depth: {}", disputeId, queue.size());
            }
        }

        evictOldJobs();
        return result;
    }

    public List<JobStatus> enqueueBatch(List<Long> disputeIds) {
        List<JobStatus> results = new ArrayList<>();
        for (Long id : disputeIds) {
            results.add(enqueue(id));
        }
        return results;
    }

    public JobStatus getJobStatus(Long disputeId) {
        return jobStatuses.get(disputeId);
    }

    public Map<String, Object> getQueueStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("queueDepth", queue.size());

        long queued = 0, processing = 0, completed = 0, failed = 0;
        for (JobStatus job : jobStatuses.values()) {
            switch (job.getStatus()) {
                case "QUEUED" -> queued++;
                case "PROCESSING" -> processing++;
                case "COMPLETED" -> completed++;
                case "FAILED" -> failed++;
            }
        }
        status.put("queued", queued);
        status.put("processing", processing);
        status.put("completed", completed);
        status.put("failed", failed);
        status.put("totalTracked", jobStatuses.size());
        status.put("maxQueueCapacity", MAX_QUEUE_CAPACITY);
        return status;
    }

    private void processLoop() {
        log.info("Summarization queue worker started");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Long disputeId = queue.take();
                JobStatus job = jobStatuses.get(disputeId);
                if (job == null) continue;

                job.setStatus("PROCESSING");
                job.setStartedAt(Instant.now());
                log.info("Processing summarization for dispute {}. Remaining in queue: {}", disputeId, queue.size());

                try {
                    String summary = caseSummarizerAgent.summarize(disputeId);
                    job.setStatus("COMPLETED");
                    job.setResult(summary);
                    job.setCompletedAt(Instant.now());
                    log.info("Summarization completed for dispute {}", disputeId);
                } catch (Exception e) {
                    job.setStatus("FAILED");
                    job.setError(e.getMessage());
                    job.setCompletedAt(Instant.now());
                    log.error("Summarization failed for dispute {}", disputeId, e);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Summarization queue worker interrupted, shutting down");
                break;
            }
        }
    }

    private void evictOldJobs() {
        if (jobStatuses.size() <= MAX_TRACKED_JOBS) return;

        jobStatuses.entrySet().removeIf(entry -> {
            String s = entry.getValue().getStatus();
            return ("COMPLETED".equals(s) || "FAILED".equals(s));
        });
    }

    public static class JobStatus {
        private final Long disputeId;
        private volatile String status;
        private volatile String result;
        private volatile String error;
        private final Instant queuedAt;
        private volatile Instant startedAt;
        private volatile Instant completedAt;

        public JobStatus(Long disputeId, String status, String result, String error, Instant queuedAt) {
            this.disputeId = disputeId;
            this.status = status;
            this.result = result;
            this.error = error;
            this.queuedAt = queuedAt;
        }

        public Long getDisputeId() { return disputeId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public Instant getQueuedAt() { return queuedAt; }
        public Instant getStartedAt() { return startedAt; }
        public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
        public Instant getCompletedAt() { return completedAt; }
        public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    }
}
