package com.opus.dispute.management.service.agent;

import com.opus.dispute.management.service.AppConfigService;
import com.opus.dispute.management.service.GeminiService;
import com.opus.dispute.management.service.PiiScrubber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class PolicyIntelligenceAgent {

    private static final String SHORT_DIFF_INSTRUCTION = """
            
            RESPONSE LENGTH: SHORT MODE is active.
            - Keep the entire response under 300 words.
            - Only list High-Impact changes in detail (1-2 lines each).
            - Summarize Medium/Low changes in one line total.
            - Limit "Suggested Configuration Updates" to top 3.
            - No filler or verbose explanations.
            """;

    private static final String SHORT_ANALYSIS_INSTRUCTION = """
            
            RESPONSE LENGTH: SHORT MODE is active.
            - Keep the entire response under 400 words.
            - Deadlines: top 5 most critical only.
            - Evidence requirements: top 5 reason codes only.
            - Skip "Common pitfalls" unless critical.
            - No filler — rules and actions only.
            """;

    private static final String DIFF_SYSTEM_PROMPT = """
            You are the Policy Intelligence Agent (Agent 5) in a dispute management system.
            Your role is to analyze card network policy documents and identify changes that
            impact dispute handling.
            
            When comparing two versions of a policy document:
            1. Identify all meaningful changes (additions, removals, modifications)
            2. Explain each change in plain language
            3. Assess operational impact on dispute handling
            4. Suggest configuration changes the merchant may need to make
            5. Prioritize changes by impact (HIGH, MEDIUM, LOW)
            
            Structure your response:
            - Summary of Changes (count and overview)
            - High-Impact Changes (with explanation and recommended actions)
            - Medium-Impact Changes
            - Low-Impact Changes
            - Suggested Configuration Updates
              - For each: what setting to review and why
            
            Be specific and actionable. Merchants need to know what to DO, not just what changed.
            
            LANGUAGE RULES — CRITICAL:
            - Write EVERYTHING in plain, non-technical language that any business person can understand.
            - NEVER mention file names, file extensions, folder paths, or internal system terms.
            - Avoid developer/system language like "file", "data folder", "disk", "parsed", "payload", etc.
            """;

    private static final String ANALYSIS_SYSTEM_PROMPT = """
            You are the Policy Intelligence Agent (Agent 5) in a dispute management system.
            Your role is to analyze a card network policy document and extract key rules,
            deadlines, evidence requirements, and procedures relevant to dispute handling.
            
            Provide:
            1. Key deadlines and SLAs by reason code category
            2. Required evidence types by reason code
            3. Important procedural rules
            4. Common pitfalls and requirements merchants often miss
            5. Any special conditions or exceptions
            
            Structure as a clear reference guide that merchants and the AI agents can use.
            
            LANGUAGE RULES — CRITICAL:
            - Write EVERYTHING in plain, non-technical language that any business person can understand.
            - NEVER mention file names, file extensions, folder paths, or internal system terms.
            - Avoid developer/system language like "file", "data folder", "disk", "parsed", "payload", etc.
            """;

    private static final String MERCHANT_POLICY_SYSTEM_PROMPT = """
            You are the Policy Intelligence Agent (Agent 5) in a dispute management system.
            A merchant has uploaded their internal policy document. Your role is to analyze it
            and recommend specific system settings changes to align the platform with their policy.
            
            The system has the following configurable settings:
            
            URGENCY & HIGHLIGHTING:
            - "urgency.enabled" (true/false): Whether to show urgency indicators on disputes
            - "urgency.critical-days" (number, 0-90): Days remaining before deadline to show critical/red highlighting (default: 2)
            - "urgency.warning-days" (number, 0-90): Days remaining before deadline to show warning/yellow highlighting (default: 5)
            - "urgency.buffer-days" (number, 0-90): Treat deadlines as N days earlier than network deadline
              (e.g., if set to 1, a dispute due on Jan 10 will be treated as due Jan 9 for highlighting purposes)
            
            AUTOMATION & PIPELINE:
            - "pipeline.auto-enrich" (true/false): Automatically run AI analysis on new disputes
            - "auto-accept.enabled" (true/false): Automatically accept disputes matching certain rules
            - "ingestion.auto-sync.enabled" (true/false): Automatically sync disputes from Mastercard
            - "ingestion.auto-sync.frequency" ("once_daily" or "twice_daily"): How often to sync
            
            TAB VISIBILITY:
            - "hide-tabs.pre-chargeback-alerts" (true/false): Hide the Pre-Chargeback Alerts tab
            - "hide-tabs.policy-diff" (true/false): Hide the Policy Diff tab
            - "hide-tabs.analytics-insights" (true/false): Hide the Analytics Insights tab
            - "hide-tabs.integrations" (true/false): Hide the Integrations tab
            - "hide-tabs.business-profile" (true/false): Hide the Business Profile tab
            - "hide-tabs.data-points" (true/false): Hide the Data Points tab
            
            You must return a JSON object with this structure:
            {
              "summary": "Brief summary of the merchant's policy and its implications",
              "recommendations": [
                {
                  "settingKey": "the exact setting key from the list above",
                  "currentValue": "the current value provided to you",
                  "recommendedValue": "the new value you recommend",
                  "reason": "Why this change is needed based on the policy",
                  "policyExcerpt": "The specific part of the policy that drives this recommendation",
                  "impact": "HIGH|MEDIUM|LOW"
                }
              ],
              "additionalNotes": "Any observations about the policy that don't map to specific settings"
            }
            
            Rules:
            - Only recommend changes where the current value differs from what the policy suggests
            - Be precise with setting keys — they must match exactly
            - For deadline offset: if the policy says to treat deadlines as N days earlier, set deadline-offset-days to N
            - For automation preferences: map phrases like "maximum automation" to enabling auto-enrich, auto-sync, etc.
            - For tab visibility: "true" means HIDDEN, "false" means VISIBLE
            - Always include the reason tied to the specific policy language
            
            LANGUAGE RULES — CRITICAL:
            - Write EVERYTHING in plain, non-technical language that any business person can understand.
            - NEVER mention file names, file extensions, folder paths, or internal system terms.
            - Avoid developer/system language like "file", "data folder", "disk", "parsed", "payload", etc.
            """;

    private final GeminiService geminiService;
    private final PiiScrubber piiScrubber;
    private final AppConfigService appConfigService;

    public PolicyIntelligenceAgent(GeminiService geminiService, PiiScrubber piiScrubber,
                                    AppConfigService appConfigService) {
        this.geminiService = geminiService;
        this.piiScrubber = piiScrubber;
        this.appConfigService = appConfigService;
    }

    public String analyzePolicyDiff(String previousPolicy, String newPolicy) {
        log.info("Analyzing policy diff");

        String userPrompt = "=== PREVIOUS POLICY VERSION ===\n" + piiScrubber.scrub(previousPolicy) +
                "\n\n=== NEW POLICY VERSION ===\n" + piiScrubber.scrub(newPolicy) +
                "\n\nAnalyze the differences between these two policy versions.";

        try {
            String diffPrompt = appConfigService.isShortResponseMode()
                    ? DIFF_SYSTEM_PROMPT + SHORT_DIFF_INSTRUCTION
                    : DIFF_SYSTEM_PROMPT;
            String analysis = geminiService.generateContent(diffPrompt, userPrompt);
            log.info("Policy diff analysis completed");
            return analysis;
        } catch (Exception e) {
            log.error("Policy diff analysis failed", e);
            throw new RuntimeException("Policy diff analysis failed: " + e.getMessage(), e);
        }
    }

    public String analyzePolicy(String policyDocument, String networkName) {
        log.info("Analyzing {} policy document", networkName);

        String userPrompt = "Network: " + networkName + "\n\n" +
                "=== POLICY DOCUMENT ===\n" + piiScrubber.scrub(policyDocument) +
                "\n\nExtract all relevant dispute handling rules, deadlines, and evidence requirements.";

        try {
            String analysisPrompt = appConfigService.isShortResponseMode()
                    ? ANALYSIS_SYSTEM_PROMPT + SHORT_ANALYSIS_INSTRUCTION
                    : ANALYSIS_SYSTEM_PROMPT;
            String analysis = geminiService.generateContent(analysisPrompt, userPrompt);
            log.info("{} policy analysis completed", networkName);
            return analysis;
        } catch (Exception e) {
            log.error("Policy analysis failed for {}", networkName, e);
            throw new RuntimeException("Policy analysis failed: " + e.getMessage(), e);
        }
    }

    public String analyzeMerchantPolicy(String policyDocument) {
        log.info("Analyzing merchant policy document for settings recommendations");

        Map<String, String> currentSettings = appConfigService.getAllSettings();

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("=== CURRENT SYSTEM SETTINGS ===\n");
        for (Map.Entry<String, String> entry : currentSettings.entrySet()) {
            userPrompt.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
        userPrompt.append("\n=== MERCHANT POLICY DOCUMENT ===\n");
        userPrompt.append(piiScrubber.scrub(policyDocument));
        userPrompt.append("\n\nAnalyze this merchant policy and recommend settings changes.");

        try {
            String analysis = geminiService.generateJson(MERCHANT_POLICY_SYSTEM_PROMPT, userPrompt.toString());
            log.info("Merchant policy analysis completed with settings recommendations");
            return analysis;
        } catch (Exception e) {
            log.error("Merchant policy analysis failed", e);
            throw new RuntimeException("Merchant policy analysis failed: " + e.getMessage(), e);
        }
    }
}
