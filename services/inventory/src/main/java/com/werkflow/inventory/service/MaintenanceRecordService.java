package com.werkflow.inventory.service;

import com.werkflow.inventory.dto.MaintenanceRecordRequest;
import com.werkflow.inventory.dto.MaintenanceRecordResponse;
import com.werkflow.inventory.entity.AssetInstance;
import com.werkflow.inventory.entity.MaintenanceRecord;
import com.werkflow.inventory.repository.AssetInstanceRepository;
import com.werkflow.inventory.repository.MaintenanceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceRecordService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final AssetInstanceRepository assetInstanceRepository;

    @Transactional
    public MaintenanceRecordResponse createMaintenanceRecord(MaintenanceRecordRequest request) {
        log.info("Creating maintenance record for asset ID: {}", request.getAssetInstanceId());

        AssetInstance assetInstance = assetInstanceRepository.findById(request.getAssetInstanceId())
            .orElseThrow(() -> new RuntimeException("Asset instance not found with ID: " + request.getAssetInstanceId()));

        MaintenanceRecord maintenanceRecord = MaintenanceRecord.builder()
            .assetInstance(assetInstance)
            .maintenanceType(request.getMaintenanceType())
            .scheduledDate(request.getScheduledDate())
            .completedDate(request.getCompletedDate())
            .performedBy(request.getPerformedBy())
            .cost(request.getCost())
            .description(request.getDescription())
            .nextMaintenanceDate(request.getNextMaintenanceDate())
            .build();

        maintenanceRecord = maintenanceRecordRepository.save(maintenanceRecord);
        log.info("Maintenance record created successfully with ID: {}", maintenanceRecord.getId());

        if (request.getCompletedDate() == null && request.getScheduledDate() != null) {
            assetInstance.setStatus(AssetInstance.AssetStatus.MAINTENANCE);
            assetInstanceRepository.save(assetInstance);
        }

        return mapToResponse(maintenanceRecord);
    }

    @Transactional(readOnly = true)
    public MaintenanceRecordResponse getMaintenanceRecordById(Long id) {
        log.debug("Fetching maintenance record with ID: {}", id);
        MaintenanceRecord maintenanceRecord = maintenanceRecordRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Maintenance record not found with ID: " + id));
        return mapToResponse(maintenanceRecord);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecordResponse> getMaintenanceRecordsByAsset(Long assetInstanceId) {
        log.debug("Fetching maintenance records for asset ID: {}", assetInstanceId);
        return maintenanceRecordRepository.findByAssetInstanceId(assetInstanceId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecordResponse> getScheduledMaintenance(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching scheduled maintenance between {} and {}", startDate, endDate);
        return maintenanceRecordRepository.findScheduledBetween(startDate, endDate).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecordResponse> getUpcomingMaintenance(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching upcoming maintenance between {} and {}", startDate, endDate);
        return maintenanceRecordRepository.findUpcomingMaintenance(startDate, endDate).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecordResponse> getOverdueMaintenance() {
        log.debug("Fetching overdue maintenance");
        return maintenanceRecordRepository.findOverdueMaintenanceBefore(LocalDate.now()).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public MaintenanceRecordResponse completeMaintenanceRecord(Long id, LocalDate completedDate, String notes) {
        log.info("Completing maintenance record ID: {}", id);

        MaintenanceRecord maintenanceRecord = maintenanceRecordRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Maintenance record not found with ID: " + id));

        if (maintenanceRecord.getCompletedDate() != null) {
            throw new RuntimeException("Maintenance record is already completed");
        }

        maintenanceRecord.setCompletedDate(completedDate);
        if (notes != null) {
            String existingDesc = maintenanceRecord.getDescription();
            maintenanceRecord.setDescription(existingDesc != null ? existingDesc + "; " + notes : notes);
        }

        if (maintenanceRecord.getAssetInstance().getAssetDefinition().getMaintenanceIntervalMonths() != null) {
            maintenanceRecord.setNextMaintenanceDate(
                completedDate.plusMonths(maintenanceRecord.getAssetInstance().getAssetDefinition().getMaintenanceIntervalMonths())
            );
        }

        maintenanceRecord = maintenanceRecordRepository.save(maintenanceRecord);

        AssetInstance assetInstance = maintenanceRecord.getAssetInstance();
        if (assetInstance.getStatus() == AssetInstance.AssetStatus.MAINTENANCE) {
            assetInstance.setStatus(AssetInstance.AssetStatus.AVAILABLE);
            assetInstanceRepository.save(assetInstance);
        }

        log.info("Maintenance record completed successfully: {}", id);
        return mapToResponse(maintenanceRecord);
    }

    @Transactional
    public MaintenanceRecordResponse updateMaintenanceRecord(Long id, MaintenanceRecordRequest request) {
        log.info("Updating maintenance record with ID: {}", id);

        MaintenanceRecord maintenanceRecord = maintenanceRecordRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Maintenance record not found with ID: " + id));

        maintenanceRecord.setMaintenanceType(request.getMaintenanceType());
        maintenanceRecord.setScheduledDate(request.getScheduledDate());
        maintenanceRecord.setCompletedDate(request.getCompletedDate());
        maintenanceRecord.setPerformedBy(request.getPerformedBy());
        maintenanceRecord.setCost(request.getCost());
        maintenanceRecord.setDescription(request.getDescription());
        maintenanceRecord.setNextMaintenanceDate(request.getNextMaintenanceDate());

        maintenanceRecord = maintenanceRecordRepository.save(maintenanceRecord);
        log.info("Maintenance record updated successfully: {}", id);

        return mapToResponse(maintenanceRecord);
    }

    @Transactional
    public void deleteMaintenanceRecord(Long id) {
        log.info("Deleting maintenance record with ID: {}", id);

        if (!maintenanceRecordRepository.existsById(id)) {
            throw new RuntimeException("Maintenance record not found with ID: " + id);
        }

        maintenanceRecordRepository.deleteById(id);
        log.info("Maintenance record deleted successfully: {}", id);
    }

    private MaintenanceRecordResponse mapToResponse(MaintenanceRecord maintenanceRecord) {
        return MaintenanceRecordResponse.builder()
            .id(maintenanceRecord.getId())
            .assetInstanceId(maintenanceRecord.getAssetInstance().getId())
            .assetTag(maintenanceRecord.getAssetInstance().getAssetTag())
            .maintenanceType(maintenanceRecord.getMaintenanceType())
            .scheduledDate(maintenanceRecord.getScheduledDate())
            .completedDate(maintenanceRecord.getCompletedDate())
            .performedBy(maintenanceRecord.getPerformedBy())
            .cost(maintenanceRecord.getCost())
            .description(maintenanceRecord.getDescription())
            .nextMaintenanceDate(maintenanceRecord.getNextMaintenanceDate())
            .createdAt(maintenanceRecord.getCreatedAt())
            .build();
    }
}
