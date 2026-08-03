package com.opus.dispute.management.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opus.dispute.management.entity.AutoAcceptRule;
import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.AutoAcceptRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AutoAcceptService {

    private final AutoAcceptRuleRepository ruleRepo;
    private final AppConfigService configService;
    private final Gson gson = new Gson();

    public AutoAcceptService(AutoAcceptRuleRepository ruleRepo, AppConfigService configService) {
        this.ruleRepo = ruleRepo;
        this.configService = configService;
    }

    public boolean isEnabled() {
        String val = configService.getSetting("auto-accept.enabled");
        return "true".equals(val);
    }

    public boolean shouldAutoAccept(Dispute dispute) {
        if (!isEnabled()) {
            return false;
        }

        List<AutoAcceptRule> rules = ruleRepo.findByEnabledTrueOrderByPriorityAsc();
        for (AutoAcceptRule rule : rules) {
            try {
                if (matchesAllConditions(rule.getConditions(), dispute)) {
                    log.info("Dispute {} auto-accepted by rule '{}' ({})", dispute.getId(), rule.getName(), rule.getId());
                    return true;
                }
            } catch (Exception e) {
                log.warn("Error evaluating auto-accept rule '{}': {}", rule.getId(), e.getMessage());
            }
        }
        return false;
    }

    private boolean matchesAllConditions(String conditionsJson, Dispute d) {
        if (conditionsJson == null || conditionsJson.isBlank()) {
            return false;
        }

        JsonObject cond = JsonParser.parseString(conditionsJson).getAsJsonObject();

        if (cond.has("amountBelow") && d.getAmount() != null) {
            double threshold = cond.get("amountBelow").getAsDouble();
            if (d.getAmount() >= threshold) return false;
        }

        if (cond.has("amountAbove") && d.getAmount() != null) {
            double threshold = cond.get("amountAbove").getAsDouble();
            if (d.getAmount() <= threshold) return false;
        }

        if (cond.has("reasonCodes")) {
            JsonArray codes = cond.getAsJsonArray("reasonCodes");
            if (codes != null && codes.size() > 0 && d.getReasonCode() != null) {
                boolean found = false;
                for (JsonElement code : codes) {
                    if (code.getAsString().equals(d.getReasonCode())) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
        }

        if (cond.has("disputeTypes")) {
            JsonArray types = cond.getAsJsonArray("disputeTypes");
            if (types != null && types.size() > 0 && d.getDisputeType() != null) {
                boolean found = false;
                for (JsonElement type : types) {
                    if (type.getAsString().equalsIgnoreCase(d.getDisputeType())) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
        }

        if (cond.has("merchantNames")) {
            JsonArray names = cond.getAsJsonArray("merchantNames");
            if (names != null && names.size() > 0 && d.getMerchantName() != null) {
                boolean found = false;
                String dMerchant = d.getMerchantName().toLowerCase();
                for (JsonElement name : names) {
                    if (dMerchant.contains(name.getAsString().toLowerCase())) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
        }

        if (cond.has("ageAboveDays") && d.getIngestedAt() != null) {
            int threshold = cond.get("ageAboveDays").getAsInt();
            long daysOld = ChronoUnit.DAYS.between(d.getIngestedAt().toLocalDate(), LocalDate.now());
            if (daysOld <= threshold) return false;
        }

        return true;
    }

    public Map<String, Object> getRulesConfig() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", isEnabled());
        List<Map<String, Object>> rulesList = new ArrayList<>();
        for (AutoAcceptRule rule : ruleRepo.findAll()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", rule.getId());
            r.put("name", rule.getName());
            r.put("description", rule.getDescription());
            r.put("enabled", rule.getEnabled());
            r.put("priority", rule.getPriority());
            try {
                r.put("conditions", gson.fromJson(rule.getConditions(), Map.class));
            } catch (Exception e) {
                r.put("conditions", rule.getConditions());
            }
            r.put("createdAt", rule.getCreatedAt() != null ? rule.getCreatedAt().toString() : null);
            r.put("updatedAt", rule.getUpdatedAt() != null ? rule.getUpdatedAt().toString() : null);
            rulesList.add(r);
        }
        result.put("rules", rulesList);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> updateRulesConfig(Map<String, Object> config) {
        if (config.containsKey("enabled")) {
            String enabledVal = String.valueOf(config.get("enabled"));
            configService.updateSettings(Map.of("auto-accept.enabled", enabledVal));
            log.info("Auto-accept engine enabled: {}", enabledVal);
        }

        if (config.containsKey("rules")) {
            List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("rules");
            List<String> incomingIds = new ArrayList<>();

            for (Map<String, Object> ruleData : rules) {
                String id = ruleData.get("id") != null ? ruleData.get("id").toString() : UUID.randomUUID().toString();
                incomingIds.add(id);

                AutoAcceptRule rule = ruleRepo.findById(id).orElse(new AutoAcceptRule());
                rule.setId(id);
                rule.setName(ruleData.getOrDefault("name", "Unnamed Rule").toString());
                rule.setDescription(ruleData.get("description") != null ? ruleData.get("description").toString() : null);
                rule.setEnabled(ruleData.get("enabled") != null ? Boolean.parseBoolean(ruleData.get("enabled").toString()) : true);
                rule.setPriority(ruleData.get("priority") != null ? Integer.parseInt(ruleData.get("priority").toString()) : 999);

                Object conditions = ruleData.get("conditions");
                rule.setConditions(conditions != null ? gson.toJson(conditions) : "{}");

                if (rule.getCreatedAt() == null) {
                    rule.setCreatedAt(LocalDateTime.now());
                }
                rule.setUpdatedAt(LocalDateTime.now());

                ruleRepo.save(rule);
            }

            List<AutoAcceptRule> existing = ruleRepo.findAll();
            for (AutoAcceptRule r : existing) {
                if (!incomingIds.contains(r.getId())) {
                    ruleRepo.delete(r);
                }
            }
        }

        return getRulesConfig();
    }
}
