package com.whrkflow.service;

import com.whrkflow.dto.DepartmentRequest;
import com.whrkflow.dto.DepartmentResponse;
import com.whrkflow.entity.Department;
import com.whrkflow.repository.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Department operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<DepartmentResponse> getAllDepartments() {
        log.debug("Fetching all departments");
        return departmentRepository.findAll().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public DepartmentResponse getDepartmentById(Long id) {
        log.debug("Fetching department by id: {}", id);
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));
        return convertToResponse(department);
    }

    public DepartmentResponse getDepartmentByCode(String code) {
        log.debug("Fetching department by code: {}", code);
        Department department = departmentRepository.findByCode(code)
            .orElseThrow(() -> new EntityNotFoundException("Department not found with code: " + code));
        return convertToResponse(department);
    }

    public List<DepartmentResponse> getActiveDepartments() {
        log.debug("Fetching active departments");
        return departmentRepository.findByIsActive(true).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<DepartmentResponse> getRootDepartments() {
        log.debug("Fetching root departments");
        return departmentRepository.findByParentDepartmentIsNull().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<DepartmentResponse> getSubDepartments(Long parentId) {
        log.debug("Fetching sub-departments for parent: {}", parentId);
        return departmentRepository.findByParentDepartmentId(parentId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        log.info("Creating new department: {}", request.getName());

        // Validate uniqueness
        if (request.getCode() != null && departmentRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Department code already exists: " + request.getCode());
        }
        if (departmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Department name already exists: " + request.getName());
        }

        Department department = convertToEntity(request);
        Department savedDepartment = departmentRepository.save(department);
        log.info("Department created successfully with id: {}", savedDepartment.getId());
        return convertToResponse(savedDepartment);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        log.info("Updating department with id: {}", id);

        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));

        // Validate uniqueness (excluding current department)
        if (request.getCode() != null && !request.getCode().equals(department.getCode())
            && departmentRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Department code already exists: " + request.getCode());
        }
        if (!request.getName().equals(department.getName())
            && departmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Department name already exists: " + request.getName());
        }

        updateEntityFromRequest(department, request);
        Department updatedDepartment = departmentRepository.save(department);
        log.info("Department updated successfully: {}", id);
        return convertToResponse(updatedDepartment);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        log.info("Deleting department with id: {}", id);
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));

        // Check if department has employees
        if (!department.getEmployees().isEmpty()) {
            throw new IllegalStateException("Cannot delete department with active employees");
        }

        departmentRepository.delete(department);
        log.info("Department deleted successfully: {}", id);
    }

    private Department convertToEntity(DepartmentRequest request) {
        Department department = Department.builder()
            .name(request.getName())
            .description(request.getDescription())
            .code(request.getCode())
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();

        if (request.getParentDepartmentId() != null) {
            Department parent = departmentRepository.findById(request.getParentDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException(
                    "Parent department not found with id: " + request.getParentDepartmentId()));
            department.setParentDepartment(parent);
        }

        return department;
    }

    private void updateEntityFromRequest(Department department, DepartmentRequest request) {
        department.setName(request.getName());
        department.setDescription(request.getDescription());
        department.setCode(request.getCode());
        department.setIsActive(request.getIsActive());

        if (request.getParentDepartmentId() != null) {
            Department parent = departmentRepository.findById(request.getParentDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException(
                    "Parent department not found with id: " + request.getParentDepartmentId()));
            department.setParentDepartment(parent);
        } else {
            department.setParentDepartment(null);
        }
    }

    private DepartmentResponse convertToResponse(Department department) {
        return DepartmentResponse.builder()
            .id(department.getId())
            .name(department.getName())
            .description(department.getDescription())
            .code(department.getCode())
            .isActive(department.getIsActive())
            .parentDepartmentId(department.getParentDepartment() != null ?
                department.getParentDepartment().getId() : null)
            .parentDepartmentName(department.getParentDepartment() != null ?
                department.getParentDepartment().getName() : null)
            .employeeCount(department.getEmployees().size())
            .createdAt(department.getCreatedAt())
            .updatedAt(department.getUpdatedAt())
            .build();
    }
}
