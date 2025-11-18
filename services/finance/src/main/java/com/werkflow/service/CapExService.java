package com.werkflow.service;

import com.werkflow.dto.CapExRequestDto;
import com.werkflow.dto.CapExResponseDto;
import com.werkflow.entity.*;
import com.werkflow.repository.CapExRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for CapEx Request operations
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CapExService {

    private final CapExRequestRepository capexRequestRepository;

    /**
     * Create a new CapEx request
     */
    public CapExResponseDto createCapExRequest(CapExRequestDto requestDto, String requestedBy) {
        log.info("Creating new CapEx request from user: {}", requestedBy);

        String requestNumber = generateRequestNumber();

        CapExRequest capexRequest = CapExRequest.builder()
            .requestNumber(requestNumber)
            .title(requestDto.getTitle())
            .description(requestDto.getDescription())
            .category(requestDto.getCategory())
            .amount(requestDto.getAmount())
            .priority(requestDto.getPriority())
            .approvalLevel(requestDto.getApprovalLevel())
            .requestedBy(requestedBy)
            .requestDate(LocalDate.now())
            .expectedCompletionDate(requestDto.getExpectedCompletionDate())
            .businessJustification(requestDto.getBusinessJustification())
            .expectedBenefits(requestDto.getExpectedBenefits())
            .budgetYear(requestDto.getBudgetYear())
            .departmentName(requestDto.getDepartmentName())
            .status(CapExStatus.SUBMITTED)
            .isActive(true)
            .build();

        CapExRequest saved = capexRequestRepository.save(capexRequest);
        log.info("CapEx request created successfully with request number: {}", saved.getRequestNumber());

        return convertToResponseDto(saved);
    }

    /**
     * Get CapEx request by ID
     */
    @Transactional(readOnly = true)
    public CapExResponseDto getCapExRequestById(Long id) {
        CapExRequest capexRequest = capexRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("CapEx request not found with id: " + id));
        return convertToResponseDto(capexRequest);
    }

    /**
     * Get CapEx request by request number
     */
    @Transactional(readOnly = true)
    public CapExResponseDto getCapExRequestByNumber(String requestNumber) {
        CapExRequest capexRequest = capexRequestRepository.findByRequestNumber(requestNumber)
            .orElseThrow(() -> new EntityNotFoundException("CapEx request not found with number: " + requestNumber));
        return convertToResponseDto(capexRequest);
    }

    /**
     * Get all CapEx requests
     */
    @Transactional(readOnly = true)
    public List<CapExResponseDto> getAllCapExRequests() {
        return capexRequestRepository.findAll().stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get CapEx requests by status
     */
    @Transactional(readOnly = true)
    public List<CapExResponseDto> getCapExRequestsByStatus(CapExStatus status) {
        return capexRequestRepository.findByStatus(status).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get CapEx requests by category
     */
    @Transactional(readOnly = true)
    public List<CapExResponseDto> getCapExRequestsByCategory(CapExCategory category) {
        return capexRequestRepository.findByCategory(category).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get CapEx requests by priority
     */
    @Transactional(readOnly = true)
    public List<CapExResponseDto> getCapExRequestsByPriority(Priority priority) {
        return capexRequestRepository.findByPriority(priority).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get CapEx requests by requested user
     */
    @Transactional(readOnly = true)
    public List<CapExResponseDto> getCapExRequestsByRequestedBy(String requestedBy) {
        return capexRequestRepository.findByRequestedBy(requestedBy).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get CapEx requests by department
     */
    @Transactional(readOnly = true)
    public List<CapExResponseDto> getCapExRequestsByDepartment(String departmentName) {
        return capexRequestRepository.findByDepartmentName(departmentName).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get CapEx requests by budget year
     */
    @Transactional(readOnly = true)
    public List<CapExResponseDto> getCapExRequestsByBudgetYear(Integer budgetYear) {
        return capexRequestRepository.findByBudgetYear(budgetYear).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get CapEx requests with pagination
     */
    @Transactional(readOnly = true)
    public Page<CapExResponseDto> getCapExRequests(Pageable pageable) {
        return capexRequestRepository.findAll(pageable)
            .map(this::convertToResponseDto);
    }

    /**
     * Search CapEx requests
     */
    @Transactional(readOnly = true)
    public Page<CapExResponseDto> searchCapExRequests(String searchTerm, Pageable pageable) {
        return capexRequestRepository.searchByTitleOrDescription(searchTerm, pageable)
            .map(this::convertToResponseDto);
    }

    /**
     * Update CapEx request
     */
    public CapExResponseDto updateCapExRequest(Long id, CapExRequestDto requestDto) {
        log.info("Updating CapEx request with id: {}", id);

        CapExRequest capexRequest = capexRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("CapEx request not found with id: " + id));

        if (!capexRequest.getStatus().equals(CapExStatus.SUBMITTED)) {
            throw new IllegalArgumentException("CapEx request cannot be updated once it has been reviewed");
        }

        capexRequest.setTitle(requestDto.getTitle());
        capexRequest.setDescription(requestDto.getDescription());
        capexRequest.setCategory(requestDto.getCategory());
        capexRequest.setAmount(requestDto.getAmount());
        capexRequest.setPriority(requestDto.getPriority());
        capexRequest.setApprovalLevel(requestDto.getApprovalLevel());
        capexRequest.setExpectedCompletionDate(requestDto.getExpectedCompletionDate());
        capexRequest.setBusinessJustification(requestDto.getBusinessJustification());
        capexRequest.setExpectedBenefits(requestDto.getExpectedBenefits());
        capexRequest.setBudgetYear(requestDto.getBudgetYear());
        capexRequest.setDepartmentName(requestDto.getDepartmentName());

        CapExRequest updated = capexRequestRepository.save(capexRequest);
        log.info("CapEx request updated successfully with id: {}", id);

        return convertToResponseDto(updated);
    }

    /**
     * Approve CapEx request
     */
    public CapExResponseDto approveCapExRequest(Long id, String approvedBy, String remarks) {
        log.info("Approving CapEx request with id: {}", id);

        CapExRequest capexRequest = capexRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("CapEx request not found with id: " + id));

        capexRequest.setStatus(CapExStatus.APPROVED);
        capexRequest.setApprovedBy(approvedBy);
        capexRequest.setApprovedAt(LocalDate.now());
        capexRequest.setApprovedAmount(capexRequest.getAmount());

        CapExRequest updated = capexRequestRepository.save(capexRequest);
        log.info("CapEx request approved with id: {}", id);

        return convertToResponseDto(updated);
    }

    /**
     * Reject CapEx request
     */
    public CapExResponseDto rejectCapExRequest(Long id, String rejectedBy, String rejectionReason) {
        log.info("Rejecting CapEx request with id: {}", id);

        CapExRequest capexRequest = capexRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("CapEx request not found with id: " + id));

        capexRequest.setStatus(CapExStatus.REJECTED);
        capexRequest.setRejectedBy(rejectedBy);
        capexRequest.setRejectedAt(LocalDate.now());
        capexRequest.setRejectionReason(rejectionReason);

        CapExRequest updated = capexRequestRepository.save(capexRequest);
        log.info("CapEx request rejected with id: {}", id);

        return convertToResponseDto(updated);
    }

    /**
     * Update CapEx request status
     */
    public CapExResponseDto updateCapExRequestStatus(Long id, CapExStatus newStatus) {
        log.info("Updating status of CapEx request with id: {} to {}", id, newStatus);

        CapExRequest capexRequest = capexRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("CapEx request not found with id: " + id));

        capexRequest.setStatus(newStatus);
        CapExRequest updated = capexRequestRepository.save(capexRequest);

        log.info("Status updated successfully for CapEx request with id: {}", id);
        return convertToResponseDto(updated);
    }

    /**
     * Get pending approvals
     */
    @Transactional(readOnly = true)
    public List<CapExResponseDto> getPendingApprovals() {
        return capexRequestRepository.findPendingApprovals().stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Convert entity to DTO
     */
    private CapExResponseDto convertToResponseDto(CapExRequest capexRequest) {
        return CapExResponseDto.builder()
            .id(capexRequest.getId())
            .requestNumber(capexRequest.getRequestNumber())
            .title(capexRequest.getTitle())
            .description(capexRequest.getDescription())
            .category(capexRequest.getCategory())
            .amount(capexRequest.getAmount())
            .priority(capexRequest.getPriority())
            .status(capexRequest.getStatus())
            .approvalLevel(capexRequest.getApprovalLevel())
            .requestedBy(capexRequest.getRequestedBy())
            .requestDate(capexRequest.getRequestDate())
            .expectedCompletionDate(capexRequest.getExpectedCompletionDate())
            .businessJustification(capexRequest.getBusinessJustification())
            .expectedBenefits(capexRequest.getExpectedBenefits())
            .budgetYear(capexRequest.getBudgetYear())
            .departmentName(capexRequest.getDepartmentName())
            .approvedAmount(capexRequest.getApprovedAmount())
            .approvedBy(capexRequest.getApprovedBy())
            .approvedAt(capexRequest.getApprovedAt())
            .rejectionReason(capexRequest.getRejectionReason())
            .rejectedBy(capexRequest.getRejectedBy())
            .rejectedAt(capexRequest.getRejectedAt())
            .workflowInstanceId(capexRequest.getWorkflowInstanceId())
            .isActive(capexRequest.getIsActive())
            .createdAt(capexRequest.getCreatedAt())
            .updatedAt(capexRequest.getUpdatedAt())
            .createdBy(capexRequest.getCreatedBy())
            .updatedBy(capexRequest.getUpdatedBy())
            .build();
    }

    /**
     * Generate unique request number
     */
    private String generateRequestNumber() {
        return "CAPEX-" + System.currentTimeMillis();
    }
}
