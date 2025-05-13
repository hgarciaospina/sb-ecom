package com.ecommerce.project.service;

import com.ecommerce.project.model.PriceHistory;
import com.ecommerce.project.payload.PriceHistoryDTO;
import com.ecommerce.project.repositories.PriceHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for managing price history records.
 * Provides methods to save and retrieve price history data.
 */
@Service
public class PriceHistoryServiceImpl implements PriceHistoryService {

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    /**
     * Saves a new price history record to the database.
     *
     * @param priceHistory the PriceHistory entity to be saved
     */
    @Override
    public void savePriceHistory(PriceHistory priceHistory) {
        priceHistoryRepository.save(priceHistory);
    }

    /**
     * Retrieves a list of PriceHistoryDTO records for a specific product,
     * ordered by the date of change in descending order.
     *
     * @param productId the ID of the product whose price history is to be retrieved
     * @return a list of PriceHistoryDTO objects
     */
    @Override
    public List<PriceHistoryDTO> getHistoricalPricesByProduct(Long productId) {
        List<PriceHistory> historicalPrices = priceHistoryRepository.findByProductIdOrderByChangedAtDesc(productId);

        return historicalPrices.stream()
                .map(h -> PriceHistoryDTO.builder()
                        .id(h.getId())
                        .oldPrice(h.getOldPrice())
                        .newPrice(h.getNewPrice())
                        .changedAt(h.getChangedAt())
                        .changedByUsername(h.getChangedBy().getUserName())
                        .productId(h.getProduct().getProductId())
                        .productName(h.getProduct().getProductName())
                        .build())
                .toList();
    }
}