package com.whrkflow.repository;

import com.whrkflow.entity.Employee;
import com.whrkflow.entity.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Employee entity
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByEmail(String email);

    List<Employee> findByDepartmentId(Long departmentId);

    List<Employee> findByManagerId(Long managerId);

    List<Employee> findByEmploymentStatus(EmploymentStatus status);

    @Query("SELECT e FROM Employee e WHERE e.department.id = :departmentId AND e.employmentStatus = :status")
    List<Employee> findByDepartmentIdAndStatus(@Param("departmentId") Long departmentId,
                                               @Param("status") EmploymentStatus status);

    @Query("SELECT e FROM Employee e WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(e.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Employee> searchEmployees(@Param("searchTerm") String searchTerm);

    @Query("SELECT e FROM Employee e WHERE e.joinDate BETWEEN :startDate AND :endDate")
    List<Employee> findByJoinDateBetween(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    long countByDepartmentId(Long departmentId);

    long countByEmploymentStatus(EmploymentStatus status);
}
