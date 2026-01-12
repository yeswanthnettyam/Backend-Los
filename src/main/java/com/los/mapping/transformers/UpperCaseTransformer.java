package com.los.mapping.transformers;

import com.los.mapping.FieldTransformer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Example transformer that converts text to uppercase.
 */
@Component("upperCaseTransformer")
public class UpperCaseTransformer implements FieldTransformer {

    @Override
    public Object transform(Map<String, Object> formData, List<String> sourceFields) {
        if (sourceFields.isEmpty()) {
            return null;
        }
        
        Object value = formData.get(sourceFields.get(0));
        return value != null ? value.toString().toUpperCase() : null;
    }
}

