package com.rafaelagamero.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingResponseDTO {
    private String postalCode;
    private String street;
    private String neighborhood;
    private String city;
    private String state;
    private List<ShippingOptionDTO> options;
}
