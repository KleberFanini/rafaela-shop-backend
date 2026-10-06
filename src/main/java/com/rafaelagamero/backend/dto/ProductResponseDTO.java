package com.rafaelagamero.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private CategoryDTO category;
    private List<ProductVariantDTO> variants;
    private LocalDateTime createdAt;
}