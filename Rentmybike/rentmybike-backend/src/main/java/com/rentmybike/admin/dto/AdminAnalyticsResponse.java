package com.rentmybike.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Time-series wrapper for the admin dashboard's activity charts — daily
 * new-user, new-bike, new-booking, and revenue counters over a requested
 * trailing window.
 * Zeitreihen-Wrapper für die Aktivitätsdiagramme des Admin-Dashboards —
 * tägliche Zähler für neue Benutzer, neue Fahrräder, neue Buchungen und
 * Umsatz über ein angefordertes zurückliegendes Zeitfenster.
 */
@Data
@Builder
public class AdminAnalyticsResponse {

    /** Number of trailing days requested (e.g. 7, 30, 90) / Angeforderte Anzahl zurückliegender Tage */
    private int rangeDays;

    /** One zero-filled point per day in the range, oldest first / Ein nullaufgefüllter Punkt pro Tag im Zeitraum, älteste zuerst */
    private List<AdminTimeSeriesPoint> series;
}
