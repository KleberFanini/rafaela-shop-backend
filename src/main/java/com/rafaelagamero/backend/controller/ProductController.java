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
    @Operation(summary = "Listar produtos com variações (ativos por padrão, ou todos incluindo inativos)")
    public List<ProductResponseDTO> listAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        List<Product> products;
        if (includeInactive) {
            products = (categoryId != null)
                    ? productRepository.findByCategoryId(categoryId)
                    : productRepository.findAll();
        } else {
            products = (categoryId != null)
                    ? productRepository.findByCategoryIdAndActiveTrue(categoryId)
                    : productRepository.findByActiveTrue();
        }

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
        product.setImageUrl(dto.getImageUrl());
        product.setCategory(category);
        product.setGender(dto.getGender() != null && !dto.getGender().isBlank() ? dto.getGender().toUpperCase() : "FEMININO");
        product.setActive(dto.getActive() != null ? dto.getActive() : true);

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

    @GetMapping("/{id}")
    @Operation(summary = "Buscar produto por ID com variações")
    public ResponseEntity<ProductResponseDTO> getById(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com o ID: " + id));
        return ResponseEntity.ok(toResponseDTO(product));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar produto e suas variações por ID")
    public ResponseEntity<ProductResponseDTO> update(@PathVariable Long id, @Valid @RequestBody ProductRequestDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com o ID: " + id));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada com o ID: " + dto.getCategoryId()));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            product.setImageUrl(dto.getImageUrl());
        }
        product.setCategory(category);
        if (dto.getGender() != null && !dto.getGender().isBlank()) {
            product.setGender(dto.getGender().toUpperCase());
        }
        if (dto.getActive() != null) {
            product.setActive(dto.getActive());
        }

        if (dto.getVariants() != null) {
            product.getVariants().clear();
            List<ProductVariant> newVariants = dto.getVariants().stream().map(v -> {
                ProductVariant variant = new ProductVariant();
                variant.setSize(v.getSize());
                variant.setColor(v.getColor());
                variant.setStock(v.getStock());
                variant.setProduct(product);
                return variant;
            }).toList();
            product.getVariants().addAll(newVariants);
        }

        Product updated = productRepository.save(product);
        return ResponseEntity.ok(toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclusão lógica de produto (desativa do catálogo da loja)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com o ID: " + id));

        product.setActive(false);
        productRepository.save(product);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Alternar status ativo/desativado do produto no catálogo")
    public ResponseEntity<ProductResponseDTO> toggleActive(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com o ID: " + id));

        product.setActive(!Boolean.TRUE.equals(product.getActive()));
        Product updated = productRepository.save(product);
        return ResponseEntity.ok(toResponseDTO(updated));
    }

    private ProductResponseDTO toResponseDTO(Product product) {
        ProductResponseDTO res = new ProductResponseDTO();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setDescription(product.getDescription());
        res.setPrice(product.getPrice());
        res.setImageUrl(product.getImageUrl());
        res.setGender(product.getGender() != null ? product.getGender() : "FEMININO");
        res.setActive(product.getActive() != null ? product.getActive() : true);
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