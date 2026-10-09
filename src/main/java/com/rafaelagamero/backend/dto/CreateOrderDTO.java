package com.rafaelagamero.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderDTO {
    @NotBlank(message = "Nome do cliente é obrigatório")
    private String customerName;

    @NotBlank(message = "Email do cliente é obrigatório")
    private String customerEmail;

    private String customerPhone;

    @NotBlank(message = "Endereço de entrega é obrigatório")
    private String shippingAddress;

    @NotNull(message = "Valor total é obrigatório")
    private BigDecimal totalAmount;

    @NotBlank(message = "Forma de pagamento é obrigatória")
    private String paymentMethod;

    @NotEmpty(message = "O pedido deve conter ao menos um item")
    private List<OrderItemRequestDTO> items;
}
