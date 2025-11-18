package com.werkflow.controller;

import com.werkflow.dto.VendorDto;
import com.werkflow.dto.VendorResponseDto;
import com.werkflow.entity.VendorStatus;
import com.werkflow.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST Controller for Vendor operations
 */
@RestController
@RequestMapping("/procurement/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Vendor management APIs")
public class VendorController {

    private final VendorService vendorService;

    @PostMapping
    @Operation(summary = "Create vendor", description = "Create a new vendor in the system")
    public ResponseEntity<VendorResponseDto> createVendor(
            @Valid @RequestBody VendorDto vendorDto,
            Authentication authentication) {
        String createdBy = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(vendorService.createVendor(vendorDto, createdBy));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vendor by ID", description = "Retrieve a vendor by its ID")
    public ResponseEntity<VendorResponseDto> getVendorById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getVendorById(id));
    }

    @GetMapping("/code/{vendorCode}")
    @Operation(summary = "Get vendor by code", description = "Retrieve a vendor by its vendor code")
    public ResponseEntity<VendorResponseDto> getVendorByCode(@PathVariable String vendorCode) {
        return ResponseEntity.ok(vendorService.getVendorByCode(vendorCode));
    }

    @GetMapping
    @Operation(summary = "Get all vendors", description = "Retrieve all vendors with pagination")
    public ResponseEntity<Page<VendorResponseDto>> getAllVendors(Pageable pageable) {
        return ResponseEntity.ok(vendorService.getVendors(pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get vendors by status", description = "Retrieve vendors by status")
    public ResponseEntity<List<VendorResponseDto>> getVendorsByStatus(@PathVariable VendorStatus status) {
        return ResponseEntity.ok(vendorService.getVendorsByStatus(status));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active vendors", description = "Retrieve all active and approved vendors")
    public ResponseEntity<List<VendorResponseDto>> getActiveVendors() {
        return ResponseEntity.ok(vendorService.getActiveVendors());
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Get vendors by city", description = "Retrieve vendors from a specific city")
    public ResponseEntity<List<VendorResponseDto>> getVendorsByCity(@PathVariable String city) {
        return ResponseEntity.ok(vendorService.getVendorsByCity(city));
    }

    @GetMapping("/country/{country}")
    @Operation(summary = "Get vendors by country", description = "Retrieve vendors from a specific country")
    public ResponseEntity<List<VendorResponseDto>> getVendorsByCountry(@PathVariable String country) {
        return ResponseEntity.ok(vendorService.getVendorsByCountry(country));
    }

    @GetMapping("/search")
    @Operation(summary = "Search vendors", description = "Search vendors by name, contact person, or code")
    public ResponseEntity<Page<VendorResponseDto>> searchVendors(
            @RequestParam String searchTerm,
            Pageable pageable) {
        return ResponseEntity.ok(vendorService.searchVendors(searchTerm, pageable));
    }

    @GetMapping("/rating/{minRating}")
    @Operation(summary = "Get vendors by minimum rating", description = "Retrieve vendors with rating greater than or equal to specified value")
    public ResponseEntity<List<VendorResponseDto>> getVendorsByMinimumRating(@PathVariable BigDecimal minRating) {
        return ResponseEntity.ok(vendorService.getVendorsByMinimumRating(minRating));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update vendor", description = "Update an existing vendor")
    public ResponseEntity<VendorResponseDto> updateVendor(
            @PathVariable Long id,
            @Valid @RequestBody VendorDto vendorDto) {
        return ResponseEntity.ok(vendorService.updateVendor(id, vendorDto));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve vendor", description = "Approve a pending vendor")
    public ResponseEntity<VendorResponseDto> approveVendor(
            @PathVariable Long id,
            Authentication authentication) {
        String approvedBy = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(vendorService.approveVendor(id, approvedBy));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject vendor", description = "Reject a vendor (blacklist)")
    public ResponseEntity<VendorResponseDto> rejectVendor(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.rejectVendor(id));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate vendor", description = "Deactivate a vendor")
    public ResponseEntity<VendorResponseDto> deactivateVendor(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.deactivateVendor(id));
    }
}
