package com.ecommerce.project.service;

import com.ecommerce.project.model.PriceHistory;
import com.ecommerce.project.payload.PriceHistoryDTO;

import java.util.List;


public interface PriceHistoryService {
    void savePriceHistory(PriceHistory priceHistory);
    List<PriceHistoryDTO> getHistoricalPricesByProduct(Long productId);
}
