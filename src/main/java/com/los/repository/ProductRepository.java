package com.los.repository;

import com.los.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    
    List<Product> findByIsActive(Boolean isActive);
    
    /**
     * Find all active products ordered by product name.
     */
    List<Product> findByIsActiveTrueOrderByProductNameAsc();
}

