package com.werkflow.admin.service;

import com.werkflow.admin.dto.DepartmentRequest;
import com.werkflow.admin.dto.DepartmentResponse;
import com.werkflow.admin.entity.Department;
import com.werkflow.admin.entity.Organization;
import com.werkflow.admin.repository.DepartmentRepository;
import com.werkflow.admin.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        log.info("Creating department: {} for organization ID: {}", request.getName(), request.getOrganizationId());

        Organization organization = organizationRepository.findById(request.getOrganizationId())
            .orElseThrow(() -> new RuntimeException("Organization not found with ID: " + request.getOrganizationId()));

        if (request.getCode() != null &&
            departmentRepository.existsByOrganizationIdAndCode(request.getOrganizationId(), request.getCode())) {
            throw new RuntimeException("Department with code '" + request.getCode() + "' already exists in this organization");
        }

        Department.DepartmentBuilder builder = Department.builder()
            .name(request.getName())
            .code(request.getCode())
            .description(request.getDescription())
            .organization(organization)
            .managerUserId(request.getManagerUserId())
            .location(request.getLocation())
            .phone(request.getPhone())
            .email(request.getEmail())
            .active(request.getActive() != null ? request.getActive() : true);

        if (request.getParentDepartmentId() != null) {
            Department parent = departmentRepository.findById(request.getParentDepartmentId())
                .orElseThrow(() -> new RuntimeException("Parent department not found with ID: " + request.getParentDepartmentId()));
            builder.parentDepartment(parent);
        }

        Department department = builder.build();
        department = departmentRepository.save(department);
        log.info("Department created successfully with ID: {}", department.getId());

        return mapToResponse(department);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        log.debug("Fetching department with ID: {}", id);

        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Department not found with ID: " + id));

        return mapToResponse(department);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentsByOrganization(Long organizationId) {
        log.debug("Fetching departments for organization ID: {}", organizationId);

        return departmentRepository.findByOrganizationId(organizationId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getTopLevelDepartments(Long organizationId) {
        log.debug("Fetching top-level departments for organization ID: {}", organizationId);

        return departmentRepository.findByOrganizationIdAndParentDepartmentIsNull(organizationId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        log.info("Updating department with ID: {}", id);

        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Department not found with ID: " + id));

        if (request.getCode() != null &&
            !request.getCode().equals(department.getCode()) &&
            departmentRepository.existsByOrganizationIdAndCode(request.getOrganizationId(), request.getCode())) {
            throw new RuntimeException("Department with code '" + request.getCode() + "' already exists in this organization");
        }

        department.setName(request.getName());
        department.setCode(request.getCode());
        department.setDescription(request.getDescription());
        department.setManagerUserId(request.getManagerUserId());
        department.setLocation(request.getLocation());
        department.setPhone(request.getPhone());
        department.setEmail(request.getEmail());
        if (request.getActive() != null) {
            department.setActive(request.getActive());
        }

        if (request.getParentDepartmentId() != null) {
            Department parent = departmentRepository.findById(request.getParentDepartmentId())
                .orElseThrow(() -> new RuntimeException("Parent department not found with ID: " + request.getParentDepartmentId()));
            department.setParentDepartment(parent);
        }

        department = departmentRepository.save(department);
        log.info("Department updated successfully: {}", id);

        return mapToResponse(department);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        log.info("Deleting department with ID: {}", id);

        if (!departmentRepository.existsById(id)) {
            throw new RuntimeException("Department not found with ID: " + id);
        }

        departmentRepository.deleteById(id);
        log.info("Department deleted successfully: {}", id);
    }

    private DepartmentResponse mapToResponse(Department department) {
        return DepartmentResponse.builder()
            .id(department.getId())
            .name(department.getName())
            .code(department.getCode())
            .description(department.getDescription())
            .organizationId(department.getOrganization().getId())
            .organizationName(department.getOrganization().getName())
            .parentDepartmentId(department.getParentDepartment() != null ? department.getParentDepartment().getId() : null)
            .parentDepartmentName(department.getParentDepartment() != null ? department.getParentDepartment().getName() : null)
            .managerUserId(department.getManagerUserId())
            .location(department.getLocation())
            .phone(department.getPhone())
            .email(department.getEmail())
            .active(department.getActive())
            .childDepartments(department.getChildDepartments().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()))
            .createdAt(department.getCreatedAt())
            .updatedAt(department.getUpdatedAt())
            .build();
    }
}
