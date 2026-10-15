package com.rafaelagamero.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingOptionDTO {
    private String id;              // ex: pac, sedex
    private String name;            // ex: PAC - Econômico
    private String carrier;         // ex: Correios, Jadlog
    private BigDecimal price;       // Preço cobrado (pode ser 0.00 se frete grátis)
    private BigDecimal originalPrice; // Preço original da cotação
    private Integer deliveryDays;   // Prazo em dias úteis
    private String deliveryRange;   // Texto legível: "3 a 5 dias úteis"
    private boolean isFree;         // Flag se é frete grátis
    private String description;     // Descrição curta informativa
}
