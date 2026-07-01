package com.rentmybike.support.entity;

/**
 * What a {@link SupportTicket} is about — lets admins triage at a glance and
 * lets the frontend show a relevant icon/label.
 * Worum es in einem {@link SupportTicket} geht — ermöglicht Admins eine
 * Triage auf einen Blick und dem Frontend ein passendes Icon/Label.
 */
public enum SupportCategory {
    BOOKING,
    PAYMENT,
    ACCOUNT,
    BIKE_LISTING,
    OTHER
}
