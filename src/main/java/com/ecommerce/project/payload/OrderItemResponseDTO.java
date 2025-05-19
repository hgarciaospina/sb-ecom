package com.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Specialized DTO for order items, including quantity ordered
 * and product information with current stock.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDTO {
    private Long orderItemId;
    private ProductSummaryDTO product;
    private Integer quantity;
    private double discount;
    private double orderedProductPrice;
}