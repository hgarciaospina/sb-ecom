package com.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used to expose product details with stock information
 * for order responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryDTO {
    private Long productId;
    private String productName;
    private String image;
    private String description;
    private Integer stock;
    private double price;
    private double discount;
    private double specialPrice;
}