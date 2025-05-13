package com.ecommerce.project.repositories;

import com.ecommerce.project.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for accessing PriceHistory entities in the database.
 * Provides methods to perform CRUD operations and custom queries.
 */
@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    /**
     * Retrieves a list of price history records for a specific product,
     * ordered by the date of change in descending order.
     *
     * @param productId the ID of the product whose price history is to be retrieved
     * @return a list of PriceHistory entities for the specified product
     */
    @Query("SELECT ph FROM PriceHistory ph WHERE ph.product.id = :productId ORDER BY ph.changedAt DESC")
    List<PriceHistory> findByProductIdOrderByChangedAtDesc(Long productId);
}
