package com.los.mapping.transformers;

import com.los.mapping.FieldTransformer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Example transformer that concatenates first, middle, and last names.
 */
@Component("fullNameTransformer")
public class FullNameTransformer implements FieldTransformer {

    @Override
    public Object transform(Map<String, Object> formData, List<String> sourceFields) {
        StringBuilder fullName = new StringBuilder();
        
        for (String fieldId : sourceFields) {
            Object value = formData.get(fieldId);
            if (value != null && !value.toString().isEmpty()) {
                if (fullName.length() > 0) {
                    fullName.append(" ");
                }
                fullName.append(value.toString());
            }
        }
        
        return fullName.toString();
    }
}

