package com.los.mapping;

import com.los.domain.Applicant;
import com.los.domain.Business;
import com.los.domain.LoanApplication;
import com.los.repository.ApplicantRepository;
import com.los.repository.BusinessRepository;
import com.los.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Engine for mapping UI form data to domain entities.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FieldMappingEngine {

    private final LoanApplicationRepository loanApplicationRepository;
    private final ApplicantRepository applicantRepository;
    private final BusinessRepository businessRepository;
    private final Map<String, FieldTransformer> transformers;

    /**
     * Apply field mappings and persist to database.
     * 
     * @param applicationId The application ID
     * @param formData The form data
     * @param mappingConfig The mapping configuration
     */
    @SuppressWarnings("unchecked")
    public void applyMappings(Long applicationId, Map<String, Object> formData, Map<String, Object> mappingConfig) {
        List<Map<String, Object>> mappings = (List<Map<String, Object>>) mappingConfig.get("mappings");
        
        if (mappings == null || mappings.isEmpty()) {
            log.warn("No mappings defined in configuration");
            return;
        }

        for (Map<String, Object> mapping : mappings) {
            applyMapping(applicationId, formData, mapping);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyMapping(Long applicationId, Map<String, Object> formData, Map<String, Object> mapping) {
        String mappingType = (String) mapping.get("mappingType");
        List<String> sourceFields = (List<String>) mapping.get("sourceFields");
        Map<String, Object> target = (Map<String, Object>) mapping.get("target");
        String transformer = (String) mapping.get("transformer");

        String targetEntity = (String) target.get("entity");
        List<String> targetFields = (List<String>) target.get("fields");

        // Get values from form data
        Object value;
        if (transformer != null && transformers.containsKey(transformer)) {
            // Apply transformer
            FieldTransformer fieldTransformer = transformers.get(transformer);
            value = fieldTransformer.transform(formData, sourceFields);
        } else if (sourceFields.size() == 1) {
            // Direct mapping
            value = formData.get(sourceFields.get(0));
        } else {
            log.warn("Multiple source fields without transformer: {}", sourceFields);
            return;
        }

        // Map to entity
        mapToEntity(applicationId, targetEntity, targetFields.get(0), value);
    }

    private void mapToEntity(Long applicationId, String entityName, String fieldName, Object value) {
        switch (entityName) {
            case "LoanApplication" -> mapToLoanApplication(applicationId, fieldName, value);
            case "Applicant" -> mapToApplicant(applicationId, fieldName, value);
            case "Business" -> mapToBusiness(applicationId, fieldName, value);
            default -> log.warn("Unknown entity: {}", entityName);
        }
    }

    private void mapToLoanApplication(Long applicationId, String fieldName, Object value) {
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        switch (fieldName) {
            case "status" -> application.setStatus(value.toString());
            case "currentScreenId" -> application.setCurrentScreenId(value.toString());
            default -> log.warn("Unknown field for LoanApplication: {}", fieldName);
        }

        loanApplicationRepository.save(application);
    }

    private void mapToApplicant(Long applicationId, String fieldName, Object value) {
        Applicant applicant = applicantRepository.findByApplicationId(applicationId)
                .stream()
                .findFirst()
                .orElseGet(() -> Applicant.builder()
                        .applicationId(applicationId)
                        .build());

        switch (fieldName) {
            case "firstName" -> applicant.setFirstName(value != null ? value.toString() : null);
            case "middleName" -> applicant.setMiddleName(value != null ? value.toString() : null);
            case "lastName" -> applicant.setLastName(value != null ? value.toString() : null);
            case "mobile" -> applicant.setMobile(value != null ? value.toString() : null);
            case "email" -> applicant.setEmail(value != null ? value.toString() : null);
            case "dob" -> applicant.setDob(value != null ? parseDate(value.toString()) : null);
            case "gender" -> applicant.setGender(value != null ? value.toString() : null);
            case "panNumber" -> applicant.setPanNumber(value != null ? value.toString() : null);
            case "aadhaarNumber" -> applicant.setAadhaarNumber(value != null ? value.toString() : null);
            default -> log.warn("Unknown field for Applicant: {}", fieldName);
        }

        applicantRepository.save(applicant);
    }

    private void mapToBusiness(Long applicationId, String fieldName, Object value) {
        Business business = businessRepository.findByApplicationId(applicationId)
                .orElseGet(() -> Business.builder()
                        .applicationId(applicationId)
                        .build());

        switch (fieldName) {
            case "businessName" -> business.setBusinessName(value != null ? value.toString() : null);
            case "businessType" -> business.setBusinessType(value != null ? value.toString() : null);
            case "businessAddress" -> business.setBusinessAddress(value != null ? value.toString() : null);
            case "businessVintageMonths" -> business.setBusinessVintageMonths(value != null ? Integer.parseInt(value.toString()) : null);
            case "annualTurnover" -> business.setAnnualTurnover(value != null ? Double.parseDouble(value.toString()) : null);
            default -> log.warn("Unknown field for Business: {}", fieldName);
        }

        businessRepository.save(business);
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            log.error("Error parsing date: {}", dateStr, e);
            return null;
        }
    }
}

