package com.opus.dispute.management.service;

import com.opus.dispute.management.entity.PolicyDocument;
import com.opus.dispute.management.repository.PolicyDocumentRepository;
import com.opus.dispute.management.service.agent.PolicyIntelligenceAgent;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class PolicyDocumentService {

    private static final String TYPE_MERCHANT = "MERCHANT";
    private static final String TYPE_NETWORK = "NETWORK";

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final PolicyDocumentRepository repository;
    private final PolicyIntelligenceAgent policyAgent;

    public PolicyDocumentService(PolicyDocumentRepository repository, PolicyIntelligenceAgent policyAgent) {
        this.repository = repository;
        this.policyAgent = policyAgent;
    }

    public Map<String, Object> uploadMerchantPolicy(MultipartFile file) {
        String text = extractText(file);
        Optional<PolicyDocument> previousOpt = repository.findTopByPolicyTypeOrderByVersionDesc(TYPE_MERCHANT);
        int nextVersion = previousOpt.map(p -> p.getVersion() + 1).orElse(1);

        PolicyDocument doc = new PolicyDocument(TYPE_MERCHANT, nextVersion, text, file.getOriginalFilename());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("policyType", TYPE_MERCHANT);
        result.put("version", nextVersion);
        result.put("filename", file.getOriginalFilename());
        result.put("contentLength", text.length());

        if (previousOpt.isPresent()) {
            String previousText = previousOpt.get().getContent();
            log.info("Diffing merchant policy v{} against v{}", nextVersion, previousOpt.get().getVersion());
            String diff = policyAgent.analyzePolicyDiff(previousText, text);
            doc.setDiffSummary(diff);
            result.put("hasPreviousVersion", true);
            result.put("previousVersion", previousOpt.get().getVersion());
            result.put("diffSummary", diff);
        } else {
            result.put("hasPreviousVersion", false);
            result.put("diffSummary", null);
        }

        log.info("Generating settings recommendations from merchant policy v{}", nextVersion);
        String recommendations = policyAgent.analyzeMerchantPolicy(text);
        doc.setSettingsRecommendations(recommendations);

        Object parsedRecs;
        try {
            parsedRecs = new com.google.gson.Gson().fromJson(recommendations, Object.class);
        } catch (Exception e) {
            log.warn("Could not parse merchant recommendations as JSON");
            parsedRecs = recommendations;
        }
        result.put("settingsRecommendations", parsedRecs);

        repository.save(doc);
        log.info("Stored merchant policy v{} (id={})", nextVersion, doc.getId());
        result.put("id", doc.getId());

        return result;
    }

    public Map<String, Object> uploadNetworkPolicy(MultipartFile file, String networkName) {
        String text = extractText(file);
        String qualifiedType = TYPE_NETWORK + ":" + networkName.toUpperCase();
        Optional<PolicyDocument> previousOpt = repository.findTopByPolicyTypeOrderByVersionDesc(qualifiedType);
        int nextVersion = previousOpt.map(p -> p.getVersion() + 1).orElse(1);

        PolicyDocument doc = new PolicyDocument(qualifiedType, nextVersion, text, file.getOriginalFilename());
        doc.setNetworkName(networkName);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("policyType", "NETWORK");
        result.put("networkName", networkName);
        result.put("version", nextVersion);
        result.put("filename", file.getOriginalFilename());
        result.put("contentLength", text.length());

        if (previousOpt.isPresent()) {
            String previousText = previousOpt.get().getContent();
            log.info("Diffing {} network policy v{} against v{}", networkName, nextVersion, previousOpt.get().getVersion());
            String diff = policyAgent.analyzePolicyDiff(previousText, text);
            doc.setDiffSummary(diff);
            result.put("hasPreviousVersion", true);
            result.put("previousVersion", previousOpt.get().getVersion());
            result.put("diffSummary", diff);
        } else {
            log.info("First {} network policy upload, performing initial analysis", networkName);
            String analysis = policyAgent.analyzePolicy(text, networkName);
            doc.setDiffSummary(analysis);
            result.put("hasPreviousVersion", false);
            result.put("initialAnalysis", analysis);
        }

        repository.save(doc);
        log.info("Stored {} network policy v{} (id={})", networkName, nextVersion, doc.getId());
        result.put("id", doc.getId());

        return result;
    }

    public List<Map<String, Object>> getMerchantHistory() {
        return toHistoryList(repository.findByPolicyTypeOrderByVersionDesc(TYPE_MERCHANT));
    }

    public List<Map<String, Object>> getNetworkHistory(String networkName) {
        String qualifiedType = TYPE_NETWORK + ":" + networkName.toUpperCase();
        return toHistoryList(repository.findByPolicyTypeOrderByVersionDesc(qualifiedType));
    }

    public Optional<PolicyDocument> getMerchantLatest() {
        return repository.findTopByPolicyTypeOrderByVersionDesc(TYPE_MERCHANT);
    }

    public Optional<PolicyDocument> getNetworkLatest(String networkName) {
        String qualifiedType = TYPE_NETWORK + ":" + networkName.toUpperCase();
        return repository.findTopByPolicyTypeOrderByVersionDesc(qualifiedType);
    }

    public List<String> getDistinctNetworkNames() {
        return repository.findDistinctNetworkNames();
    }

    public Optional<PolicyDocument> getById(Long id) {
        return repository.findById(id);
    }

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or missing");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File too large: " + (file.getSize() / 1024 / 1024) + "MB. Maximum is 10MB");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        if (filename.endsWith(".txt") || "text/plain".equals(contentType)) {
            return extractPlainText(file);
        } else if (filename.endsWith(".pdf") || "application/pdf".equals(contentType)) {
            return extractPdfText(file);
        } else if (filename.endsWith(".docx") ||
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            return extractDocxText(file);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + contentType + ". Supported: .txt, .pdf, .docx");
        }
    }

    private String extractPlainText(MultipartFile file) {
        try {
            return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read text file: " + e.getMessage(), e);
        }
    }

    private String extractPdfText(MultipartFile file) {
        try (InputStream is = file.getInputStream(); PDDocument pdf = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdf);
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("PDF contains no extractable text (may be image-based)");
            }
            log.info("Extracted {} chars from PDF ({} pages)", text.length(), pdf.getNumberOfPages());
            return text;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }

    private String extractDocxText(MultipartFile file) {
        try (InputStream is = file.getInputStream(); XWPFDocument docx = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : docx.getParagraphs()) {
                String text = para.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }
            String result = sb.toString().trim();
            if (result.isEmpty()) {
                throw new IllegalArgumentException("DOCX contains no extractable text");
            }
            log.info("Extracted {} chars from DOCX", result.length());
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from DOCX: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> toHistoryList(List<PolicyDocument> docs) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (PolicyDocument doc : docs) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", doc.getId());
            entry.put("version", doc.getVersion());
            entry.put("filename", doc.getFilename());
            entry.put("networkName", doc.getNetworkName());
            entry.put("contentLength", doc.getContent().length());
            entry.put("hasDiff", doc.getDiffSummary() != null);
            entry.put("hasRecommendations", doc.getSettingsRecommendations() != null);
            entry.put("uploadedAt", doc.getUploadedAt().toString());
            result.add(entry);
        }
        return result;
    }
}
