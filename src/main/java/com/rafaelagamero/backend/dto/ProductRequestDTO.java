package com.rafaelagamero.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequestDTO {

    @NotBlank(message = "O nome do produto é obrigatório")
    @Schema(description = "Nome do produto", example = "Legging Glow Rose")
    private String name;

    @Schema(description = "Descrição detalhada do produto", example = "Tecido de alta compressão com brilho sutil acetinado")
    private String description;

    @NotNull(message = "O preço é obrigatório")
    @Positive(message = "O preço deve ser maior que zero")
    @Schema(description = "Preço unitário", example = "149.90")
    private BigDecimal price;

    @NotNull(message = "A categoria é obrigatória")
    @Schema(description = "ID da categoria associada", example = "1")
    private Long categoryId;

    @Schema(description = "Lista de variações (cores/tamanhos/estoque)")
    private List<ProductVariantDTO> variants;
}