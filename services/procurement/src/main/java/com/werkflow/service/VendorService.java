package com.werkflow.service;

import com.werkflow.dto.VendorDto;
import com.werkflow.dto.VendorResponseDto;
import com.werkflow.entity.Vendor;
import com.werkflow.entity.VendorStatus;
import com.werkflow.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Vendor operations
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VendorService {

    private final VendorRepository vendorRepository;

    /**
     * Create a new vendor
     */
    public VendorResponseDto createVendor(VendorDto vendorDto, String createdBy) {
        log.info("Creating new vendor from user: {}", createdBy);

        if (vendorRepository.findByVendorCode(vendorDto.getVendorCode()).isPresent()) {
            throw new IllegalArgumentException("Vendor with code " + vendorDto.getVendorCode() + " already exists");
        }

        Vendor vendor = Vendor.builder()
            .vendorCode(vendorDto.getVendorCode())
            .vendorName(vendorDto.getVendorName())
            .description(vendorDto.getDescription())
            .contactPerson(vendorDto.getContactPerson())
            .email(vendorDto.getEmail())
            .phone(vendorDto.getPhone())
            .website(vendorDto.getWebsite())
            .address(vendorDto.getAddress())
            .city(vendorDto.getCity())
            .state(vendorDto.getState())
            .postalCode(vendorDto.getPostalCode())
            .country(vendorDto.getCountry())
            .taxId(vendorDto.getTaxId())
            .paymentTerms(vendorDto.getPaymentTerms())
            .deliveryLeadTimeDays(vendorDto.getDeliveryLeadTimeDays())
            .minimumOrderAmount(vendorDto.getMinimumOrderAmount())
            .status(VendorStatus.PENDING_APPROVAL)
            .rating(BigDecimal.ZERO)
            .totalPurchases(BigDecimal.ZERO)
            .isActive(true)
            .build();

        Vendor saved = vendorRepository.save(vendor);
        log.info("Vendor created successfully with code: {}", saved.getVendorCode());

        return convertToResponseDto(saved);
    }

    /**
     * Get vendor by ID
     */
    @Transactional(readOnly = true)
    public VendorResponseDto getVendorById(Long id) {
        Vendor vendor = vendorRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + id));
        return convertToResponseDto(vendor);
    }

    /**
     * Get vendor by vendor code
     */
    @Transactional(readOnly = true)
    public VendorResponseDto getVendorByCode(String vendorCode) {
        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found with code: " + vendorCode));
        return convertToResponseDto(vendor);
    }

    /**
     * Get all vendors
     */
    @Transactional(readOnly = true)
    public List<VendorResponseDto> getAllVendors() {
        return vendorRepository.findAll().stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get vendors by status
     */
    @Transactional(readOnly = true)
    public List<VendorResponseDto> getVendorsByStatus(VendorStatus status) {
        return vendorRepository.findByStatus(status).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get all active vendors
     */
    @Transactional(readOnly = true)
    public List<VendorResponseDto> getActiveVendors() {
        return vendorRepository.findByStatusAndIsActive(VendorStatus.ACTIVE, true).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get vendors by city
     */
    @Transactional(readOnly = true)
    public List<VendorResponseDto> getVendorsByCity(String city) {
        return vendorRepository.findByCity(city).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get vendors by country
     */
    @Transactional(readOnly = true)
    public List<VendorResponseDto> getVendorsByCountry(String country) {
        return vendorRepository.findByCountry(country).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get vendors with pagination
     */
    @Transactional(readOnly = true)
    public Page<VendorResponseDto> getVendors(Pageable pageable) {
        return vendorRepository.findAll(pageable)
            .map(this::convertToResponseDto);
    }

    /**
     * Search vendors
     */
    @Transactional(readOnly = true)
    public Page<VendorResponseDto> searchVendors(String searchTerm, Pageable pageable) {
        return vendorRepository.searchByNameOrContactOrCode(searchTerm, pageable)
            .map(this::convertToResponseDto);
    }

    /**
     * Update vendor
     */
    public VendorResponseDto updateVendor(Long id, VendorDto vendorDto) {
        log.info("Updating vendor with id: {}", id);

        Vendor vendor = vendorRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + id));

        vendor.setVendorName(vendorDto.getVendorName());
        vendor.setDescription(vendorDto.getDescription());
        vendor.setContactPerson(vendorDto.getContactPerson());
        vendor.setEmail(vendorDto.getEmail());
        vendor.setPhone(vendorDto.getPhone());
        vendor.setWebsite(vendorDto.getWebsite());
        vendor.setAddress(vendorDto.getAddress());
        vendor.setCity(vendorDto.getCity());
        vendor.setState(vendorDto.getState());
        vendor.setPostalCode(vendorDto.getPostalCode());
        vendor.setCountry(vendorDto.getCountry());
        vendor.setTaxId(vendorDto.getTaxId());
        vendor.setPaymentTerms(vendorDto.getPaymentTerms());
        vendor.setDeliveryLeadTimeDays(vendorDto.getDeliveryLeadTimeDays());
        vendor.setMinimumOrderAmount(vendorDto.getMinimumOrderAmount());

        Vendor updated = vendorRepository.save(vendor);
        log.info("Vendor updated successfully with id: {}", id);

        return convertToResponseDto(updated);
    }

    /**
     * Approve vendor
     */
    public VendorResponseDto approveVendor(Long id, String approvedBy) {
        log.info("Approving vendor with id: {}", id);

        Vendor vendor = vendorRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + id));

        vendor.setStatus(VendorStatus.ACTIVE);
        vendor.setApprovedBy(approvedBy);
        vendor.setApprovedAt(LocalDate.now());

        Vendor updated = vendorRepository.save(vendor);
        log.info("Vendor approved with id: {}", id);

        return convertToResponseDto(updated);
    }

    /**
     * Reject vendor
     */
    public VendorResponseDto rejectVendor(Long id) {
        log.info("Rejecting vendor with id: {}", id);

        Vendor vendor = vendorRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + id));

        vendor.setStatus(VendorStatus.BLACKLISTED);

        Vendor updated = vendorRepository.save(vendor);
        log.info("Vendor rejected with id: {}", id);

        return convertToResponseDto(updated);
    }

    /**
     * Deactivate vendor
     */
    public VendorResponseDto deactivateVendor(Long id) {
        log.info("Deactivating vendor with id: {}", id);

        Vendor vendor = vendorRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Vendor not found with id: " + id));

        vendor.setIsActive(false);
        vendor.setStatus(VendorStatus.INACTIVE);

        Vendor updated = vendorRepository.save(vendor);
        log.info("Vendor deactivated with id: {}", id);

        return convertToResponseDto(updated);
    }

    /**
     * Get vendors with minimum rating
     */
    @Transactional(readOnly = true)
    public List<VendorResponseDto> getVendorsByMinimumRating(BigDecimal minRating) {
        return vendorRepository.findByMinimumRating(minRating).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Convert entity to DTO
     */
    private VendorResponseDto convertToResponseDto(Vendor vendor) {
        return VendorResponseDto.builder()
            .id(vendor.getId())
            .vendorCode(vendor.getVendorCode())
            .vendorName(vendor.getVendorName())
            .description(vendor.getDescription())
            .contactPerson(vendor.getContactPerson())
            .email(vendor.getEmail())
            .phone(vendor.getPhone())
            .website(vendor.getWebsite())
            .address(vendor.getAddress())
            .city(vendor.getCity())
            .state(vendor.getState())
            .postalCode(vendor.getPostalCode())
            .country(vendor.getCountry())
            .taxId(vendor.getTaxId())
            .status(vendor.getStatus())
            .rating(vendor.getRating())
            .totalPurchases(vendor.getTotalPurchases())
            .paymentTerms(vendor.getPaymentTerms())
            .deliveryLeadTimeDays(vendor.getDeliveryLeadTimeDays())
            .minimumOrderAmount(vendor.getMinimumOrderAmount())
            .approvedBy(vendor.getApprovedBy())
            .approvedAt(vendor.getApprovedAt())
            .lastPurchaseDate(vendor.getLastPurchaseDate())
            .isActive(vendor.getIsActive())
            .createdAt(vendor.getCreatedAt())
            .updatedAt(vendor.getUpdatedAt())
            .createdBy(vendor.getCreatedBy())
            .updatedBy(vendor.getUpdatedBy())
            .build();
    }
}
