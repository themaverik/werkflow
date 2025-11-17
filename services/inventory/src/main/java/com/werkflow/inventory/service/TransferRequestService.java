package com.werkflow.inventory.service;

import com.werkflow.inventory.dto.TransferRequestRequest;
import com.werkflow.inventory.dto.TransferRequestResponse;
import com.werkflow.inventory.entity.AssetInstance;
import com.werkflow.inventory.entity.TransferRequest;
import com.werkflow.inventory.repository.AssetInstanceRepository;
import com.werkflow.inventory.repository.TransferRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferRequestService {

    private final TransferRequestRepository transferRequestRepository;
    private final AssetInstanceRepository assetInstanceRepository;
    private final CustodyRecordService custodyRecordService;

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("5000.00");

    @Transactional
    public TransferRequestResponse createTransferRequest(TransferRequestRequest request) {
        log.info("Creating transfer request for asset ID: {}", request.getAssetInstanceId());

        AssetInstance assetInstance = assetInstanceRepository.findById(request.getAssetInstanceId())
            .orElseThrow(() -> new RuntimeException("Asset instance not found with ID: " + request.getAssetInstanceId()));

        if (assetInstance.getStatus() == AssetInstance.AssetStatus.DISPOSED ||
            assetInstance.getStatus() == AssetInstance.AssetStatus.LOST) {
            throw new RuntimeException("Cannot transfer asset in status: " + assetInstance.getStatus());
        }

        TransferRequest transferRequest = TransferRequest.builder()
            .assetInstance(assetInstance)
            .fromDeptId(request.getFromDeptId())
            .fromUserId(request.getFromUserId())
            .toDeptId(request.getToDeptId())
            .toUserId(request.getToUserId())
            .transferType(request.getTransferType())
            .transferReason(request.getTransferReason())
            .expectedReturnDate(request.getExpectedReturnDate())
            .initiatedByUserId(request.getInitiatedByUserId())
            .initiatedDate(LocalDateTime.now())
            .status(TransferRequest.TransferStatus.PENDING)
            .build();

        transferRequest = transferRequestRepository.save(transferRequest);
        log.info("Transfer request created successfully with ID: {}", transferRequest.getId());

        return mapToResponse(transferRequest);
    }

    @Transactional(readOnly = true)
    public TransferRequestResponse getTransferRequestById(Long id) {
        log.debug("Fetching transfer request with ID: {}", id);
        TransferRequest transferRequest = transferRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Transfer request not found with ID: " + id));
        return mapToResponse(transferRequest);
    }

    @Transactional(readOnly = true)
    public List<TransferRequestResponse> getTransferRequestsByAsset(Long assetInstanceId) {
        log.debug("Fetching transfer requests for asset ID: {}", assetInstanceId);
        return transferRequestRepository.findByAssetInstanceId(assetInstanceId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransferRequestResponse> getTransferRequestsByDepartment(Long deptId) {
        log.debug("Fetching transfer requests for department ID: {}", deptId);
        return transferRequestRepository.findAllByDepartment(deptId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransferRequestResponse> getPendingTransferRequests() {
        log.debug("Fetching pending transfer requests");
        return transferRequestRepository.findByStatus(TransferRequest.TransferStatus.PENDING).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransferRequestResponse> getHighValuePendingTransfers() {
        log.debug("Fetching high-value pending transfer requests");
        return transferRequestRepository.findPendingHighValueTransfers(HIGH_VALUE_THRESHOLD).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public TransferRequestResponse approveTransferRequest(Long id, Long approvedByUserId) {
        log.info("Approving transfer request ID: {}", id);

        TransferRequest transferRequest = transferRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Transfer request not found with ID: " + id));

        if (transferRequest.getStatus() != TransferRequest.TransferStatus.PENDING) {
            throw new RuntimeException("Cannot approve transfer request with status: " + transferRequest.getStatus());
        }

        transferRequest.setStatus(TransferRequest.TransferStatus.APPROVED);
        transferRequest.setApprovedByUserId(approvedByUserId);
        transferRequest.setApprovedDate(LocalDateTime.now());

        transferRequest = transferRequestRepository.save(transferRequest);
        log.info("Transfer request approved successfully: {}", id);

        return mapToResponse(transferRequest);
    }

    @Transactional
    public TransferRequestResponse rejectTransferRequest(Long id, String rejectionReason) {
        log.info("Rejecting transfer request ID: {}", id);

        TransferRequest transferRequest = transferRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Transfer request not found with ID: " + id));

        if (transferRequest.getStatus() != TransferRequest.TransferStatus.PENDING) {
            throw new RuntimeException("Cannot reject transfer request with status: " + transferRequest.getStatus());
        }

        transferRequest.setStatus(TransferRequest.TransferStatus.REJECTED);
        transferRequest.setRejectionReason(rejectionReason);

        transferRequest = transferRequestRepository.save(transferRequest);
        log.info("Transfer request rejected successfully: {}", id);

        return mapToResponse(transferRequest);
    }

    @Transactional
    public TransferRequestResponse completeTransferRequest(Long id, Long completedByUserId) {
        log.info("Completing transfer request ID: {}", id);

        TransferRequest transferRequest = transferRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Transfer request not found with ID: " + id));

        if (transferRequest.getStatus() != TransferRequest.TransferStatus.APPROVED) {
            throw new RuntimeException("Cannot complete transfer request with status: " + transferRequest.getStatus());
        }

        custodyRecordService.transferCustody(
            transferRequest.getAssetInstance().getId(),
            transferRequest.getToDeptId(),
            transferRequest.getToUserId(),
            completedByUserId,
            "Transfer completed via request ID: " + id
        );

        transferRequest.setStatus(TransferRequest.TransferStatus.COMPLETED);
        transferRequest.setCompletedDate(LocalDateTime.now());

        transferRequest = transferRequestRepository.save(transferRequest);
        log.info("Transfer request completed successfully: {}", id);

        return mapToResponse(transferRequest);
    }

    @Transactional
    public void cancelTransferRequest(Long id) {
        log.info("Cancelling transfer request ID: {}", id);

        TransferRequest transferRequest = transferRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Transfer request not found with ID: " + id));

        if (transferRequest.getStatus() == TransferRequest.TransferStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel completed transfer request");
        }

        transferRequest.setStatus(TransferRequest.TransferStatus.CANCELLED);
        transferRequestRepository.save(transferRequest);
        log.info("Transfer request cancelled successfully: {}", id);
    }

    private TransferRequestResponse mapToResponse(TransferRequest transferRequest) {
        return TransferRequestResponse.builder()
            .id(transferRequest.getId())
            .assetInstanceId(transferRequest.getAssetInstance().getId())
            .assetTag(transferRequest.getAssetInstance().getAssetTag())
            .assetName(transferRequest.getAssetInstance().getAssetDefinition().getName())
            .fromDeptId(transferRequest.getFromDeptId())
            .fromUserId(transferRequest.getFromUserId())
            .toDeptId(transferRequest.getToDeptId())
            .toUserId(transferRequest.getToUserId())
            .transferType(transferRequest.getTransferType())
            .transferReason(transferRequest.getTransferReason())
            .expectedReturnDate(transferRequest.getExpectedReturnDate())
            .initiatedByUserId(transferRequest.getInitiatedByUserId())
            .initiatedDate(transferRequest.getInitiatedDate())
            .approvedByUserId(transferRequest.getApprovedByUserId())
            .approvedDate(transferRequest.getApprovedDate())
            .completedDate(transferRequest.getCompletedDate())
            .status(transferRequest.getStatus())
            .processInstanceId(transferRequest.getProcessInstanceId())
            .rejectionReason(transferRequest.getRejectionReason())
            .createdAt(transferRequest.getCreatedAt())
            .updatedAt(transferRequest.getUpdatedAt())
            .build();
    }
}
