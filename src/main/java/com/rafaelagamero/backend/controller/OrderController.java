package com.rafaelagamero.backend.controller;

import com.rafaelagamero.backend.dto.*;
import com.rafaelagamero.backend.model.*;
import com.rafaelagamero.backend.repository.OrderRepository;
import com.rafaelagamero.backend.repository.ProductRepository;
import com.rafaelagamero.backend.repository.ProductVariantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos & Dashboard", description = "Gestão de pedidos e estatísticas reais do banco")
public class OrderController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @GetMapping("/dashboard-stats")
    @Operation(summary = "Obter estatísticas reais do painel administrativo vindas do banco de dados")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countPendingOrders();
        long activeCatalogCount = productRepository.count();

        // Cálculo comparativo do mês passado em relação ao mês atual
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfCurrentMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfPreviousMonth = startOfCurrentMonth.minusMonths(1);

        BigDecimal currentMonthRevenue = orderRepository.calculateRevenueBetween(startOfCurrentMonth, now);
        BigDecimal previousMonthRevenue = orderRepository.calculateRevenueBetween(startOfPreviousMonth, startOfCurrentMonth);

        double growthPercentage = 0.0;
        if (previousMonthRevenue != null && previousMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
            double diff = currentMonthRevenue.doubleValue() - previousMonthRevenue.doubleValue();
            growthPercentage = (diff / previousMonthRevenue.doubleValue()) * 100.0;
        } else if (currentMonthRevenue != null && currentMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
            growthPercentage = 100.0;
        }

        List<Product> products = productRepository.findAll();
        long lowStockCount = products.stream()
                .filter(p -> p.getVariants() != null && p.getVariants().stream().anyMatch(v -> v.getStock() != null && v.getStock() <= 3))
                .count();

        DashboardStatsDTO stats = DashboardStatsDTO.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .revenueGrowthPercentage(Math.round(growthPercentage * 10.0) / 10.0)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .activeCatalogCount(activeCatalogCount)
                .lowStockCount(lowStockCount)
                .build();

        return ResponseEntity.ok(stats);
    }

    @GetMapping
    @Operation(summary = "Listar todos os pedidos ordenados por data")
    public ResponseEntity<List<OrderResponseDTO>> listAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<OrderResponseDTO> dtos = orders.stream().map(this::toResponseDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Criar novo pedido e atualizar estoque dos produtos comprados")
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody CreateOrderDTO dto) {
        Order order = Order.builder()
                .customerName(dto.getCustomerName())
                .customerEmail(dto.getCustomerEmail())
                .customerPhone(dto.getCustomerPhone())
                .shippingAddress(dto.getShippingAddress())
                .totalAmount(dto.getTotalAmount())
                .paymentMethod(dto.getPaymentMethod())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        List<OrderItem> items = new ArrayList<>();

        for (OrderItemRequestDTO itemDto : dto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + itemDto.getProductId()));

            // Decrementa o estoque da variação se informada
            if (itemDto.getVariantId() != null) {
                productVariantRepository.findById(itemDto.getVariantId()).ifPresent(v -> {
                    int currentStock = v.getStock() != null ? v.getStock() : 0;
                    v.setStock(Math.max(0, currentStock - itemDto.getQuantity()));
                    productVariantRepository.save(v);
                });
            } else if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                // Tenta associar por tamanho / cor
                product.getVariants().stream()
                        .filter(v -> (itemDto.getVariantSize() != null && itemDto.getVariantSize().equalsIgnoreCase(v.getSize())) ||
                                     (itemDto.getVariantColor() != null && itemDto.getVariantColor().equalsIgnoreCase(v.getColor())))
                        .findFirst()
                        .ifPresent(v -> {
                            int currentStock = v.getStock() != null ? v.getStock() : 0;
                            v.setStock(Math.max(0, currentStock - itemDto.getQuantity()));
                            productVariantRepository.save(v);
                        });
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .variantSize(itemDto.getVariantSize())
                    .variantColor(itemDto.getVariantColor())
                    .quantity(itemDto.getQuantity())
                    .price(itemDto.getPrice())
                    .build();

            items.add(orderItem);
        }

        order.setItems(items);
        Order savedOrder = orderRepository.save(order);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(savedOrder));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status de um pedido (ex: PAID, SHIPPED, CANCELED)")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado com ID: " + id));

        order.setStatus(status.toUpperCase());
        Order updated = orderRepository.save(order);
        return ResponseEntity.ok(toResponseDTO(updated));
    }

    private OrderResponseDTO toResponseDTO(Order order) {
        List<OrderResponseDTO.OrderItemResponseDTO> itemDTOs = order.getItems() != null ? order.getItems().stream().map(item ->
                OrderResponseDTO.OrderItemResponseDTO.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProduct() != null ? item.getProduct().getName() : "Produto Removido")
                        .productImage(item.getProduct() != null ? item.getProduct().getImageUrl() : null)
                        .variantSize(item.getVariantSize())
                        .variantColor(item.getVariantColor())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build()
        ).toList() : List.of();

        return OrderResponseDTO.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .shippingAddress(order.getShippingAddress())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(itemDTOs)
                .build();
    }
}
