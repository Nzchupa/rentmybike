package com.rentmybike.common.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Cloudinary configuration — sets up the image upload SDK.
 * Cloudinary-Konfiguration — richtet das Bild-Upload-SDK ein.
 *
 * <p>Architecture decision: images flow Client → Backend → Cloudinary → URL stored in DB.
 * <p>Architekturentscheidung: Bilder fließen Client → Backend → Cloudinary → URL in DB gespeichert.
 * The backend never stores raw image bytes on disk.
 * Das Backend speichert niemals rohe Bild-Bytes auf der Festplatte.
 *
 * <p>Configure these env vars in Railway (prod) and application-dev.yml (dev):
 * <p>Diese Umgebungsvariablen in Railway (prod) und application-dev.yml (dev) konfigurieren:
 * <ul>
 *   <li>CLOUDINARY_CLOUD_NAME</li>
 *   <li>CLOUDINARY_API_KEY</li>
 *   <li>CLOUDINARY_API_SECRET</li>
 * </ul>
 */
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    /**
     * Creates and configures the Cloudinary client bean.
     * Erstellt und konfiguriert den Cloudinary-Client-Bean.
     */
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(Map.of(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true   // Always use HTTPS URLs / Immer HTTPS-URLs verwenden
        ));
    }
}
