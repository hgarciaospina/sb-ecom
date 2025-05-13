package com.ecommerce.project.controller;

import com.ecommerce.project.payload.PriceHistoryDTO;
import com.ecommerce.project.service.PriceHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for handling HTTP requests related to price history.
 * Provides endpoints to retrieve price history data for products.
 */
@RestController
@RequestMapping("/api")
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    @Autowired
    public PriceHistoryController(PriceHistoryService priceHistoryService) {
        this.priceHistoryService = priceHistoryService;
    }

    /**
     * Retrieves the price history for a specific product.
     *
     * @param productId the ID of the product whose price history is to be retrieved
     * @return a ResponseEntity containing a list of PriceHistoryDTO objects
     */
    @GetMapping("/admin/product/{productId}/price-history")
    public ResponseEntity<List<PriceHistoryDTO>> getPriceHistory(@PathVariable Long productId) {
        List<PriceHistoryDTO> historicalPrices = priceHistoryService.getHistoricalPricesByProduct(productId);
        return ResponseEntity.ok(historicalPrices);
    }
}
