package com.rentmybike.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One numbered clause of the rendered rental contract (e.g. "§4 Mietpreis und
 * Zahlung"). The frontend renders these in order without needing to know
 * anything about the underlying legal template — all substitution (names,
 * dates, prices) already happened server-side.
 * Eine nummerierte Klausel des gerenderten Mietvertrags (z. B. "§4 Mietpreis
 * und Zahlung"). Das Frontend rendert diese der Reihe nach, ohne etwas über
 * die zugrunde liegende Rechtsvorlage wissen zu müssen — jede Ersetzung
 * (Namen, Termine, Preise) ist bereits serverseitig erfolgt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractSectionResponse {
    private String title;
    private String body;
}
