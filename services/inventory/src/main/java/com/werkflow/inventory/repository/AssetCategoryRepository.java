package com.werkflow.inventory.repository;

import com.werkflow.inventory.entity.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {

    List<AssetCategory> findByParentCategoryId(Long parentCategoryId);

    List<AssetCategory> findByParentCategoryIsNull();

    List<AssetCategory> findByPrimaryCustodianDeptId(Long deptId);

    Optional<AssetCategory> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT c FROM AssetCategory c WHERE c.active = :active")
    List<AssetCategory> findByActive(@Param("active") Boolean active);

    @Query("SELECT c FROM AssetCategory c WHERE c.primaryCustodianDeptId = :deptId AND c.active = :active")
    List<AssetCategory> findByPrimaryCustodianDeptIdAndActive(
        @Param("deptId") Long deptId,
        @Param("active") Boolean active
    );
}
