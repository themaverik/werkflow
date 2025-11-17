package com.werkflow.service;

import com.werkflow.dto.PurchaseRequestDto;
import com.werkflow.dto.PurchaseRequestResponseDto;
import com.werkflow.entity.*;
import com.werkflow.repository.PurchaseRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Purchase Request operations
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProcurementService {

    private final PurchaseRequestRepository purchaseRequestRepository;

    /**
     * Create a new purchase request
     */
    public PurchaseRequestResponseDto createPurchaseRequest(PurchaseRequestDto requestDto, String requestedBy) {
        log.info("Creating new purchase request from user: {}", requestedBy);

        String requestNumber = generateRequestNumber();
        BigDecimal totalAmount = requestDto.getQuantity().multiply(requestDto.getEstimatedUnitPrice());

        PurchaseRequest purchaseRequest = PurchaseRequest.builder()
            .requestNumber(requestNumber)
            .title(requestDto.getTitle())
            .description(requestDto.getDescription())
            .quantity(requestDto.getQuantity())
            .unit(requestDto.getUnit())
            .estimatedUnitPrice(requestDto.getEstimatedUnitPrice())
            .totalAmount(totalAmount)
            .priority(requestDto.getPriority())
            .requestedBy(requestedBy)
            .requestDate(LocalDate.now())
            .requiredByDate(requestDto.getRequiredByDate())
            .departmentName(requestDto.getDepartmentName())
            .businessJustification(requestDto.getBusinessJustification())
            .preferredVendorId(requestDto.getPreferredVendorId())
            .status(RequestStatus.SUBMITTED)
            .isActive(true)
            .build();

        PurchaseRequest saved = purchaseRequestRepository.save(purchaseRequest);
        log.info("Purchase request created successfully with request number: {}", saved.getRequestNumber());

        return convertToResponseDto(saved);
    }

    /**
     * Get purchase request by ID
     */
    @Transactional(readOnly = true)
    public PurchaseRequestResponseDto getPurchaseRequestById(Long id) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase request not found with id: " + id));
        return convertToResponseDto(purchaseRequest);
    }

    /**
     * Get purchase request by request number
     */
    @Transactional(readOnly = true)
    public PurchaseRequestResponseDto getPurchaseRequestByNumber(String requestNumber) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findByRequestNumber(requestNumber)
            .orElseThrow(() -> new EntityNotFoundException("Purchase request not found with number: " + requestNumber));
        return convertToResponseDto(purchaseRequest);
    }

    /**
     * Get all purchase requests
     */
    @Transactional(readOnly = true)
    public List<PurchaseRequestResponseDto> getAllPurchaseRequests() {
        return purchaseRequestRepository.findAll().stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get purchase requests by status
     */
    @Transactional(readOnly = true)
    public List<PurchaseRequestResponseDto> getPurchaseRequestsByStatus(RequestStatus status) {
        return purchaseRequestRepository.findByStatus(status).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get purchase requests by priority
     */
    @Transactional(readOnly = true)
    public List<PurchaseRequestResponseDto> getPurchaseRequestsByPriority(Priority priority) {
        return purchaseRequestRepository.findByPriority(priority).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get purchase requests by requested user
     */
    @Transactional(readOnly = true)
    public List<PurchaseRequestResponseDto> getPurchaseRequestsByRequestedBy(String requestedBy) {
        return purchaseRequestRepository.findByRequestedBy(requestedBy).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get purchase requests by department
     */
    @Transactional(readOnly = true)
    public List<PurchaseRequestResponseDto> getPurchaseRequestsByDepartment(String departmentName) {
        return purchaseRequestRepository.findByDepartmentName(departmentName).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get purchase requests with pagination
     */
    @Transactional(readOnly = true)
    public Page<PurchaseRequestResponseDto> getPurchaseRequests(Pageable pageable) {
        return purchaseRequestRepository.findAll(pageable)
            .map(this::convertToResponseDto);
    }

    /**
     * Search purchase requests
     */
    @Transactional(readOnly = true)
    public Page<PurchaseRequestResponseDto> searchPurchaseRequests(String searchTerm, Pageable pageable) {
        return purchaseRequestRepository.searchByTitleOrDescription(searchTerm, pageable)
            .map(this::convertToResponseDto);
    }

    /**
     * Update purchase request
     */
    public PurchaseRequestResponseDto updatePurchaseRequest(Long id, PurchaseRequestDto requestDto) {
        log.info("Updating purchase request with id: {}", id);

        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase request not found with id: " + id));

        if (!purchaseRequest.getStatus().equals(RequestStatus.SUBMITTED)) {
            throw new IllegalArgumentException("Purchase request cannot be updated once it has been reviewed");
        }

        BigDecimal totalAmount = requestDto.getQuantity().multiply(requestDto.getEstimatedUnitPrice());

        purchaseRequest.setTitle(requestDto.getTitle());
        purchaseRequest.setDescription(requestDto.getDescription());
        purchaseRequest.setQuantity(requestDto.getQuantity());
        purchaseRequest.setUnit(requestDto.getUnit());
        purchaseRequest.setEstimatedUnitPrice(requestDto.getEstimatedUnitPrice());
        purchaseRequest.setTotalAmount(totalAmount);
        purchaseRequest.setPriority(requestDto.getPriority());
        purchaseRequest.setRequiredByDate(requestDto.getRequiredByDate());
        purchaseRequest.setDepartmentName(requestDto.getDepartmentName());
        purchaseRequest.setBusinessJustification(requestDto.getBusinessJustification());
        purchaseRequest.setPreferredVendorId(requestDto.getPreferredVendorId());

        PurchaseRequest updated = purchaseRequestRepository.save(purchaseRequest);
        log.info("Purchase request updated successfully with id: {}", id);

        return convertToResponseDto(updated);
    }

    /**
     * Approve purchase request
     */
    public PurchaseRequestResponseDto approvePurchaseRequest(Long id, String approvedBy) {
        log.info("Approving purchase request with id: {}", id);

        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase request not found with id: " + id));

        purchaseRequest.setStatus(RequestStatus.APPROVED);
        purchaseRequest.setApprovedBy(approvedBy);
        purchaseRequest.setApprovedAt(LocalDate.now());

        PurchaseRequest updated = purchaseRequestRepository.save(purchaseRequest);
        log.info("Purchase request approved with id: {}", id);

        return convertToResponseDto(updated);
    }

    /**
     * Reject purchase request
     */
    public PurchaseRequestResponseDto rejectPurchaseRequest(Long id, String rejectedBy, String rejectionReason) {
        log.info("Rejecting purchase request with id: {}", id);

        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase request not found with id: " + id));

        purchaseRequest.setStatus(RequestStatus.REJECTED);
        purchaseRequest.setRejectedBy(rejectedBy);
        purchaseRequest.setRejectedAt(LocalDate.now());
        purchaseRequest.setRejectionReason(rejectionReason);

        PurchaseRequest updated = purchaseRequestRepository.save(purchaseRequest);
        log.info("Purchase request rejected with id: {}", id);

        return convertToResponseDto(updated);
    }

    /**
     * Update purchase request status
     */
    public PurchaseRequestResponseDto updatePurchaseRequestStatus(Long id, RequestStatus newStatus) {
        log.info("Updating status of purchase request with id: {} to {}", id, newStatus);

        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase request not found with id: " + id));

        purchaseRequest.setStatus(newStatus);
        PurchaseRequest updated = purchaseRequestRepository.save(purchaseRequest);

        log.info("Status updated successfully for purchase request with id: {}", id);
        return convertToResponseDto(updated);
    }

    /**
     * Get pending approvals
     */
    @Transactional(readOnly = true)
    public List<PurchaseRequestResponseDto> getPendingApprovals() {
        return purchaseRequestRepository.findPendingApprovals().stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Convert entity to DTO
     */
    private PurchaseRequestResponseDto convertToResponseDto(PurchaseRequest purchaseRequest) {
        return PurchaseRequestResponseDto.builder()
            .id(purchaseRequest.getId())
            .requestNumber(purchaseRequest.getRequestNumber())
            .title(purchaseRequest.getTitle())
            .description(purchaseRequest.getDescription())
            .quantity(purchaseRequest.getQuantity())
            .unit(purchaseRequest.getUnit())
            .estimatedUnitPrice(purchaseRequest.getEstimatedUnitPrice())
            .totalAmount(purchaseRequest.getTotalAmount())
            .priority(purchaseRequest.getPriority())
            .status(purchaseRequest.getStatus())
            .requestedBy(purchaseRequest.getRequestedBy())
            .requestDate(purchaseRequest.getRequestDate())
            .requiredByDate(purchaseRequest.getRequiredByDate())
            .departmentName(purchaseRequest.getDepartmentName())
            .businessJustification(purchaseRequest.getBusinessJustification())
            .approvedBy(purchaseRequest.getApprovedBy())
            .approvedAt(purchaseRequest.getApprovedAt())
            .rejectionReason(purchaseRequest.getRejectionReason())
            .rejectedBy(purchaseRequest.getRejectedBy())
            .rejectedAt(purchaseRequest.getRejectedAt())
            .rfqSentAt(purchaseRequest.getRfqSentAt())
            .preferredVendorId(purchaseRequest.getPreferredVendorId())
            .selectedVendorId(purchaseRequest.getSelectedVendorId())
            .purchaseOrderId(purchaseRequest.getPurchaseOrderId())
            .workflowInstanceId(purchaseRequest.getWorkflowInstanceId())
            .isActive(purchaseRequest.getIsActive())
            .createdAt(purchaseRequest.getCreatedAt())
            .updatedAt(purchaseRequest.getUpdatedAt())
            .createdBy(purchaseRequest.getCreatedBy())
            .updatedBy(purchaseRequest.getUpdatedBy())
            .build();
    }

    /**
     * Generate unique request number
     */
    private String generateRequestNumber() {
        return "PR-" + System.currentTimeMillis();
    }
}
