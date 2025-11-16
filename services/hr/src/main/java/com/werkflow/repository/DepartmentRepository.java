package com.werkflow.repository;

import com.werkflow.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Department entity
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByCode(String code);

    Optional<Department> findByName(String name);

    List<Department> findByIsActive(Boolean isActive);

    List<Department> findByParentDepartmentIsNull();

    List<Department> findByParentDepartmentId(Long parentId);

    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.employees WHERE d.id = :id")
    Optional<Department> findByIdWithEmployees(Long id);

    boolean existsByCode(String code);

    boolean existsByName(String name);
}
