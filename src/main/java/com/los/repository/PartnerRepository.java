package com.los.repository;

import com.los.domain.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, String> {
    
    List<Partner> findByIsActive(Boolean isActive);
}

