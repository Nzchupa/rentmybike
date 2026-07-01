package com.rentmybike.contract.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.rentmybike.booking.entity.Booking;
import com.rentmybike.booking.entity.PaymentMethod;
import com.rentmybike.common.exception.AccessDeniedException;
import com.rentmybike.common.exception.BusinessException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.contract.dto.ContractResponse;
import com.rentmybike.contract.dto.ContractSectionResponse;
import com.rentmybike.contract.entity.RentalContract;
import com.rentmybike.contract.repository.RentalContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generates, serves and renders the rental contract auto-created when an
 * owner accepts a booking (see {@code BookingService.acceptBooking}).
 * Erstellt, liefert und rendert den Mietvertrag, der automatisch erstellt
 * wird, wenn ein Eigentümer eine Buchung akzeptiert (siehe
 * {@code BookingService.acceptBooking}).
 *
 * <p>The legal text below is a German-law rental agreement template (BGB
 * §§535 ff., §546, §538, §§280/823, §985, and a §312g Widerrufsrecht
 * reservation clause) with per-booking values substituted in. It is
 * intentionally always rendered in German regardless of the viewer's UI
 * locale, since it is a legal document meant to hold up under German law
 * regardless of which language the app's chrome happens to be in. This is
 * not a substitute for legal advice.
 * <p>Der untenstehende Rechtstext ist eine Mietvertragsvorlage nach
 * deutschem Recht (BGB §§535 ff., §546, §538, §§280/823, §985, sowie eine
 * Vorbehaltsklausel zum Widerrufsrecht nach §312g) mit eingesetzten,
 * buchungsspezifischen Werten. Er wird absichtlich immer auf Deutsch
 * gerendert, unabhängig von der UI-Sprache des Betrachters, da es sich um
 * ein Rechtsdokument handelt, das nach deutschem Recht Bestand haben soll,
 * unabhängig davon, in welcher Sprache die App-Oberfläche gerade ist. Dies
 * ersetzt keine Rechtsberatung.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContractService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final RentalContractRepository contractRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // GENERATION / ERSTELLUNG
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates the frozen contract snapshot for a just-accepted booking.
     * Called from {@code BookingService.acceptBooking} in the same
     * transaction as the ACCEPTED status change — must not be called more
     * than once per booking (unique constraint on booking_id enforces this
     * at the DB level as a backstop).
     * Erstellt die eingefrorene Vertrags-Momentaufnahme für eine gerade
     * akzeptierte Buchung. Wird von {@code BookingService.acceptBooking} in
     * derselben Transaktion wie die ACCEPTED-Statusänderung aufgerufen —
     * darf pro Buchung nicht mehr als einmal aufgerufen werden (eine
     * Unique-Constraint auf booking_id erzwingt dies als Absicherung auf
     * DB-Ebene).
     */
    public void generateForBooking(Booking booking, PaymentMethod paymentMethod) {
        RentalContract contract = RentalContract.builder()
                .booking(booking)
                .ownerName(booking.getOwner().getFullName())
                .ownerEmail(booking.getOwner().getEmail())
                .renterName(booking.getRenter().getFullName())
                .renterEmail(booking.getRenter().getEmail())
                .bikeTitle(booking.getBike().getTitle())
                .bikeModel(booking.getBike().getModel())
                .bikeCategory(booking.getBike().getCategory().name())
                .bikeCity(booking.getBike().getCity())
                .bikeAddress(booking.getBike().getAddress())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .rentalDays((int) booking.getRentalDays())
                .pricePerDay(booking.getBike().getPricePerDay())
                .totalPrice(booking.getTotalPrice())
                .paymentMethod(paymentMethod)
                // Frozen from the bike's listing-time deposit setting — null means the
                // owner didn't require one, in which case §4's deposit line is omitted.
                // Eingefroren aus der Kautionseinstellung des Fahrrads zum Zeitpunkt des
                // Inserats — null bedeutet, der Eigentümer verlangte keine, in diesem Fall
                // entfällt die Kautionszeile in §4.
                .depositAmount(booking.getBike().getDepositAmount())
                .build();

        contractRepository.save(contract);
        log.info("Rental contract generated for booking: {} / Mietvertrag erstellt für Buchung: {}",
                booking.getId(), booking.getId());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // READ / LESEN
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ContractResponse getContract(UUID bookingId, UUID requesterId) {
        RentalContract contract = loadForParticipant(bookingId, requesterId);
        return toResponse(contract, requesterId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CLICK-TO-ACCEPT / KLICK-ZUSTIMMUNG
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Records the caller's acceptance of the contract (timestamp + IP). A
     * second click by the same role is a no-op — it must not overwrite the
     * original timestamp/IP, since that pair is the closest thing this
     * contract has to a signature. The role (owner vs.
     * renter) is resolved from which side of the underlying booking the
     * caller is on, so the controller doesn't need to know or guess it.
     * Erfasst die Zustimmung des Aufrufers zum Vertrag — die Rolle
     * (Eigentümer oder Mieter) wird daraus abgeleitet, auf welcher Seite der
     * zugrunde liegenden Buchung sich der Aufrufer befindet, sodass der
     * Controller sie nicht kennen oder erraten muss.
     */
    public ContractResponse accept(UUID bookingId, UUID requesterId, String ipAddress) {
        RentalContract contract = loadForParticipant(bookingId, requesterId);
        Booking booking = contract.getBooking();
        boolean isOwner = booking.getOwner().getId().equals(requesterId);

        if (isOwner) {
            if (contract.getOwnerAcceptedAt() == null) {
                contract.setOwnerAcceptedAt(LocalDateTime.now());
                contract.setOwnerAcceptedIp(ipAddress);
                contractRepository.save(contract);
                log.info("Contract accepted by owner: booking {} / Vertrag vom Eigentümer akzeptiert: Buchung {}",
                        bookingId, bookingId);
            }
        } else {
            if (contract.getRenterAcceptedAt() == null) {
                contract.setRenterAcceptedAt(LocalDateTime.now());
                contract.setRenterAcceptedIp(ipAddress);
                contractRepository.save(contract);
                log.info("Contract accepted by renter: booking {} / Vertrag vom Mieter akzeptiert: Buchung {}",
                        bookingId, bookingId);
            }
        }
        return toResponse(contract, requesterId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PDF EXPORT
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generatePdf(UUID bookingId, UUID requesterId) {
        RentalContract contract = loadForParticipant(bookingId, requesterId);
        String html = renderHtml(contract);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render contract PDF for booking {} / Fehler beim PDF-Rendern für Buchung {}",
                    bookingId, bookingId, e);
            throw new BusinessException(
                    "Could not generate contract PDF / Mietvertrags-PDF konnte nicht erstellt werden");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    private RentalContract loadForParticipant(UUID bookingId, UUID requesterId) {
        RentalContract contract = contractRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("RentalContract", bookingId));

        Booking booking = contract.getBooking();
        boolean isRenter = booking.getRenter().getId().equals(requesterId);
        boolean isOwner = booking.getOwner().getId().equals(requesterId);
        if (!isRenter && !isOwner) {
            throw new AccessDeniedException(
                    "Access denied to this contract / Zugriff auf diesen Vertrag verweigert");
        }
        return contract;
    }

    private ContractResponse toResponse(RentalContract c, UUID requesterId) {
        boolean isOwner = c.getBooking().getOwner().getId().equals(requesterId);
        boolean acceptedByMe = isOwner ? c.getOwnerAcceptedAt() != null : c.getRenterAcceptedAt() != null;

        return ContractResponse.builder()
                .id(c.getId())
                .bookingId(c.getBooking().getId())
                .ownerName(c.getOwnerName())
                .ownerEmail(c.getOwnerEmail())
                .renterName(c.getRenterName())
                .renterEmail(c.getRenterEmail())
                .bikeTitle(c.getBikeTitle())
                .bikeModel(c.getBikeModel())
                .bikeCategory(c.getBikeCategory())
                .bikeCity(c.getBikeCity())
                .bikeAddress(c.getBikeAddress())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .rentalDays(c.getRentalDays())
                .pricePerDay(c.getPricePerDay())
                .totalPrice(c.getTotalPrice())
                .paymentMethod(c.getPaymentMethod())
                .depositAmount(c.getDepositAmount())
                .ownerAcceptedAt(c.getOwnerAcceptedAt())
                .renterAcceptedAt(c.getRenterAcceptedAt())
                .fullyAccepted(c.isFullyAccepted())
                .acceptedByMe(acceptedByMe)
                .createdAt(c.getCreatedAt())
                .sections(buildSections(c))
                .build();
    }

    /**
     * Renders the twelve numbered clauses of the German rental contract with
     * this contract's values substituted in. Shared by both the on-screen
     * {@link ContractResponse#getSections()} and the PDF export, so the
     * legal text only ever lives in one place.
     * Rendert die zwölf nummerierten Klauseln des deutschen Mietvertrags mit
     * den eingesetzten Werten dieses Vertrags. Wird sowohl von der
     * Bildschirmanzeige ({@link ContractResponse#getSections()}) als auch
     * vom PDF-Export verwendet, damit der Rechtstext nur an einer Stelle
     * existiert.
     */
    private List<ContractSectionResponse> buildSections(RentalContract c) {
        List<ContractSectionResponse> sections = new ArrayList<>();

        sections.add(section("§1 Vertragsparteien", String.format(
                "Dieser Mietvertrag (nachfolgend \"Vertrag\") wird über die Plattform Velohood " +
                "geschlossen zwischen dem Vermieter %s (%s) und dem Mieter %s (%s).",
                c.getOwnerName(), c.getOwnerEmail(), c.getRenterName(), c.getRenterEmail())));

        String bikeDescriptor = c.getBikeModel() != null && !c.getBikeModel().isBlank()
                ? String.format("%s (Modell: %s)", c.getBikeTitle(), c.getBikeModel())
                : c.getBikeTitle();
        String location = c.getBikeAddress() != null && !c.getBikeAddress().isBlank()
                ? String.format("%s, %s", c.getBikeAddress(), c.getBikeCity())
                : c.getBikeCity();
        sections.add(section("§2 Mietgegenstand", String.format(
                "Vermietet wird das folgende Fahrrad: %s, Kategorie: %s, Standort: %s. Das Fahrrad " +
                "wird in dem auf der Plattform beschriebenen und bei der Übergabe besichtigten " +
                "Zustand übergeben.",
                bikeDescriptor, c.getBikeCategory(), location)));

        sections.add(section("§3 Mietzeitraum", String.format(
                "Die Mietdauer beginnt am %s und endet am %s (%d Tag(e)). Das Fahrrad ist bis zum " +
                "Ende des Mietzeitraums an den Vermieter zurückzugeben (§ 546 BGB).",
                c.getStartDate().format(DATE_FMT), c.getEndDate().format(DATE_FMT), c.getRentalDays())));

        StringBuilder priceClause = new StringBuilder(String.format(
                "Der Mietpreis beträgt %s EUR pro Tag, insgesamt %s EUR für die vereinbarte Mietdauer. " +
                "Die Zahlung erfolgt wie folgt: %s",
                formatMoney(c.getPricePerDay()), formatMoney(c.getTotalPrice()), paymentClause(c.getPaymentMethod())));
        if (c.getDepositAmount() != null) {
            priceClause.append(String.format(
                    " Zusätzlich ist eine Kaution in Höhe von %s EUR zu hinterlegen. Die Kaution " +
                    "wird unmittelbar nach Rückgabe und gemeinsamer Zustandsprüfung des Fahrrads " +
                    "vollständig zurückerstattet, sofern keine über die gewöhnliche Abnutzung " +
                    "hinausgehenden Schäden festgestellt werden und die Rückgabe fristgerecht " +
                    "erfolgt ist. Werden Schäden festgestellt oder erfolgt die Rückgabe verspätet, " +
                    "kann der Vermieter die Kaution in dem Umfang einbehalten, der zur Deckung des " +
                    "entstandenen Schadens bzw. der Verspätung erforderlich ist; ein darüber " +
                    "hinausgehender Betrag ist unverzüglich zurückzuzahlen.",
                    formatMoney(c.getDepositAmount())));
        }
        sections.add(section("§4 Mietpreis und Zahlung", priceClause.toString()));

        sections.add(section("§5 Zustand des Fahrrads",
                "Der Mieter bestätigt, das Fahrrad vor Mietbeginn in Augenschein genommen zu haben " +
                "bzw. hierzu Gelegenheit erhalten zu haben. Etwaige bereits bestehende Mängel oder " +
                "Schäden sind vor der Übergabe festzuhalten (z. B. per Foto über die " +
                "Plattform-Funktion für Zustandsfotos)."));

        sections.add(section("§6 Pflichten des Mieters und Haftung",
                "Der Mieter verpflichtet sich, das Fahrrad pfleglich zu behandeln und ausschließlich " +
                "zum vertragsgemäßen Gebrauch zu nutzen. Der Mieter haftet gegenüber dem Vermieter " +
                "für Schäden am Fahrrad sowie für dessen Verlust oder Nichtrückgabe nach den " +
                "gesetzlichen Vorschriften (§§ 280, 823 BGB), sofern ihn hieran ein Verschulden " +
                "trifft. Für gewöhnliche, vertragsgemäße Abnutzung (§ 538 BGB) haftet der Mieter " +
                "nicht. Gibt der Mieter das Fahrrad nach Ende der Mietzeit nicht zurück, kann der " +
                "Vermieter zivilrechtliche Ansprüche auf Rückgabe (§ 985 BGB) sowie auf " +
                "Schadensersatz geltend machen; je nach den Umständen des Einzelfalls kann auch " +
                "eine strafrechtliche Prüfung in Betracht kommen. Dieser Vertrag stellt keine " +
                "abschließende Rechtsberatung dar."));

        sections.add(section("§7 Pflichten des Vermieters",
                "Der Vermieter übergibt das Fahrrad in betriebssicherem und vertragsgemäßem Zustand " +
                "und stellt sicher, dass zum Zeitpunkt der Übergabe keine ihm bekannten " +
                "sicherheitsrelevanten Mängel bestehen."));

        sections.add(section("§8 Identitätsprüfung",
                "Der Mieter weist sich gegenüber dem Vermieter bei der Übergabe auf Verlangen durch " +
                "ein amtliches Ausweisdokument aus. Die Plattform selbst nimmt keine " +
                "Identitätsprüfung vor und speichert keine Ausweisdaten; die Prüfung erfolgt allein " +
                "zwischen den Vertragsparteien in Person."));

        sections.add(section("§9 Haftungsausschluss der Plattform",
                "Die Plattform ist nicht Vertragspartei dieses Mietvertrags, sondern vermittelt " +
                "lediglich den Kontakt zwischen Vermieter und Mieter. Für die Erfüllung dieses " +
                "Vertrags — insbesondere Zustand des Fahrrads, Zahlung und Rückgabe — sind allein " +
                "die Vertragsparteien verantwortlich."));

        sections.add(section("§10 Stornierung",
                "Bis zur Annahme der Buchung durch den Vermieter kann die Anfrage vom Mieter " +
                "jederzeit kostenfrei storniert werden. Nach Annahme richten sich " +
                "Stornierungsmöglichkeiten nach den in der Plattform hinterlegten Bedingungen."));

        sections.add(section("§11 Vorbehalt Widerrufsrecht",
                "Sofern und soweit dem Mieter als Verbraucher ein gesetzliches Widerrufsrecht nach " +
                "§ 312g BGB zusteht, bleibt dieses von den vorstehenden Regelungen unberührt. Ob im " +
                "Einzelfall ein Widerrufsrecht besteht, hängt von den gesetzlichen " +
                "Ausnahmetatbeständen ab; diese Klausel begründet kein zusätzliches Recht, sondern " +
                "stellt gesetzliche Rechte lediglich klar."));

        sections.add(section("§12 Zustimmung",
                "Durch Klick auf \"Ich akzeptiere\" bestätigt jede Partei, den vorstehenden Vertrag " +
                "gelesen und verstanden zu haben und seinem Inhalt zuzustimmen. Die Zustimmung wird " +
                "mit Zeitstempel und IP-Adresse der jeweiligen Partei elektronisch protokolliert."));

        return sections;
    }

    private ContractSectionResponse section(String title, String body) {
        return ContractSectionResponse.builder().title(title).body(body).build();
    }

    private String paymentClause(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Barzahlung bei Übergabe des Fahrrads.";
            case PAYPAL -> "Überweisung per PayPal. Der Mieter zahlt den fälligen Betrag nach Erhalt " +
                    "der PayPal-Zahlungsanforderung des Vermieters und lädt einen Zahlungsnachweis auf " +
                    "der Plattform hoch; der Vermieter bestätigt den Zahlungseingang vor Übergabe des " +
                    "Fahrrads.";
            case CARD_ON_SITE -> "Kartenzahlung vor Ort im Geschäft des Vermieters.";
        };
    }

    private String formatMoney(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Wraps the rendered sections in minimal HTML for the PDF renderer.
     * openhtmltopdf needs well-formed XHTML — no unclosed tags.
     * Umschließt die gerenderten Abschnitte mit minimalem HTML für den
     * PDF-Renderer. openhtmltopdf benötigt wohlgeformtes XHTML — keine
     * offenen Tags.
     */
    private String renderHtml(RentalContract c) {
        StringBuilder body = new StringBuilder();
        body.append("<html><head><meta charset=\"UTF-8\"/><style>")
            .append("body { font-family: 'Helvetica', sans-serif; font-size: 11pt; color: #1e293b; }")
            .append("h1 { font-size: 16pt; margin-bottom: 4px; }")
            .append("h2 { font-size: 11pt; margin-top: 18px; margin-bottom: 4px; }")
            .append("p { line-height: 1.5; margin: 0 0 8px 0; }")
            .append(".meta { color: #64748b; font-size: 9pt; margin-bottom: 20px; }")
            .append("</style></head><body>");

        body.append("<h1>Mietvertrag</h1>");
        body.append("<p class=\"meta\">Vertrags-ID: ").append(c.getId()).append(" · Erstellt am ")
            .append(c.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("</p>");

        for (ContractSectionResponse s : buildSections(c)) {
            body.append("<h2>").append(escape(s.getTitle())).append("</h2>");
            body.append("<p>").append(escape(s.getBody())).append("</p>");
        }

        body.append("<h2>Zustimmung</h2>");
        body.append("<p>Vermieter: ").append(escape(c.getOwnerName())).append(" — ")
            .append(c.getOwnerAcceptedAt() != null
                    ? "akzeptiert am " + c.getOwnerAcceptedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) +
                      " (IP: " + escape(String.valueOf(c.getOwnerAcceptedIp())) + ")"
                    : "noch nicht akzeptiert")
            .append("</p>");
        body.append("<p>Mieter: ").append(escape(c.getRenterName())).append(" — ")
            .append(c.getRenterAcceptedAt() != null
                    ? "akzeptiert am " + c.getRenterAcceptedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) +
                      " (IP: " + escape(String.valueOf(c.getRenterAcceptedIp())) + ")"
                    : "noch nicht akzeptiert")
            .append("</p>");

        body.append("</body></html>");
        return body.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
