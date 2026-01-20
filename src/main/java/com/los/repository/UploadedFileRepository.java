package com.los.repository;

import com.los.domain.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    
    List<UploadedFile> findByApplicationId(Long applicationId);
    
    List<UploadedFile> findByApplicationIdAndScreenId(Long applicationId, String screenId);
    
    List<UploadedFile> findByApplicationIdAndScreenIdAndFieldId(Long applicationId, String screenId, String fieldId);
    
    boolean existsByApplicationIdAndScreenIdAndFieldId(Long applicationId, String screenId, String fieldId);
}
