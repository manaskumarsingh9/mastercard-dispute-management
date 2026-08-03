package com.opus.dispute.management.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DataSourceService {

    private static final Path DATA_ROOT = Paths.get("src/data");
    private static final Path SOURCES_ROOT = DATA_ROOT.resolve("sources");
    private static final Path ISSUER_ROOT = SOURCES_ROOT.resolve("issuer");
    private static final Path ACQUIRER_ROOT = SOURCES_ROOT.resolve("acquirer");
    private static final Path REASON_CODE_RULES_PATH = DATA_ROOT.resolve("reason-code-rules.json");

    private static final String[] SOURCE_CATEGORIES = {
            "customer-comms", "device", "fraud-tools", "identity", "merchant", "psp", "shipping"
    };

    public String loadReasonCodeRules() {
        try {
            if (Files.exists(REASON_CODE_RULES_PATH)) {
                return Files.readString(REASON_CODE_RULES_PATH);
            }
        } catch (IOException e) {
            log.error("Failed to read reason-code-rules.json", e);
        }
        return null;
    }

    public JsonObject getReasonCodeRule(String reasonCode) {
        String rulesJson = loadReasonCodeRules();
        if (rulesJson == null || reasonCode == null) return null;

        try {
            JsonObject rules = JsonParser.parseString(rulesJson).getAsJsonObject();
            if (rules.has(reasonCode)) {
                return rules.getAsJsonObject(reasonCode);
            }
        } catch (Exception e) {
            log.error("Failed to parse reason code rules for code {}", reasonCode, e);
        }
        return null;
    }

    public Map<String, String> loadIssuerSourcesForCase(int caseNumber) {
        return loadSourcesFromRoot(ISSUER_ROOT, caseNumber, "issuer");
    }

    public Map<String, String> loadAcquirerSourcesForCase(int caseNumber) {
        return loadSourcesFromRoot(ACQUIRER_ROOT, caseNumber, "acquirer");
    }

    public Map<String, String> loadIssuerSourcesForReasonCode(String reasonCode) {
        return loadSourcesFromReasonCodeDir(ISSUER_ROOT, reasonCode, "issuer");
    }

    public Map<String, String> loadAcquirerSourcesForReasonCode(String reasonCode) {
        return loadSourcesFromReasonCodeDir(ACQUIRER_ROOT, reasonCode, "acquirer");
    }

    public Map<String, String> loadAllSourcesForCase(int caseNumber) {
        Map<String, String> all = new LinkedHashMap<>();
        Map<String, String> issuer = loadIssuerSourcesForCase(caseNumber);
        for (Map.Entry<String, String> entry : issuer.entrySet()) {
            all.put("issuer/" + entry.getKey(), entry.getValue());
        }
        Map<String, String> acquirer = loadAcquirerSourcesForCase(caseNumber);
        for (Map.Entry<String, String> entry : acquirer.entrySet()) {
            all.put("acquirer/" + entry.getKey(), entry.getValue());
        }
        log.info("Loaded {} total source files for case {} ({} issuer, {} acquirer)",
                all.size(), caseNumber, issuer.size(), acquirer.size());
        return all;
    }

    public Map<String, String> loadAllSourcesForReasonCode(String reasonCode) {
        Map<String, String> all = new LinkedHashMap<>();
        Map<String, String> issuer = loadIssuerSourcesForReasonCode(reasonCode);
        for (Map.Entry<String, String> entry : issuer.entrySet()) {
            all.put("issuer/" + entry.getKey(), entry.getValue());
        }
        Map<String, String> acquirer = loadAcquirerSourcesForReasonCode(reasonCode);
        for (Map.Entry<String, String> entry : acquirer.entrySet()) {
            all.put("acquirer/" + entry.getKey(), entry.getValue());
        }
        log.info("Loaded {} total source files for reason code {} ({} issuer, {} acquirer)",
                all.size(), reasonCode, issuer.size(), acquirer.size());
        return all;
    }

    public Map<String, String> loadIssuerSourcesWithFallback(int caseNumber, String reasonCode) {
        Map<String, String> sources = new LinkedHashMap<>();
        if (reasonCode != null && !reasonCode.isEmpty()) {
            sources.putAll(loadIssuerSourcesForReasonCode(reasonCode));
        }
        Map<String, String> caseSources = loadIssuerSourcesForCase(caseNumber);
        sources.putAll(caseSources);
        if (!caseSources.isEmpty() && !sources.isEmpty()) {
            log.info("Merged {} reason-code + {} case-specific issuer files for case {}", sources.size() - caseSources.size(), caseSources.size(), caseNumber);
        } else if (caseSources.isEmpty() && !sources.isEmpty()) {
            log.info("No case-specific issuer files for case {}; using {} reason-code files from {}", caseNumber, sources.size(), reasonCode);
        }
        return sources;
    }

    public Map<String, String> loadAcquirerSourcesWithFallback(int caseNumber, String reasonCode) {
        Map<String, String> sources = new LinkedHashMap<>();
        if (reasonCode != null && !reasonCode.isEmpty()) {
            sources.putAll(loadAcquirerSourcesForReasonCode(reasonCode));
        }
        Map<String, String> caseSources = loadAcquirerSourcesForCase(caseNumber);
        sources.putAll(caseSources);
        if (!caseSources.isEmpty() && !sources.isEmpty()) {
            log.info("Merged {} reason-code + {} case-specific acquirer files for case {}", sources.size() - caseSources.size(), caseSources.size(), caseNumber);
        }
        return sources;
    }

    public Map<String, String> loadAllSourcesWithFallback(int caseNumber, String reasonCode) {
        Map<String, String> all = new LinkedHashMap<>();
        Map<String, String> issuer = loadIssuerSourcesWithFallback(caseNumber, reasonCode);
        for (Map.Entry<String, String> entry : issuer.entrySet()) {
            all.put("issuer/" + entry.getKey(), entry.getValue());
        }
        Map<String, String> acquirer = loadAcquirerSourcesWithFallback(caseNumber, reasonCode);
        for (Map.Entry<String, String> entry : acquirer.entrySet()) {
            all.put("acquirer/" + entry.getKey(), entry.getValue());
        }
        log.info("Loaded {} total source files for case {} / reason code {} ({} issuer, {} acquirer)",
                all.size(), caseNumber, reasonCode, issuer.size(), acquirer.size());
        return all;
    }

    private Map<String, String> loadSourcesFromReasonCodeDir(Path root, String reasonCode, String label) {
        Map<String, String> sources = new LinkedHashMap<>();
        if (!isValidReasonCode(reasonCode)) {
            log.warn("Invalid reason code format: '{}' — skipping directory scan", reasonCode);
            return sources;
        }
        Path rcDir = root.resolve(reasonCode);
        if (!Files.isDirectory(rcDir)) {
            log.debug("No reason-code directory for {} / {}", label, reasonCode);
            return sources;
        }

        try (DirectoryStream<Path> catStream = Files.newDirectoryStream(rcDir)) {
            for (Path catDir : catStream) {
                if (!Files.isDirectory(catDir)) continue;
                String category = catDir.getFileName().toString();

                try (DirectoryStream<Path> fileStream = Files.newDirectoryStream(catDir, "*.json")) {
                    for (Path file : fileStream) {
                        try {
                            String content = Files.readString(file);
                            String fileType = file.getFileName().toString().replace(".json", "");
                            String key = category + "/" + fileType;
                            sources.put(key, content);
                            log.debug("Loaded {} reason-code source: {}/{}/{}", label, reasonCode, category, fileType);
                        } catch (IOException e) {
                            log.warn("Failed to read reason-code source file: {}", file, e);
                        }
                    }
                } catch (IOException e) {
                    log.warn("Failed to scan reason-code category dir: {}", catDir, e);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to scan reason-code dir: {}", rcDir, e);
        }

        log.debug("Loaded {} {} source files from reason code {}", sources.size(), label, reasonCode);
        return sources;
    }

    private Map<String, String> loadSourcesFromRoot(Path root, int caseNumber, String label) {
        Map<String, String> sources = new LinkedHashMap<>();

        for (String category : SOURCE_CATEGORIES) {
            Path categoryDir = root.resolve(category);
            if (!Files.isDirectory(categoryDir)) continue;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(categoryDir, "*_case_" + caseNumber + ".json")) {
                for (Path file : stream) {
                    try {
                        String content = Files.readString(file);
                        String fileType = file.getFileName().toString()
                                .replace("_case_" + caseNumber + ".json", "");
                        String key = category + "/" + fileType;
                        sources.put(key, content);
                        log.debug("Loaded {} source file: {}", label, key);
                    } catch (IOException e) {
                        log.warn("Failed to read source file: {}", file, e);
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to scan directory: {}", categoryDir, e);
            }
        }

        return sources;
    }

    public String loadSourceFile(String side, String category, String fileType, int caseNumber) {
        Path root = "issuer".equals(side) ? ISSUER_ROOT : ACQUIRER_ROOT;
        Path file = root.resolve(category).resolve(fileType + "_case_" + caseNumber + ".json");
        try {
            if (Files.exists(file)) {
                return Files.readString(file);
            }
        } catch (IOException e) {
            log.warn("Failed to read source file: {}", file, e);
        }
        return null;
    }

    public Map<String, String> loadSourcesByCategory(String side, String category, int caseNumber) {
        Map<String, String> sources = new LinkedHashMap<>();
        Path root = "issuer".equals(side) ? ISSUER_ROOT : ACQUIRER_ROOT;
        Path categoryDir = root.resolve(category);
        if (!Files.isDirectory(categoryDir)) return sources;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(categoryDir, "*_case_" + caseNumber + ".json")) {
            for (Path file : stream) {
                try {
                    String content = Files.readString(file);
                    String fileType = file.getFileName().toString()
                            .replace("_case_" + caseNumber + ".json", "");
                    sources.put(fileType, content);
                } catch (IOException e) {
                    log.warn("Failed to read: {}", file, e);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to scan: {}", categoryDir, e);
        }
        return sources;
    }

    public boolean hasSourcesForCase(int caseNumber) {
        return hasSourcesInRoot(ACQUIRER_ROOT, caseNumber) || hasSourcesInRoot(ISSUER_ROOT, caseNumber);
    }

    private static final String[] ALL_SOURCE_EXTENSIONS = {
            "json", "pdf", "png", "jpg", "jpeg", "gif", "webp"
    };

    private boolean hasSourcesInRoot(Path root, int caseNumber) {
        for (String category : SOURCE_CATEGORIES) {
            Path categoryDir = root.resolve(category);
            if (!Files.isDirectory(categoryDir)) continue;

            for (String ext : ALL_SOURCE_EXTENSIONS) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(categoryDir, "*_case_" + caseNumber + "." + ext)) {
                    if (stream.iterator().hasNext()) return true;
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return false;
    }

    public int extractCaseNumber(Long disputeId, String claimId) {
        return extractCaseNumber(disputeId, claimId, null);
    }

    public int extractCaseNumber(Long disputeId, String claimId, Integer storedCaseNumber) {
        if (storedCaseNumber != null && storedCaseNumber > 0) {
            if (hasSourcesForCase(storedCaseNumber)) {
                log.debug("Matched case number {} from stored caseNumber field", storedCaseNumber);
                return storedCaseNumber;
            }
        }

        if (disputeId != null) {
            int id = disputeId.intValue();
            if (hasSourcesForCase(id)) {
                log.debug("Matched case number {} from dispute DB id", id);
                return id;
            }
        }

        if (claimId != null && !claimId.isEmpty()) {
            int lastHyphen = claimId.lastIndexOf('-');
            if (lastHyphen >= 0 && lastHyphen < claimId.length() - 1) {
                try {
                    int suffix = Integer.parseInt(claimId.substring(lastHyphen + 1));
                    if (hasSourcesForCase(suffix)) {
                        log.debug("Matched case number {} from claimId suffix of '{}'", suffix, claimId);
                        return suffix;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        log.debug("No case data files found for disputeId={}, claimId={}", disputeId, claimId);
        return -1;
    }

    private static final String[] MEDIA_EXTENSIONS = {"pdf", "png", "jpg", "jpeg", "gif", "webp"};

    public List<MediaFile> loadIssuerMediaForCase(int caseNumber) {
        return loadMediaFromRoot(ISSUER_ROOT, caseNumber, "issuer");
    }

    public List<MediaFile> loadAcquirerMediaForCase(int caseNumber) {
        return loadMediaFromRoot(ACQUIRER_ROOT, caseNumber, "acquirer");
    }

    public List<MediaFile> loadIssuerMediaForReasonCode(String reasonCode) {
        return loadMediaFromReasonCodeDir(ISSUER_ROOT, reasonCode, "issuer");
    }

    public List<MediaFile> loadAcquirerMediaForReasonCode(String reasonCode) {
        return loadMediaFromReasonCodeDir(ACQUIRER_ROOT, reasonCode, "acquirer");
    }

    public List<MediaFile> loadIssuerMediaWithFallback(int caseNumber, String reasonCode) {
        List<MediaFile> media = new ArrayList<>();
        if (reasonCode != null && !reasonCode.isEmpty()) {
            media.addAll(loadIssuerMediaForReasonCode(reasonCode));
        }
        List<MediaFile> caseMedia = loadIssuerMediaForCase(caseNumber);
        media.addAll(caseMedia);
        return media;
    }

    public List<MediaFile> loadAcquirerMediaWithFallback(int caseNumber, String reasonCode) {
        List<MediaFile> media = new ArrayList<>();
        if (reasonCode != null && !reasonCode.isEmpty()) {
            media.addAll(loadAcquirerMediaForReasonCode(reasonCode));
        }
        List<MediaFile> caseMedia = loadAcquirerMediaForCase(caseNumber);
        media.addAll(caseMedia);
        return media;
    }

    private boolean isValidReasonCode(String reasonCode) {
        if (reasonCode == null || reasonCode.isEmpty()) return false;
        return reasonCode.matches("^[0-9]{4,6}$");
    }

    private List<MediaFile> loadMediaFromRoot(Path root, int caseNumber, String label) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        for (String category : SOURCE_CATEGORIES) {
            Path categoryDir = root.resolve(category);
            if (!Files.isDirectory(categoryDir)) continue;

            for (String ext : MEDIA_EXTENSIONS) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(categoryDir, "*_case_" + caseNumber + "." + ext)) {
                    for (Path file : stream) {
                        try {
                            byte[] data = Files.readAllBytes(file);
                            String mimeType = MediaFile.mimeTypeForExtension(ext);
                            String name = category + "/" + file.getFileName().toString();
                            mediaFiles.add(new MediaFile(name, mimeType, data));
                            log.debug("Loaded {} media file: {} ({}, {} bytes)", label, name, mimeType, data.length);
                        } catch (IOException e) {
                            log.warn("Failed to read media file: {}", file, e);
                        }
                    }
                } catch (IOException e) {
                    log.warn("Failed to scan media in directory: {}", categoryDir, e);
                }
            }
        }
        return mediaFiles;
    }

    private List<MediaFile> loadMediaFromReasonCodeDir(Path root, String reasonCode, String label) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        if (!isValidReasonCode(reasonCode)) return mediaFiles;
        Path rcDir = root.resolve(reasonCode);
        if (!Files.isDirectory(rcDir)) return mediaFiles;

        try (DirectoryStream<Path> catStream = Files.newDirectoryStream(rcDir)) {
            for (Path catDir : catStream) {
                if (!Files.isDirectory(catDir)) continue;
                String category = catDir.getFileName().toString();

                for (String ext : MEDIA_EXTENSIONS) {
                    try (DirectoryStream<Path> fileStream = Files.newDirectoryStream(catDir, "*." + ext)) {
                        for (Path file : fileStream) {
                            try {
                                byte[] data = Files.readAllBytes(file);
                                String mimeType = MediaFile.mimeTypeForExtension(ext);
                                String name = category + "/" + file.getFileName().toString();
                                mediaFiles.add(new MediaFile(name, mimeType, data));
                                log.debug("Loaded {} reason-code media: {}/{}", label, reasonCode, name);
                            } catch (IOException e) {
                                log.warn("Failed to read media file: {}", file, e);
                            }
                        }
                    } catch (IOException e) {
                        log.warn("Failed to scan media in dir: {}", catDir, e);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to scan reason-code dir for media: {}", rcDir, e);
        }

        log.debug("Loaded {} {} media files from reason code {}", mediaFiles.size(), label, reasonCode);
        return mediaFiles;
    }

    public int[] getAvailableCaseRange() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        Path[] roots = {ACQUIRER_ROOT, ISSUER_ROOT};
        for (Path root : roots) {
            for (String category : SOURCE_CATEGORIES) {
                Path categoryDir = root.resolve(category);
                if (!Files.isDirectory(categoryDir)) continue;

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(categoryDir, "*_case_*.json")) {
                    for (Path file : stream) {
                        String name = file.getFileName().toString();
                        int idx = name.lastIndexOf("_case_");
                        if (idx >= 0) {
                            String numStr = name.substring(idx + 6).replace(".json", "");
                            try {
                                int num = Integer.parseInt(numStr);
                                min = Math.min(min, num);
                                max = Math.max(max, num);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                } catch (IOException ignored) {}
            }
        }

        if (min == Integer.MAX_VALUE) return new int[]{0, 0};
        return new int[]{min, max};
    }
}
