package com.werkflow.repository;

import com.werkflow.entity.Vendor;
import com.werkflow.entity.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Vendor entity
 */
@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    /**
     * Find a vendor by vendor code
     */
    Optional<Vendor> findByVendorCode(String vendorCode);

    /**
     * Find vendors by status
     */
    List<Vendor> findByStatus(VendorStatus status);

    /**
     * Find vendors by status with pagination
     */
    Page<Vendor> findByStatus(VendorStatus status, Pageable pageable);

    /**
     * Find all active vendors
     */
    List<Vendor> findByIsActive(Boolean isActive);

    /**
     * Find vendors by city
     */
    List<Vendor> findByCity(String city);

    /**
     * Find vendors by country
     */
    List<Vendor> findByCountry(String country);

    /**
     * Find active vendors by status
     */
    List<Vendor> findByStatusAndIsActive(VendorStatus status, Boolean isActive);

    /**
     * Search vendors by name or contact person
     */
    @Query("SELECT v FROM Vendor v WHERE " +
           "LOWER(v.vendorName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.contactPerson) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.vendorCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Vendor> searchByNameOrContactOrCode(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find vendors with minimum rating
     */
    @Query("SELECT v FROM Vendor v WHERE v.rating >= :minRating AND v.isActive = true")
    List<Vendor> findByMinimumRating(@Param("minRating") java.math.BigDecimal minRating);

    /**
     * Find vendors approved by specific user
     */
    List<Vendor> findByApprovedBy(String approvedBy);

    /**
     * Count vendors by status
     */
    long countByStatus(VendorStatus status);
}
