package com.rentmybike.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentmybike.common.config.AppProperties;
import com.rentmybike.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Service for sending transactional emails (verification, etc.).
 * Service zum Senden von Transaktions-E-Mails (Verifizierung, etc.).
 *
 * <p>Sending is synchronous and returns a success/failure boolean — callers
 * use it to tell the user honestly whether the verification email actually
 * went out, instead of always claiming success.
 * <p>Der Versand erfolgt synchron und liefert ein Erfolg/Fehlschlag-Boolean
 * zurück — Aufrufer nutzen es, um dem Benutzer ehrlich mitzuteilen, ob die
 * Verifizierungs-E-Mail tatsächlich verschickt wurde, statt immer Erfolg zu behaupten.
 *
 * <p>Emails are sent via Resend's HTTPS API (https://api.resend.com/emails) rather
 * than SMTP, because Railway blocks outbound SMTP ports (25/465/587) on this plan.
 * The HTTP API only needs port 443, which is never blocked.
 * <p>E-Mails werden über Resends HTTPS-API gesendet statt über SMTP, da Railway
 * ausgehende SMTP-Ports (25/465/587) auf diesem Plan blockiert. Die HTTP-API
 * benötigt nur Port 443, der nie blockiert wird.
 */
@Service
@Slf4j
public class EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final String FROM_ADDRESS = "RentMyBike <noreply@rentmybike.xyz>";

    private final AppProperties appProperties;
    private final String resendApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmailService(AppProperties appProperties,
                         @Value("${resend.api-key:}") String resendApiKey) {
        this.appProperties = appProperties;
        this.resendApiKey = resendApiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Email verification / E-Mail-Verifizierung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Sends an email verification link to the newly registered user.
     * Sendet einen E-Mail-Verifizierungslink an den neu registrierten Benutzer.
     *
     * <p>The link is valid for 24 hours after sending.
     * <p>Der Link ist 24 Stunden nach dem Versand gültig.
     *
     * <p>Intentionally synchronous (not {@code @Async}): callers (AuthService)
     * need the success/failure result to decide what to tell the user — if this
     * silently failed in the background, registration would always claim "check
     * your email" even when no email was ever sent.
     * <p>Bewusst synchron (kein {@code @Async}): Aufrufer (AuthService) benötigen
     * das Erfolgs-/Fehlschlag-Ergebnis, um zu entscheiden, was dem Benutzer
     * mitgeteilt wird — würde dies im Hintergrund stillschweigend fehlschlagen,
     * würde die Registrierung immer "E-Mail prüfen" behaupten, auch wenn nie eine
     * E-Mail gesendet wurde.
     *
     * @param user  the user to verify / der zu verifizierende Benutzer
     * @param token the verification token / der Verifizierungstoken
     * @return true if the email was accepted by Resend / true, wenn die E-Mail von Resend angenommen wurde
     */
    public boolean sendVerificationEmail(User user, String token) {
        // Construct the verification URL / Verifizierungs-URL zusammenstellen
        String verificationUrl = appProperties.getFrontendUrl()
                + "/auth/verify-email?token=" + token;

        String subject = "Verify your RentMyBike account / RentMyBike-Konto bestätigen";
        String htmlBody = buildVerificationEmailHtml(user.getFirstName(), verificationUrl);

        return sendHtmlEmail(user.getEmail(), subject, htmlBody);
    }

    /**
     * Resends the verification email with a new token.
     * Sendet die Verifizierungs-E-Mail mit einem neuen Token erneut.
     *
     * @param user  the user requesting resend / der Benutzer, der erneuten Versand anfordert
     * @param token the new verification token / der neue Verifizierungstoken
     * @return true if the email was accepted by Resend / true, wenn die E-Mail von Resend angenommen wurde
     */
    public boolean resendVerificationEmail(User user, String token) {
        // Reuse the same template, just with a fresh token / Gleiche Vorlage verwenden, nur mit neuem Token
        return sendVerificationEmail(user, token);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Sends an HTML email via the Resend HTTPS API.
     * Sendet eine HTML-E-Mail über die Resend-HTTPS-API.
     *
     * @param to       recipient email / Empfänger-E-Mail
     * @param subject  email subject / E-Mail-Betreff
     * @param htmlBody HTML content / HTML-Inhalt
     */
    private boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.error("Resend API key is not configured — email to {} not sent / "
                    + "Resend API-Schlüssel ist nicht konfiguriert — E-Mail an {} nicht gesendet", to, to);
            return false;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "from", FROM_ADDRESS,
                    "to", new String[]{to},
                    "subject", subject,
                    "html", htmlBody
            );
            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email sent to: {} / E-Mail gesendet an: {}", to, to);
                return true;
            } else {
                log.error("Resend API returned {} for email to {}: {} / "
                        + "Resend-API gab {} für E-Mail an {} zurück: {}",
                        response.statusCode(), to, response.body(),
                        response.statusCode(), to, response.body());
                return false;
            }

        } catch (Exception e) {
            // Log but don't rethrow — email failure should not break the user flow;
            // the caller decides how to inform the user based on the boolean result.
            // Protokollieren aber nicht weiterwerfen — E-Mail-Fehler soll den Benutzerfluss
            // nicht unterbrechen; der Aufrufer entscheidet anhand des booleschen Ergebnisses,
            // wie der Benutzer informiert wird.
            log.error("Failed to send email to: {} / E-Mail-Versand an {} fehlgeschlagen: {}", to, to, e.getMessage());
            return false;
        }
    }

    /**
     * Builds the HTML body for the verification email.
     * Erstellt den HTML-Body für die Verifizierungs-E-Mail.
     *
     * @param firstName       user's first name for personalization / Vorname des Benutzers zur Personalisierung
     * @param verificationUrl the full verification URL / die vollständige Verifizierungs-URL
     * @return HTML string / HTML-String
     */
    private String buildVerificationEmailHtml(String firstName, String verificationUrl) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Verify your email</title>
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                             background-color: #f5f5f5; margin: 0; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background: white;
                                border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">

                        <!-- Header / Kopfzeile -->
                        <div style="background: linear-gradient(135deg, #0057FF, #4D8FFF);
                                    padding: 32px; text-align: center;">
                            <h1 style="color: white; margin: 0; font-size: 28px;">🚲 RentMyBike</h1>
                        </div>

                        <!-- Body / Inhalt -->
                        <div style="padding: 40px 32px;">
                            <h2 style="color: #1a1a2e; margin-top: 0;">
                                Hi %s! Welcome to RentMyBike 👋
                            </h2>
                            <p style="color: #4a4a4a; line-height: 1.6;">
                                Please verify your email address to activate your account
                                and start renting or listing bikes in your city.
                            </p>
                            <p style="color: #4a4a4a; line-height: 1.6; font-size: 14px;">
                                <em>Bitte bestätige deine E-Mail-Adresse, um dein Konto zu aktivieren
                                und Fahrräder in deiner Stadt zu mieten oder anzubieten.</em>
                            </p>

                            <!-- CTA Button / Aktionsschaltfläche -->
                            <div style="text-align: center; margin: 40px 0;">
                                <a href="%s"
                                   style="background: #0057FF; color: white; text-decoration: none;
                                          padding: 16px 40px; border-radius: 8px; font-size: 16px;
                                          font-weight: 600; display: inline-block;">
                                    ✅ Verify Email / E-Mail bestätigen
                                </a>
                            </div>

                            <!-- Fallback link / Ausweich-Link -->
                            <p style="color: #888; font-size: 13px; text-align: center;">
                                If the button doesn't work, copy this link:<br>
                                <em>Falls die Schaltfläche nicht funktioniert, kopiere diesen Link:</em><br>
                                <a href="%s" style="color: #0057FF; word-break: break-all;">%s</a>
                            </p>

                            <!-- Expiry notice / Ablaufhinweis -->
                            <p style="color: #888; font-size: 12px; text-align: center; margin-top: 24px;">
                                This link expires in 24 hours. /
                                Dieser Link läuft in 24 Stunden ab.
                            </p>
                        </div>

                        <!-- Footer / Fußzeile -->
                        <div style="background: #f9f9f9; padding: 20px 32px; text-align: center;
                                    border-top: 1px solid #eee;">
                            <p style="color: #aaa; font-size: 12px; margin: 0;">
                                © 2024 RentMyBike — P2P Bike Rental Marketplace
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(firstName, verificationUrl, verificationUrl, verificationUrl);
    }
}
