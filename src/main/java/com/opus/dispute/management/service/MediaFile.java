package com.opus.dispute.management.service;

import java.util.Base64;
import java.util.Set;

public record MediaFile(String name, String mimeType, byte[] data) {

    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp"
    );

    private static final Set<String> SUPPORTED_PDF_TYPES = Set.of(
            "application/pdf"
    );

    public String base64Data() {
        return Base64.getEncoder().encodeToString(data);
    }

    public boolean isImage() {
        return SUPPORTED_IMAGE_TYPES.contains(mimeType);
    }

    public boolean isPdf() {
        return SUPPORTED_PDF_TYPES.contains(mimeType);
    }

    public static String mimeTypeForExtension(String ext) {
        return switch (ext.toLowerCase()) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            default -> null;
        };
    }

    public static boolean isSupportedExtension(String ext) {
        return mimeTypeForExtension(ext) != null;
    }
}
