package com.rentmybike.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.rentmybike.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Service for uploading and deleting images on Cloudinary.
 * Service zum Hochladen und Löschen von Bildern auf Cloudinary.
 *
 * <p>Architecture: Client → Backend → Cloudinary CDN → URL stored in DB.
 * <p>Architektur: Client → Backend → Cloudinary CDN → URL in DB gespeichert.
 * The backend never stores raw image bytes on disk.
 * Das Backend speichert niemals rohe Bild-Bytes auf der Festplatte.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // Max file size: 5MB / Maximale Dateigröße: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Allowed image types / Erlaubte Bildtypen
    private static final java.util.Set<String> ALLOWED_TYPES = java.util.Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    // ──────────────────────────────────────────────────────────────────────────
    // Upload / Hochladen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Uploads an image file to Cloudinary and returns the secure URL.
     * Lädt eine Bilddatei auf Cloudinary hoch und gibt die sichere URL zurück.
     *
     * @param file   the image file to upload / die hochzuladende Bilddatei
     * @param folder Cloudinary folder path (e.g. "rentmybike/avatars") / Cloudinary-Ordnerpfad
     * @return secure HTTPS URL of the uploaded image / sichere HTTPS-URL des hochgeladenen Bildes
     * @throws BusinessException if file is invalid or upload fails / wenn Datei ungültig oder Upload fehlschlägt
     */
    public String uploadImage(MultipartFile file, String folder) {
        // Validate file / Datei validieren
        validateImageFile(file);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            // Auto-optimize quality and format / Qualität und Format automatisch optimieren
                            "quality", "auto",
                            "fetch_format", "auto"
                    )
            );

            String url = (String) result.get("secure_url");
            log.info("Image uploaded to Cloudinary: {} / Bild auf Cloudinary hochgeladen: {}", url, url);
            return url;

        } catch (IOException e) {
            log.error("Cloudinary upload failed / Cloudinary-Upload fehlgeschlagen: {}", e.getMessage());
            throw new BusinessException("Image upload failed. Please try again. / Bild-Upload fehlgeschlagen. Bitte erneut versuchen.");
        }
    }

    /**
     * Deletes an image from Cloudinary by its public ID extracted from the URL.
     * Löscht ein Bild von Cloudinary anhand der öffentlichen ID aus der URL.
     *
     * @param imageUrl the full Cloudinary URL / die vollständige Cloudinary-URL
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        try {
            // Extract public_id from URL (everything between /upload/ and the file extension)
            // Public-ID aus URL extrahieren (alles zwischen /upload/ und der Dateiendung)
            String publicId = extractPublicId(imageUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted from Cloudinary: {} / Bild von Cloudinary gelöscht: {}", publicId, publicId);

        } catch (IOException e) {
            // Log but don't throw — deletion failure shouldn't break the user flow
            // Protokollieren aber nicht werfen — Löschfehler soll den Benutzerfluss nicht unterbrechen
            log.warn("Failed to delete image from Cloudinary: {} / Bild-Löschung von Cloudinary fehlgeschlagen: {}", e.getMessage(), e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Validates that the uploaded file is a non-empty image within size limits.
     * Validiert, dass die hochgeladene Datei ein nicht-leeres Bild innerhalb der Größenlimits ist.
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("No file provided / Keine Datei angegeben");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("File too large. Max 5MB. / Datei zu groß. Max. 5MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Invalid file type. Only JPEG, PNG, WebP allowed. / Ungültiger Dateityp. Nur JPEG, PNG, WebP erlaubt.");
        }
    }

    /**
     * Extracts the Cloudinary public_id from a full Cloudinary URL.
     * Extrahiert die Cloudinary public_id aus einer vollständigen Cloudinary-URL.
     *
     * <p>Example: https://res.cloudinary.com/cloud/image/upload/v123/rentmybike/avatars/abc.jpg
     * <p>→ rentmybike/avatars/abc
     */
    private String extractPublicId(String url) {
        // Find /upload/ segment and take everything after it, removing the version and extension
        // /upload/-Segment finden und alles danach nehmen, Version und Erweiterung entfernen
        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex == -1) return url;

        String afterUpload = url.substring(uploadIndex + 8); // skip "/upload/"

        // Remove version prefix (v1234567/) if present / Versionspräfix (v1234567/) entfernen falls vorhanden
        if (afterUpload.matches("v\\d+/.*")) {
            afterUpload = afterUpload.substring(afterUpload.indexOf('/') + 1);
        }

        // Remove file extension / Dateiendung entfernen
        int dotIndex = afterUpload.lastIndexOf('.');
        return dotIndex > 0 ? afterUpload.substring(0, dotIndex) : afterUpload;
    }
}
