package com.los.repository;

import com.los.domain.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
    
    List<Applicant> findByApplicationId(Long applicationId);
    
    Optional<Applicant> findByMobile(String mobile);
    
    Optional<Applicant> findByPanNumber(String panNumber);
}

