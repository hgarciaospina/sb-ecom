package com.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO that represents a full order including user, items, payment, status, and address reference.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long orderId;
    private String email;

    // ✅ Use the correct DTO type here
    private List<OrderItemResponseDTO> orderItemsDTO = new ArrayList<>();

    private LocalDate orderDate;
    private PaymentDTO paymentDTO;
    private Double totalAmount;
    private String orderStatus;
    private Long addressId;

    // This method is now correctly typed and implemented
    public void setOrderItemsDTO(List<OrderItemResponseDTO> itemsDTO) {
        this.orderItemsDTO = itemsDTO;
    }
}
