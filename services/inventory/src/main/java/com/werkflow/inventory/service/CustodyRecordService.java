package com.werkflow.inventory.service;

import com.werkflow.inventory.dto.CustodyRecordRequest;
import com.werkflow.inventory.dto.CustodyRecordResponse;
import com.werkflow.inventory.entity.AssetInstance;
import com.werkflow.inventory.entity.CustodyRecord;
import com.werkflow.inventory.repository.AssetInstanceRepository;
import com.werkflow.inventory.repository.CustodyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustodyRecordService {

    private final CustodyRecordRepository custodyRecordRepository;
    private final AssetInstanceRepository assetInstanceRepository;

    @Transactional
    public CustodyRecordResponse createCustodyRecord(CustodyRecordRequest request) {
        log.info("Creating custody record for asset ID: {}", request.getAssetInstanceId());

        AssetInstance assetInstance = assetInstanceRepository.findById(request.getAssetInstanceId())
            .orElseThrow(() -> new RuntimeException("Asset instance not found with ID: " + request.getAssetInstanceId()));

        Optional<CustodyRecord> existingCustody = custodyRecordRepository
            .findCurrentCustodyByAssetId(request.getAssetInstanceId());

        if (existingCustody.isPresent() && request.getEndDate() == null) {
            throw new RuntimeException("Asset already has an active custody record. Close existing custody before creating new one.");
        }

        CustodyRecord custodyRecord = CustodyRecord.builder()
            .assetInstance(assetInstance)
            .custodianDeptId(request.getCustodianDeptId())
            .custodianUserId(request.getCustodianUserId())
            .physicalLocation(request.getPhysicalLocation())
            .custodyType(request.getCustodyType())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .assignedByUserId(request.getAssignedByUserId())
            .returnCondition(request.getReturnCondition() != null ? AssetInstance.AssetCondition.valueOf(request.getReturnCondition()) : null)
            .notes(request.getNotes())
            .build();

        custodyRecord = custodyRecordRepository.save(custodyRecord);
        log.info("Custody record created successfully with ID: {}", custodyRecord.getId());

        return mapToResponse(custodyRecord);
    }

    @Transactional(readOnly = true)
    public CustodyRecordResponse getCustodyRecordById(Long id) {
        log.debug("Fetching custody record with ID: {}", id);
        CustodyRecord custodyRecord = custodyRecordRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Custody record not found with ID: " + id));
        return mapToResponse(custodyRecord);
    }

    @Transactional(readOnly = true)
    public Optional<CustodyRecordResponse> getCurrentCustodyForAsset(Long assetInstanceId) {
        log.debug("Fetching current custody for asset ID: {}", assetInstanceId);
        return custodyRecordRepository.findCurrentCustodyByAssetId(assetInstanceId)
            .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<CustodyRecordResponse> getCustodyHistoryForAsset(Long assetInstanceId) {
        log.debug("Fetching custody history for asset ID: {}", assetInstanceId);
        return custodyRecordRepository.findByAssetInstanceId(assetInstanceId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustodyRecordResponse> getCurrentCustodyByDepartment(Long deptId) {
        log.debug("Fetching current custody records for department ID: {}", deptId);
        return custodyRecordRepository.findCurrentCustodyByDepartment(deptId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustodyRecordResponse> getCurrentCustodyByUser(Long userId) {
        log.debug("Fetching current custody records for user ID: {}", userId);
        return custodyRecordRepository.findCurrentCustodyByUser(userId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public CustodyRecordResponse transferCustody(Long assetInstanceId, Long toDeptId, Long toUserId,
                                                  Long assignedByUserId, String notes) {
        log.info("Transferring custody for asset ID: {} to department: {}", assetInstanceId, toDeptId);

        AssetInstance assetInstance = assetInstanceRepository.findById(assetInstanceId)
            .orElseThrow(() -> new RuntimeException("Asset instance not found with ID: " + assetInstanceId));

        Optional<CustodyRecord> currentCustody = custodyRecordRepository.findCurrentCustodyByAssetId(assetInstanceId);

        if (currentCustody.isPresent()) {
            CustodyRecord existing = currentCustody.get();
            existing.setEndDate(LocalDateTime.now());
            existing.setReturnCondition(assetInstance.getCondition());
            custodyRecordRepository.save(existing);
            log.info("Closed existing custody record ID: {}", existing.getId());
        }

        CustodyRecord newCustody = CustodyRecord.builder()
            .assetInstance(assetInstance)
            .custodianDeptId(toDeptId)
            .custodianUserId(toUserId)
            .physicalLocation(null)
            .custodyType(CustodyRecord.CustodyType.PERMANENT)
            .startDate(LocalDateTime.now())
            .endDate(null)
            .assignedByUserId(assignedByUserId)
            .notes(notes)
            .build();

        newCustody = custodyRecordRepository.save(newCustody);
        log.info("Created new custody record ID: {}", newCustody.getId());

        assetInstance.setStatus(AssetInstance.AssetStatus.IN_USE);
        assetInstanceRepository.save(assetInstance);

        return mapToResponse(newCustody);
    }

    @Transactional
    public void closeCustody(Long custodyRecordId, AssetInstance.AssetCondition returnCondition, String notes) {
        log.info("Closing custody record ID: {}", custodyRecordId);

        CustodyRecord custodyRecord = custodyRecordRepository.findById(custodyRecordId)
            .orElseThrow(() -> new RuntimeException("Custody record not found with ID: " + custodyRecordId));

        if (custodyRecord.getEndDate() != null) {
            throw new RuntimeException("Custody record is already closed");
        }

        custodyRecord.setEndDate(LocalDateTime.now());
        custodyRecord.setReturnCondition(returnCondition);
        if (notes != null) {
            custodyRecord.setNotes(custodyRecord.getNotes() != null ?
                custodyRecord.getNotes() + "; " + notes : notes);
        }

        custodyRecordRepository.save(custodyRecord);

        AssetInstance assetInstance = custodyRecord.getAssetInstance();
        assetInstance.setStatus(AssetInstance.AssetStatus.AVAILABLE);
        assetInstance.setCondition(returnCondition);
        assetInstanceRepository.save(assetInstance);

        log.info("Custody record closed successfully: {}", custodyRecordId);
    }

    private CustodyRecordResponse mapToResponse(CustodyRecord custodyRecord) {
        return CustodyRecordResponse.builder()
            .id(custodyRecord.getId())
            .assetInstanceId(custodyRecord.getAssetInstance().getId())
            .assetTag(custodyRecord.getAssetInstance().getAssetTag())
            .custodianDeptId(custodyRecord.getCustodianDeptId())
            .custodianUserId(custodyRecord.getCustodianUserId())
            .physicalLocation(custodyRecord.getPhysicalLocation())
            .custodyType(custodyRecord.getCustodyType())
            .startDate(custodyRecord.getStartDate())
            .endDate(custodyRecord.getEndDate())
            .assignedByUserId(custodyRecord.getAssignedByUserId())
            .returnCondition(custodyRecord.getReturnCondition() != null ? custodyRecord.getReturnCondition().name() : null)
            .notes(custodyRecord.getNotes())
            .createdAt(custodyRecord.getCreatedAt())
            .build();
    }
}
