package com.rafaelagamero.backend.controller;

import com.rafaelagamero.backend.dto.*;
import com.rafaelagamero.backend.model.*;
import com.rafaelagamero.backend.repository.CategoryRepository;
import com.rafaelagamero.backend.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Catálogo e gestão de produtos e variações")
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping
    @Operation(summary = "Listar todos os produtos com variações")
    public List<ProductResponseDTO> listAll(@RequestParam(required = false) Long categoryId) {
        List<Product> products = (categoryId != null)
                ? productRepository.findByCategoryId(categoryId)
                : productRepository.findAll();

        return products.stream().map(this::toResponseDTO).toList();
    }

    @PostMapping
    @Operation(summary = "Cadastrar produto com suas variações e estoque")
    public ResponseEntity<ProductResponseDTO> create(@Valid @RequestBody ProductRequestDTO dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada com o ID: " + dto.getCategoryId()));

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setCategory(category);

        if (dto.getVariants() != null) {
            List<ProductVariant> variants = dto.getVariants().stream().map(v -> {
                ProductVariant variant = new ProductVariant();
                variant.setSize(v.getSize());
                variant.setColor(v.getColor());
                variant.setStock(v.getStock());
                variant.setProduct(product);
                return variant;
            }).toList();
            product.setVariants(variants);
        }

        Product saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(saved));
    }

    private ProductResponseDTO toResponseDTO(Product product) {
        ProductResponseDTO res = new ProductResponseDTO();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setDescription(product.getDescription());
        res.setPrice(product.getPrice());
        res.setCreatedAt(product.getCreatedAt());

        CategoryDTO catDTO = new CategoryDTO();
        catDTO.setId(product.getCategory().getId());
        catDTO.setName(product.getCategory().getName());
        res.setCategory(catDTO);

        if (product.getVariants() != null) {
            res.setVariants(product.getVariants().stream().map(v -> {
                ProductVariantDTO vDto = new ProductVariantDTO();
                vDto.setId(v.getId());
                vDto.setSize(v.getSize());
                vDto.setColor(v.getColor());
                vDto.setStock(v.getStock());
                return vDto;
            }).toList());
        }
        return res;
    }
}