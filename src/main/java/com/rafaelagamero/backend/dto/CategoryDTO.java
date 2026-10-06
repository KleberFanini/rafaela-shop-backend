package com.rafaelagamero.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryDTO {
    private Long id;

    @NotBlank(message = "O nome da categoria é obrigatório")
    @Schema(description = "Nome da categoria", example = "FITNESS")
    private String name;
}