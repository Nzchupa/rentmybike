package com.rentmybike.support.entity;

/**
 * Lifecycle status of a {@link SupportTicket} as it moves through the
 * support workflow.
 * Lebenszyklusstatus eines {@link SupportTicket} im Support-Workflow.
 *
 * <p>OPEN -&gt; IN_PROGRESS -&gt; RESOLVED -&gt; CLOSED. A user can still add
 * messages while OPEN/IN_PROGRESS/RESOLVED (e.g. to say "actually, still not
 * fixed") — only CLOSED is a hard terminal state re-opened by filing a new
 * ticket, matching how most helpdesks behave.
 * <p>OPEN -&gt; IN_PROGRESS -&gt; RESOLVED -&gt; CLOSED. Ein Benutzer kann bei
 * OPEN/IN_PROGRESS/RESOLVED weiterhin Nachrichten hinzufügen (z. B. "eigentlich
 * doch nicht behoben") — nur CLOSED ist ein harter Endzustand, der nur durch
 * ein neues Ticket wieder geöffnet wird, wie bei den meisten Helpdesks üblich.
 */
public enum SupportTicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
