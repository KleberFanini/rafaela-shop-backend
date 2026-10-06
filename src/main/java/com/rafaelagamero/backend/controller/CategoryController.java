package com.rafaelagamero.backend.controller;

import com.rafaelagamero.backend.dto.CategoryDTO;
import com.rafaelagamero.backend.model.Category;
import com.rafaelagamero.backend.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categorias", description = "Endpoints para gerenciamento de categorias da loja")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    @Operation(summary = "Listar todas as categorias")
    public List<CategoryDTO> listAll() {
        return categoryRepository.findAll().stream().map(c -> {
            CategoryDTO dto = new CategoryDTO();
            dto.setId(c.getId());
            dto.setName(c.getName());
            return dto;
        }).toList();
    }

    @PostMapping
    @Operation(summary = "Criar nova categoria")
    public ResponseEntity<CategoryDTO> create(@Valid @RequestBody CategoryDTO dto) {
        Category category = new Category();
        category.setName(dto.getName().toUpperCase());
        Category saved = categoryRepository.save(category);
        dto.setId(saved.getId());
        dto.setName(saved.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}