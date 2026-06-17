package com.rentmybike.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * A single occupied date range for a bike, exposed publicly so renters can
 * see which dates are already taken before submitting a booking request.
 * Ein einzelner belegter Datumsbereich für ein Fahrrad, öffentlich zugänglich,
 * damit Mieter sehen können, welche Termine bereits vergeben sind, bevor sie
 * eine Buchungsanfrage senden.
 *
 * <p>Only PENDING/ACCEPTED bookings are exposed here (see
 * BookingRepository.findActiveBookingsByBikeId) — no renter identity or other
 * booking details are included, to avoid leaking unrelated personal data
 * through a public endpoint.
 * <p>Hier werden nur PENDING/ACCEPTED-Buchungen offengelegt (siehe
 * BookingRepository.findActiveBookingsByBikeId) — keine Mieteridentität oder
 * andere Buchungsdetails sind enthalten, um keine unzusammenhängenden
 * persönlichen Daten über einen öffentlichen Endpunkt offenzulegen.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookedDateRangeResponse {
    private LocalDate startDate;
    private LocalDate endDate;
}
