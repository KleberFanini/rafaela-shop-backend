package com.rafaelagamero.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {
    private BigDecimal totalRevenue;
    private Double revenueGrowthPercentage;
    private long totalOrders;
    private long pendingOrders;
    private long activeCatalogCount;
    private long lowStockCount;
}
