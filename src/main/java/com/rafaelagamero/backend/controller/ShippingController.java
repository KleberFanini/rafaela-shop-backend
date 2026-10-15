package com.rafaelagamero.backend.controller;

import com.rafaelagamero.backend.dto.ShippingCalculationDTO;
import com.rafaelagamero.backend.dto.ShippingResponseDTO;
import com.rafaelagamero.backend.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
@Tag(name = "Frete", description = "Cálculo de frete e prazos por CEP via Melhor Envio e Correios")
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/calculate")
    @Operation(summary = "Calcular frete e prazos para o CEP de destino")
    public ResponseEntity<ShippingResponseDTO> calculateShipping(@Valid @RequestBody ShippingCalculationDTO request) {
        ShippingResponseDTO response = shippingService.calculateShipping(request);
        return ResponseEntity.ok(response);
    }
}
