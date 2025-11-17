package com.werkflow.dto;

import com.werkflow.entity.VendorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Vendor response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorResponseDto {

    private Long id;

    private String vendorCode;

    private String vendorName;

    private String description;

    private String contactPerson;

    private String email;

    private String phone;

    private String website;

    private String address;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private String taxId;

    private VendorStatus status;

    private BigDecimal rating;

    private BigDecimal totalPurchases;

    private String paymentTerms;

    private Integer deliveryLeadTimeDays;

    private BigDecimal minimumOrderAmount;

    private String approvedBy;

    private LocalDate approvedAt;

    private LocalDate lastPurchaseDate;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
