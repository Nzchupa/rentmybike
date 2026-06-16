package com.rentmybike.auth.service;

import com.rentmybike.common.config.AppProperties;
import com.rentmybike.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Service for sending transactional emails (verification, etc.).
 * Service zum Senden von Transaktions-E-Mails (Verifizierung, etc.).
 *
 * <p>All email sending is done asynchronously to avoid blocking HTTP requests.
 * <p>Alle E-Mail-Versendungen erfolgen asynchron, um HTTP-Anfragen nicht zu blockieren.
 *
 * <p>Dev environment uses Mailtrap; prod uses configured SMTP (Resend/Mailgun).
 * <p>Entwicklungsumgebung nutzt Mailtrap; Produktion nutzt konfiguriertes SMTP (Resend/Mailgun).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

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
     * @param user  the user to verify / der zu verifizierende Benutzer
     * @param token the verification token / der Verifizierungstoken
     */
    @Async
    public void sendVerificationEmail(User user, String token) {
        // Construct the verification URL / Verifizierungs-URL zusammenstellen
        String verificationUrl = appProperties.getFrontendUrl()
                + "/auth/verify-email?token=" + token;

        String subject = "Verify your RentMyBike account / RentMyBike-Konto bestätigen";
        String htmlBody = buildVerificationEmailHtml(user.getFirstName(), verificationUrl);

        sendHtmlEmail(user.getEmail(), subject, htmlBody);
    }

    /**
     * Resends the verification email with a new token.
     * Sendet die Verifizierungs-E-Mail mit einem neuen Token erneut.
     *
     * @param user  the user requesting resend / der Benutzer, der erneuten Versand anfordert
     * @param token the new verification token / der neue Verifizierungstoken
     */
    @Async
    public void resendVerificationEmail(User user, String token) {
        // Reuse the same template, just with a fresh token / Gleiche Vorlage verwenden, nur mit neuem Token
        sendVerificationEmail(user, token);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Sends an HTML email to the given address.
     * Sendet eine HTML-E-Mail an die angegebene Adresse.
     *
     * @param to       recipient email / Empfänger-E-Mail
     * @param subject  email subject / E-Mail-Betreff
     * @param htmlBody HTML content / HTML-Inhalt
     */
    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Verified custom domain — can send to any recipient, not just the sandbox owner.
            // Verifizierte eigene Domain — kann an beliebige Empfänger senden, nicht nur an den Sandbox-Besitzer.
            helper.setFrom("noreply@rentmybike.xyz", "RentMyBike");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = isHtml / true = ist HTML

            mailSender.send(message);
            log.info("Email sent to: {} / E-Mail gesendet an: {}", to, to);

        } catch (MessagingException e) {
            // Log but don't rethrow — email failure should not break the user flow
            // Protokollieren aber nicht weiterwerfen — E-Mail-Fehler soll den Benutzerfluss nicht unterbrechen
            log.error("Failed to send email to: {} / E-Mail-Versand an {} fehlgeschlagen: {}", to, to, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email / Unerwarteter Fehler beim E-Mail-Versand: {}", e.getMessage());
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
