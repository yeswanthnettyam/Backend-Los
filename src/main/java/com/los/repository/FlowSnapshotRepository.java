package com.los.repository;

import com.los.config.entity.FlowSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlowSnapshotRepository extends JpaRepository<FlowSnapshot, Long> {
    
    Optional<FlowSnapshot> findByApplicationId(Long applicationId);
    
    List<FlowSnapshot> findByFlowConfigId(Long flowConfigId);
}

