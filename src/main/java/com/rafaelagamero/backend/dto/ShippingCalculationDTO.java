package com.rafaelagamero.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingCalculationDTO {

    @NotBlank(message = "O CEP de destino é obrigatório")
    private String postalCode;

    private BigDecimal subtotal;
}
