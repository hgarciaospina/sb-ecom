package com.ecommerce.project.config;

import com.ecommerce.project.model.Order;
import com.ecommerce.project.model.OrderItem;
import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.OrderItemDTO;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // Order -> OrderDTO: map Payment to PaymentDTO
        TypeMap<Order, OrderDTO> orderTypeMap = modelMapper.createTypeMap(Order.class, OrderDTO.class);
        orderTypeMap.addMappings(mapper -> mapper.map(Order::getPayment, OrderDTO::setPaymentDTO));

        // OrderItem -> OrderItemDTO: map Product to ProductDTO
        TypeMap<OrderItem, OrderItemDTO> itemTypeMap = modelMapper.createTypeMap(OrderItem.class, OrderItemDTO.class);
        itemTypeMap.addMappings(mapper -> mapper.map(OrderItem::getProduct, OrderItemDTO::setProductDTO));

        return modelMapper;
    }
}
