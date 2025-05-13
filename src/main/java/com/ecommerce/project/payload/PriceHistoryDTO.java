package com.ecommerce.project.payload;

import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistoryDTO {
    private Long id;
    private double oldPrice;
    private double newPrice;
    private LocalDateTime changedAt;
    private String changedByUsername;
    private Long productId;
    private String productName;
}
