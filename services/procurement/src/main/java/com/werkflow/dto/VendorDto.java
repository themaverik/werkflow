package com.werkflow.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Vendor creation and update
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorDto {

    @NotBlank(message = "Vendor code is required")
    @Size(max = 50, message = "Vendor code cannot exceed 50 characters")
    private String vendorCode;

    @NotBlank(message = "Vendor name is required")
    @Size(min = 3, max = 255, message = "Vendor name must be between 3 and 255 characters")
    private String vendorName;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Contact person is required")
    @Size(max = 255, message = "Contact person cannot exceed 255 characters")
    private String contactPerson;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;

    @Size(max = 100, message = "Website cannot exceed 100 characters")
    private String website;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;

    @Size(max = 100, message = "Country cannot exceed 100 characters")
    private String country;

    @Size(max = 50, message = "Tax ID cannot exceed 50 characters")
    private String taxId;

    private String paymentTerms;

    private Integer deliveryLeadTimeDays;

    private BigDecimal minimumOrderAmount;
}
