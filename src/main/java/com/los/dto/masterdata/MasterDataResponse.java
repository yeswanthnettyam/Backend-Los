package com.los.dto.masterdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Consolidated response containing all master data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterDataResponse {
    
    private List<PartnerDTO> partners;
    private List<ProductDTO> products;
    private List<BranchDTO> branches;
}
