package com.werkflow.inventory.repository;

import com.werkflow.inventory.entity.AssetDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetDefinitionRepository extends JpaRepository<AssetDefinition, Long> {

    List<AssetDefinition> findByCategoryId(Long categoryId);

    Optional<AssetDefinition> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("SELECT d FROM AssetDefinition d WHERE d.active = :active")
    List<AssetDefinition> findByActive(@Param("active") Boolean active);

    @Query("SELECT d FROM AssetDefinition d WHERE d.category.id = :categoryId AND d.active = :active")
    List<AssetDefinition> findByCategoryIdAndActive(
        @Param("categoryId") Long categoryId,
        @Param("active") Boolean active
    );

    @Query("SELECT d FROM AssetDefinition d WHERE " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.manufacturer) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.model) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<AssetDefinition> searchByNameOrManufacturerOrModel(@Param("searchTerm") String searchTerm);

    @Query("SELECT d FROM AssetDefinition d WHERE d.requiresMaintenance = true AND d.active = true")
    List<AssetDefinition> findAllRequiringMaintenance();
}
