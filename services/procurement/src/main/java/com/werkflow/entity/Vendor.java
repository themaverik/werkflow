package com.werkflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Vendor Entity
 * Represents a vendor/supplier in the procurement system
 */
@Entity
@Table(name = "vendors", indexes = {
    @Index(name = "idx_vendor_code", columnList = "vendor_code"),
    @Index(name = "idx_vendor_status", columnList = "status"),
    @Index(name = "idx_vendor_name", columnList = "vendor_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor extends BaseEntity {

    @NotBlank(message = "Vendor code is required")
    @Size(max = 50, message = "Vendor code cannot exceed 50 characters")
    @Column(name = "vendor_code", nullable = false, unique = true, length = 50)
    private String vendorCode;

    @NotBlank(message = "Vendor name is required")
    @Size(min = 3, max = 255, message = "Vendor name must be between 3 and 255 characters")
    @Column(name = "vendor_name", nullable = false, length = 255)
    private String vendorName;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(name = "description", length = 1000)
    private String description;

    @NotBlank(message = "Contact person is required")
    @Size(max = 255, message = "Contact person cannot exceed 255 characters")
    @Column(name = "contact_person", nullable = false, length = 255)
    private String contactPerson;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Size(max = 100, message = "Website cannot exceed 100 characters")
    @Column(name = "website", length = 100)
    private String website;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    @Column(name = "address", length = 500)
    private String address;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    @Column(name = "city", length = 100)
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    @Column(name = "state", length = 100)
    private String state;

    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Size(max = 100, message = "Country cannot exceed 100 characters")
    @Column(name = "country", length = 100)
    private String country;

    @Size(max = 50, message = "Tax ID cannot exceed 50 characters")
    @Column(name = "tax_id", length = 50)
    private String taxId;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VendorStatus status = VendorStatus.PENDING_APPROVAL;

    @DecimalMin(value = "0.00", message = "Rating must be between 0 and 5")
    @DecimalMax(value = "5.00", message = "Rating must be between 0 and 5")
    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "total_purchases", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPurchases = BigDecimal.ZERO;

    @Column(name = "payment_terms")
    private String paymentTerms;

    @Column(name = "delivery_lead_time_days")
    private Integer deliveryLeadTimeDays;

    @Column(name = "minimum_order_amount", precision = 15, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDate approvedAt;

    @Column(name = "last_purchase_date")
    private LocalDate lastPurchaseDate;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
