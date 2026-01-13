package com.los.dto.masterdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Partner master data in API response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerDTO {
    
    private String code;
    private String name;
}
