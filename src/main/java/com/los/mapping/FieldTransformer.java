package com.los.mapping;

import java.util.List;
import java.util.Map;

/**
 * Interface for field transformers.
 * Transformers convert UI field values to domain values.
 */
public interface FieldTransformer {

    /**
     * Transform source field values.
     * 
     * @param formData All form data
     * @param sourceFields The source field IDs
     * @return Transformed value
     */
    Object transform(Map<String, Object> formData, List<String> sourceFields);
}

