package com.werkflow.inventory.repository;

import com.werkflow.inventory.entity.AssetInstance;
import com.werkflow.inventory.entity.AssetInstance.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetInstanceRepository extends JpaRepository<AssetInstance, Long> {

    Optional<AssetInstance> findByAssetTag(String assetTag);

    boolean existsByAssetTag(String assetTag);

    List<AssetInstance> findByAssetDefinitionId(Long assetDefinitionId);

    List<AssetInstance> findByStatus(AssetStatus status);

    @Query("SELECT ai FROM AssetInstance ai WHERE ai.assetDefinition.id = :defId AND ai.status = :status")
    List<AssetInstance> findByAssetDefinitionIdAndStatus(
        @Param("defId") Long assetDefinitionId,
        @Param("status") AssetStatus status
    );

    @Query("SELECT ai FROM AssetInstance ai WHERE " +
           "LOWER(ai.assetTag) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ai.serialNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ai.assetDefinition.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<AssetInstance> searchByTagOrSerialOrName(@Param("searchTerm") String searchTerm);

    @Query("SELECT COUNT(ai) FROM AssetInstance ai WHERE ai.assetDefinition.id = :defId AND ai.status = 'AVAILABLE'")
    Long countAvailableByDefinition(@Param("defId") Long assetDefinitionId);

    @Query("SELECT ai FROM AssetInstance ai " +
           "JOIN ai.assetDefinition ad " +
           "WHERE ad.requiresMaintenance = true AND ai.status IN ('IN_USE', 'AVAILABLE')")
    List<AssetInstance> findAllRequiringMaintenance();
}
