package com.rentmybike.notification.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Tiny DTO backing the bell icon's unread badge.
 * Winziges DTO, das das Ungelesen-Badge des Glocken-Icons unterstützt.
 */
@Getter
@Builder
public class UnreadCountResponse {
    private long unreadCount;
}
