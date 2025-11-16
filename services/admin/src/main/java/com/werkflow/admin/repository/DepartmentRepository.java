package com.werkflow.admin.repository;

import com.werkflow.admin.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByOrganizationId(Long organizationId);

    List<Department> findByParentDepartmentId(Long parentDepartmentId);

    Optional<Department> findByOrganizationIdAndCode(Long organizationId, String code);

    List<Department> findByOrganizationIdAndParentDepartmentIsNull(Long organizationId);

    @Query("SELECT d FROM Department d WHERE d.organization.id = :orgId AND d.active = :active")
    List<Department> findByOrganizationIdAndActive(@Param("orgId") Long orgId, @Param("active") Boolean active);

    boolean existsByOrganizationIdAndCode(Long organizationId, String code);
}
