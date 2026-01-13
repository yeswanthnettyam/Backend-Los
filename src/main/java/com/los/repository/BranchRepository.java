package com.los.repository;

import com.los.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Branch master data.
 */
@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {

    /**
     * Find all active branches ordered by branch name.
     */
    List<Branch> findByIsActiveTrueOrderByBranchNameAsc();
}
