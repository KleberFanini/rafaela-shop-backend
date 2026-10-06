package com.rafaelagamero.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String size; // P, M, G ou null para anéis/semijoias de aro único
    private String color; // Preto, Rosa, Ouro Rosê, Dourado
    private Integer stock;
}