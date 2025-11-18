package com.werkflow.inventory.repository;

import com.werkflow.inventory.entity.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AssetCategory entity
 */
@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {

    /**
     * Find category by code
     */
    Optional<AssetCategory> findByCode(String code);

    /**
     * Find category by name
     */
    Optional<AssetCategory> findByName(String name);

    /**
     * Find all active categories
     */
    List<AssetCategory> findByActiveTrue();

    /**
     * Find all inactive categories
     */
    List<AssetCategory> findByActiveFalse();

    /**
     * Find root categories (no parent)
     */
    @Query("SELECT c FROM AssetCategory c WHERE c.parentCategory IS NULL AND c.active = true")
    List<AssetCategory> findRootCategories();

    /**
     * Find child categories by parent ID
     */
    List<AssetCategory> findByParentCategoryIdAndActiveTrue(Long parentCategoryId);

    /**
     * Find categories by custodian department
     */
    List<AssetCategory> findByPrimaryCustodianDeptIdAndActiveTrue(Long deptId);

    /**
     * Find categories requiring approval
     */
    List<AssetCategory> findByRequiresApprovalTrueAndActiveTrue();

    /**
     * Search categories by name or code
     */
    @Query("SELECT c FROM AssetCategory c WHERE " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND c.active = true")
    List<AssetCategory> searchCategories(@Param("searchTerm") String searchTerm);
}
