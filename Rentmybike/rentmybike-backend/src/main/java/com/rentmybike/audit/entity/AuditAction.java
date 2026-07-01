package com.rentmybike.audit.entity;

/**
 * The set of admin/moderation/account events recorded in the audit log.
 * Die Menge der im Audit-Log erfassten Admin-/Moderations-/Kontoereignisse.
 */
public enum AuditAction {
    USER_REGISTERED,
    USER_BANNED,
    USER_UNBANNED,
    USER_SUSPENDED,
    USER_UNSUSPENDED,
    USER_DELETED,
    USER_PROMOTED_TO_BUSINESS,
    USER_PROMOTED_TO_ADMIN,
    USER_VERIFIED_EMAIL,
    BIKE_APPROVED,
    BIKE_REJECTED,
    BIKE_CHANGES_REQUESTED,
    BUSINESS_VERIFIED,
    BUSINESS_UNVERIFIED,
    BOOKING_CANCELLED,
    USER_WARNED,
    REPORT_RESOLVED,
    REPORT_DISMISSED,
    SUPPORT_TICKET_RESOLVED,
    SUPPORT_TICKET_CLOSED
}
