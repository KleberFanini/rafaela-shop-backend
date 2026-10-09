package com.rafaelagamero.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequestDTO {
    @NotNull(message = "ID do produto é obrigatório")
    private Long productId;

    private Long variantId;
    private String variantSize;
    private String variantColor;

    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade mínima é 1")
    private Integer quantity;

    @NotNull(message = "Preço é obrigatório")
    private BigDecimal price;
}
