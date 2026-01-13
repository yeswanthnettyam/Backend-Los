package com.los.service;

import com.los.domain.Branch;
import com.los.domain.Partner;
import com.los.domain.Product;
import com.los.dto.masterdata.BranchDTO;
import com.los.dto.masterdata.MasterDataResponse;
import com.los.dto.masterdata.PartnerDTO;
import com.los.dto.masterdata.ProductDTO;
import com.los.repository.BranchRepository;
import com.los.repository.PartnerRepository;
import com.los.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing and retrieving master data.
 * This service provides a consolidated view of all master data (Partners, Products, Branches).
 * 
 * Data is cached for performance as master data is READ-HEAVY and WRITE-RARE.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterDataService {

    private final PartnerRepository partnerRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;

    /**
     * Get all master data in a single consolidated response.
     * Returns only ACTIVE records, sorted by name.
     * If no data found, returns empty arrays (not null, not 404).
     * 
     * Results are cached to improve performance.
     * Cache is evicted only on application restart.
     * 
     * @return consolidated master data response
     */
    @Cacheable(value = "masterData", key = "'all'")
    @Transactional(readOnly = true)
    public MasterDataResponse getAllMasterData() {
        log.info("Fetching all master data from database");
        
        // Fetch active partners
        List<Partner> partners = partnerRepository.findByIsActiveTrueOrderByPartnerNameAsc();
        List<PartnerDTO> partnerDTOs = partners.stream()
                .map(this::mapToPartnerDTO)
                .collect(Collectors.toList());
        
        // Fetch active products
        List<Product> products = productRepository.findByIsActiveTrueOrderByProductNameAsc();
        List<ProductDTO> productDTOs = products.stream()
                .map(this::mapToProductDTO)
                .collect(Collectors.toList());
        
        // Fetch active branches
        List<Branch> branches = branchRepository.findByIsActiveTrueOrderByBranchNameAsc();
        List<BranchDTO> branchDTOs = branches.stream()
                .map(this::mapToBranchDTO)
                .collect(Collectors.toList());
        
        log.info("Fetched {} partners, {} products, {} branches", 
                partnerDTOs.size(), productDTOs.size(), branchDTOs.size());
        
        return MasterDataResponse.builder()
                .partners(partnerDTOs.isEmpty() ? Collections.emptyList() : partnerDTOs)
                .products(productDTOs.isEmpty() ? Collections.emptyList() : productDTOs)
                .branches(branchDTOs.isEmpty() ? Collections.emptyList() : branchDTOs)
                .build();
    }

    /**
     * Map Partner entity to PartnerDTO.
     */
    private PartnerDTO mapToPartnerDTO(Partner partner) {
        return PartnerDTO.builder()
                .code(partner.getPartnerCode())
                .name(partner.getPartnerName())
                .build();
    }

    /**
     * Map Product entity to ProductDTO.
     */
    private ProductDTO mapToProductDTO(Product product) {
        return ProductDTO.builder()
                .code(product.getProductCode())
                .name(product.getProductName())
                .build();
    }

    /**
     * Map Branch entity to BranchDTO.
     */
    private BranchDTO mapToBranchDTO(Branch branch) {
        return BranchDTO.builder()
                .code(branch.getBranchCode())
                .name(branch.getBranchName())
                .partnerCode(branch.getPartnerCode())
                .build();
    }
}
