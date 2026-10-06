package com.rafaelagamero.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ProductVariantDTO {
    private Long id;

    @Schema(description = "Tamanho da peça (P, M, G ou aro)", example = "M")
    private String size;

    @Schema(description = "Cor da peça ou banho da semijoia", example = "Preto")
    private String color;

    @Min(value = 0, message = "O estoque não pode ser negativo")
    @Schema(description = "Quantidade em estoque", example = "10")
    private Integer stock;
}