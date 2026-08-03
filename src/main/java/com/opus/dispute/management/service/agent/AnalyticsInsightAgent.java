package com.opus.dispute.management.service.agent;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.DisputeRepository;
import com.opus.dispute.management.service.AppConfigService;
import com.opus.dispute.management.service.GeminiService;
import com.opus.dispute.management.service.PiiScrubber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AnalyticsInsightAgent {

    private static final String SHORT_MODE_INSTRUCTION = """
            
            RESPONSE LENGTH: SHORT MODE is active.
            - Keep the entire response under 300 words.
            - Key Metrics: 3-5 bullet points max.
            - Trend Analysis: 2-3 sentences.
            - Reason Code Insights: top 3 only.
            - Recommendations: top 3, one line each.
            - Skip "Risk Alerts" unless critical.
            - No filler — data and actions only.
            """;

    private static final String SYSTEM_PROMPT = """
            You are the Analytics Insight Agent (Agent 6) in a dispute management system.
            Your role is to analyze dispute data and provide actionable insights.
            
            Based on the dispute statistics provided, generate insights covering:
            1. Dispute volume trends and patterns
            2. Most common reason codes and what they indicate
            3. Merchant performance observations
            4. Queue distribution analysis
            5. Risk indicators and early warnings
            6. Actionable recommendations to reduce disputes
            
            Structure your response:
            - Key Metrics Overview
            - Trend Analysis
            - Reason Code Insights (top reason codes and what merchants should do)
            - Risk Alerts (if any patterns suggest growing problems)
            - Recommendations (specific, actionable steps)
            
            Be data-driven. Reference specific numbers from the statistics.
            Keep insights practical and actionable for merchants.
            
            LANGUAGE RULES — CRITICAL:
            - Write EVERYTHING in plain, non-technical language that any business person can understand.
            - NEVER mention file names, file extensions (.json, .pdf, .csv), folder paths, data sources,
              or internal system terms in your response.
            - Avoid developer/system language like "file", "data folder", "disk", "parsed", "payload", etc.
            """;

    private final GeminiService geminiService;
    private final PiiScrubber piiScrubber;
    private final DisputeRepository disputeRepository;
    private final AppConfigService appConfigService;

    public AnalyticsInsightAgent(GeminiService geminiService, PiiScrubber piiScrubber,
                                  DisputeRepository disputeRepository,
                                  AppConfigService appConfigService) {
        this.geminiService = geminiService;
        this.piiScrubber = piiScrubber;
        this.disputeRepository = disputeRepository;
        this.appConfigService = appConfigService;
    }

    private String getEffectivePrompt() {
        return appConfigService.isShortResponseMode()
                ? SYSTEM_PROMPT + SHORT_MODE_INSTRUCTION
                : SYSTEM_PROMPT;
    }

    public String generateInsights() {
        log.info("Generating analytics insights");

        try {
            String statisticsContext = buildStatisticsContext();
            String insights = geminiService.generateContent(getEffectivePrompt(), statisticsContext);
            log.info("Analytics insights generated");
            return insights;
        } catch (Exception e) {
            log.error("Failed to generate analytics insights", e);
            throw new RuntimeException("Analytics insights generation failed: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalDisputes = disputeRepository.count();
        stats.put("totalDisputes", totalDisputes);

        List<Dispute> recentDisputes = disputeRepository.findAll(PageRequest.of(0, 1000)).getContent();

        Map<String, Integer> byReasonCode = new HashMap<>();
        Map<String, Integer> byQueue = new HashMap<>();
        Map<String, Integer> byStatus = new HashMap<>();
        Map<String, Integer> byNetwork = new HashMap<>();
        int openCount = 0;
        int closedCount = 0;

        for (Dispute d : recentDisputes) {
            if (d.getReasonCode() != null) {
                byReasonCode.merge(d.getReasonCode(), 1, Integer::sum);
            }
            if (d.getQueueName() != null) {
                byQueue.merge(d.getQueueName(), 1, Integer::sum);
            }
            if (d.getStatus() != null) {
                byStatus.merge(d.getStatus(), 1, Integer::sum);
            }
            if (d.getClearingNetwork() != null) {
                byNetwork.merge(d.getClearingNetwork(), 1, Integer::sum);
            }
            if (Boolean.TRUE.equals(d.getIsOpen())) {
                openCount++;
            } else {
                closedCount++;
            }
        }

        stats.put("byReasonCode", byReasonCode);
        stats.put("byQueue", byQueue);
        stats.put("byStatus", byStatus);
        stats.put("byNetwork", byNetwork);
        stats.put("openDisputes", openCount);
        stats.put("closedDisputes", closedCount);
        stats.put("sampleSize", recentDisputes.size());

        return stats;
    }

    private String buildStatisticsContext() {
        Map<String, Object> stats = getStatistics();
        StringBuilder context = new StringBuilder();

        context.append("=== DISPUTE ANALYTICS DATA ===\n\n");
        context.append("Total Disputes in System: ").append(stats.get("totalDisputes")).append("\n");
        context.append("Sample Size for Analysis: ").append(stats.get("sampleSize")).append("\n");
        context.append("Open Disputes: ").append(stats.get("openDisputes")).append("\n");
        context.append("Closed Disputes: ").append(stats.get("closedDisputes")).append("\n\n");

        context.append("--- Disputes by Reason Code ---\n");
        appendMap(context, (Map<String, Integer>) stats.get("byReasonCode"));

        context.append("\n--- Disputes by Queue ---\n");
        appendMap(context, (Map<String, Integer>) stats.get("byQueue"));

        context.append("\n--- Disputes by Status ---\n");
        appendMap(context, (Map<String, Integer>) stats.get("byStatus"));

        context.append("\n--- Disputes by Network ---\n");
        appendMap(context, (Map<String, Integer>) stats.get("byNetwork"));

        return context.toString();
    }

    private void appendMap(StringBuilder sb, Map<String, Integer> map) {
        if (map == null || map.isEmpty()) {
            sb.append("  No data available\n");
            return;
        }
        map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));
    }
}
