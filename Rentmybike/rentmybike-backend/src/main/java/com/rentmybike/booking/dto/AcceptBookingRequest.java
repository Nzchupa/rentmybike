package com.rentmybike.booking.dto;

import com.rentmybike.booking.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for the owner accepting a PENDING booking — the owner must
 * name the payment method in the same step, since it is immediately frozen
 * into the auto-generated rental contract (see
 * {@code com.rentmybike.contract.service.ContractService}).
 * Request-Body für die Annahme einer PENDING-Buchung durch den Eigentümer —
 * der Eigentümer muss die Zahlungsmethode im selben Schritt angeben, da sie
 * sofort in den automatisch erstellten Mietvertrag eingefroren wird (siehe
 * {@code com.rentmybike.contract.service.ContractService}).
 */
@Data
public class AcceptBookingRequest {

    @NotNull(message = "Payment method is required / Zahlungsmethode ist erforderlich")
    private PaymentMethod paymentMethod;
}
