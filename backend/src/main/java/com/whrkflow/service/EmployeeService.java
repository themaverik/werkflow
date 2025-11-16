package com.whrkflow.service;

import com.whrkflow.dto.EmployeeRequest;
import com.whrkflow.dto.EmployeeResponse;
import com.whrkflow.entity.Department;
import com.whrkflow.entity.Employee;
import com.whrkflow.entity.EmploymentStatus;
import com.whrkflow.repository.DepartmentRepository;
import com.whrkflow.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Employee operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public List<EmployeeResponse> getAllEmployees() {
        log.debug("Fetching all employees");
        return employeeRepository.findAll().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public EmployeeResponse getEmployeeById(Long id) {
        log.debug("Fetching employee by id: {}", id);
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));
        return convertToResponse(employee);
    }

    public EmployeeResponse getEmployeeByCode(String employeeCode) {
        log.debug("Fetching employee by code: {}", employeeCode);
        Employee employee = employeeRepository.findByEmployeeCode(employeeCode)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found with code: " + employeeCode));
        return convertToResponse(employee);
    }

    public EmployeeResponse getEmployeeByEmail(String email) {
        log.debug("Fetching employee by email: {}", email);
        Employee employee = employeeRepository.findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found with email: " + email));
        return convertToResponse(employee);
    }

    public List<EmployeeResponse> getEmployeesByDepartment(Long departmentId) {
        log.debug("Fetching employees for department: {}", departmentId);
        return employeeRepository.findByDepartmentId(departmentId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<EmployeeResponse> getEmployeesByManager(Long managerId) {
        log.debug("Fetching employees for manager: {}", managerId);
        return employeeRepository.findByManagerId(managerId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<EmployeeResponse> getEmployeesByStatus(EmploymentStatus status) {
        log.debug("Fetching employees by status: {}", status);
        return employeeRepository.findByEmploymentStatus(status).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<EmployeeResponse> searchEmployees(String searchTerm) {
        log.debug("Searching employees with term: {}", searchTerm);
        return employeeRepository.searchEmployees(searchTerm).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        log.info("Creating new employee: {}", request.getEmployeeCode());

        // Validate uniqueness
        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new IllegalArgumentException("Employee code already exists: " + request.getEmployeeCode());
        }
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        Employee employee = convertToEntity(request);
        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee created successfully with id: {}", savedEmployee.getId());
        return convertToResponse(savedEmployee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        log.info("Updating employee with id: {}", id);

        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));

        // Validate uniqueness (excluding current employee)
        if (!request.getEmployeeCode().equals(employee.getEmployeeCode())
            && employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new IllegalArgumentException("Employee code already exists: " + request.getEmployeeCode());
        }
        if (!request.getEmail().equals(employee.getEmail())
            && employeeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        updateEntityFromRequest(employee, request);
        Employee updatedEmployee = employeeRepository.save(employee);
        log.info("Employee updated successfully: {}", id);
        return convertToResponse(updatedEmployee);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        log.info("Deleting employee with id: {}", id);
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));

        // Check if employee is a manager
        if (!employee.getSubordinates().isEmpty()) {
            throw new IllegalStateException("Cannot delete employee who is managing other employees");
        }

        employeeRepository.delete(employee);
        log.info("Employee deleted successfully: {}", id);
    }

    private Employee convertToEntity(EmployeeRequest request) {
        // Fetch department
        Department department = departmentRepository.findById(request.getDepartmentId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Department not found with id: " + request.getDepartmentId()));

        Employee employee = Employee.builder()
            .employeeCode(request.getEmployeeCode())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .dateOfBirth(request.getDateOfBirth())
            .joinDate(request.getJoinDate())
            .endDate(request.getEndDate())
            .jobTitle(request.getJobTitle())
            .employmentStatus(request.getEmploymentStatus())
            .salary(request.getSalary())
            .address(request.getAddress())
            .department(department)
            .build();

        // Set manager if provided
        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                .orElseThrow(() -> new EntityNotFoundException(
                    "Manager not found with id: " + request.getManagerId()));
            employee.setManager(manager);
        }

        return employee;
    }

    private void updateEntityFromRequest(Employee employee, EmployeeRequest request) {
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setJoinDate(request.getJoinDate());
        employee.setEndDate(request.getEndDate());
        employee.setJobTitle(request.getJobTitle());
        employee.setEmploymentStatus(request.getEmploymentStatus());
        employee.setSalary(request.getSalary());
        employee.setAddress(request.getAddress());

        // Update department
        Department department = departmentRepository.findById(request.getDepartmentId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Department not found with id: " + request.getDepartmentId()));
        employee.setDepartment(department);

        // Update manager
        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                .orElseThrow(() -> new EntityNotFoundException(
                    "Manager not found with id: " + request.getManagerId()));
            employee.setManager(manager);
        } else {
            employee.setManager(null);
        }
    }

    private EmployeeResponse convertToResponse(Employee employee) {
        return EmployeeResponse.builder()
            .id(employee.getId())
            .employeeCode(employee.getEmployeeCode())
            .firstName(employee.getFirstName())
            .lastName(employee.getLastName())
            .fullName(employee.getFullName())
            .email(employee.getEmail())
            .phoneNumber(employee.getPhoneNumber())
            .dateOfBirth(employee.getDateOfBirth())
            .joinDate(employee.getJoinDate())
            .endDate(employee.getEndDate())
            .jobTitle(employee.getJobTitle())
            .employmentStatus(employee.getEmploymentStatus())
            .salary(employee.getSalary())
            .address(employee.getAddress())
            .departmentId(employee.getDepartment().getId())
            .departmentName(employee.getDepartment().getName())
            .managerId(employee.getManager() != null ? employee.getManager().getId() : null)
            .managerName(employee.getManager() != null ? employee.getManager().getFullName() : null)
            .createdAt(employee.getCreatedAt())
            .updatedAt(employee.getUpdatedAt())
            .build();
    }
}
