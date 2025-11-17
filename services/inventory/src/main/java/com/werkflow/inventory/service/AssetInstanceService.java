package com.werkflow.inventory.service;

import com.werkflow.inventory.dto.AssetInstanceRequest;
import com.werkflow.inventory.dto.AssetInstanceResponse;
import com.werkflow.inventory.dto.CustodyRecordResponse;
import com.werkflow.inventory.entity.AssetDefinition;
import com.werkflow.inventory.entity.AssetInstance;
import com.werkflow.inventory.entity.CustodyRecord;
import com.werkflow.inventory.repository.AssetDefinitionRepository;
import com.werkflow.inventory.repository.AssetInstanceRepository;
import com.werkflow.inventory.repository.CustodyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetInstanceService {

    private final AssetInstanceRepository instanceRepository;
    private final AssetDefinitionRepository definitionRepository;
    private final CustodyRecordRepository custodyRecordRepository;

    @Transactional
    public AssetInstanceResponse createInstance(AssetInstanceRequest request) {
        log.info("Creating asset instance with tag: {}", request.getAssetTag());

        if (instanceRepository.existsByAssetTag(request.getAssetTag())) {
            throw new RuntimeException("Asset instance with tag '" + request.getAssetTag() + "' already exists");
        }

        AssetDefinition definition = definitionRepository.findById(request.getAssetDefinitionId())
            .orElseThrow(() -> new RuntimeException("Asset definition not found with ID: " + request.getAssetDefinitionId()));

        AssetInstance instance = AssetInstance.builder()
            .assetDefinition(definition)
            .assetTag(request.getAssetTag())
            .serialNumber(request.getSerialNumber())
            .purchaseDate(request.getPurchaseDate())
            .purchaseCost(request.getPurchaseCost())
            .warrantyExpiryDate(request.getWarrantyExpiryDate())
            .condition(request.getCondition() != null ? request.getCondition() : AssetInstance.AssetCondition.NEW)
            .status(request.getStatus() != null ? request.getStatus() : AssetInstance.AssetStatus.AVAILABLE)
            .currentLocation(request.getCurrentLocation())
            .notes(request.getNotes())
            .metadata(request.getMetadata())
            .build();

        instance = instanceRepository.save(instance);
        log.info("Asset instance created successfully with ID: {}", instance.getId());

        return mapToResponse(instance);
    }

    @Transactional(readOnly = true)
    public AssetInstanceResponse getInstanceById(Long id) {
        log.debug("Fetching asset instance with ID: {}", id);
        AssetInstance instance = instanceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Asset instance not found with ID: " + id));
        return mapToResponse(instance);
    }

    @Transactional(readOnly = true)
    public AssetInstanceResponse getInstanceByTag(String assetTag) {
        log.debug("Fetching asset instance with tag: {}", assetTag);
        AssetInstance instance = instanceRepository.findByAssetTag(assetTag)
            .orElseThrow(() -> new RuntimeException("Asset instance not found with tag: " + assetTag));
        return mapToResponse(instance);
    }

    @Transactional(readOnly = true)
    public List<AssetInstanceResponse> getAllInstances() {
        log.debug("Fetching all asset instances");
        return instanceRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetInstanceResponse> getInstancesByDefinition(Long definitionId) {
        log.debug("Fetching asset instances for definition ID: {}", definitionId);
        return instanceRepository.findByAssetDefinitionId(definitionId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetInstanceResponse> getInstancesByStatus(AssetInstance.AssetStatus status) {
        log.debug("Fetching asset instances with status: {}", status);
        return instanceRepository.findByStatus(status).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetInstanceResponse> searchInstances(String searchTerm) {
        log.debug("Searching asset instances with term: {}", searchTerm);
        return instanceRepository.searchByTagOrSerialOrName(searchTerm).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public AssetInstanceResponse updateInstance(Long id, AssetInstanceRequest request) {
        log.info("Updating asset instance with ID: {}", id);

        AssetInstance instance = instanceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Asset instance not found with ID: " + id));

        if (!request.getAssetTag().equals(instance.getAssetTag()) &&
            instanceRepository.existsByAssetTag(request.getAssetTag())) {
            throw new RuntimeException("Asset instance with tag '" + request.getAssetTag() + "' already exists");
        }

        AssetDefinition definition = definitionRepository.findById(request.getAssetDefinitionId())
            .orElseThrow(() -> new RuntimeException("Asset definition not found with ID: " + request.getAssetDefinitionId()));

        instance.setAssetDefinition(definition);
        instance.setAssetTag(request.getAssetTag());
        instance.setSerialNumber(request.getSerialNumber());
        instance.setPurchaseDate(request.getPurchaseDate());
        instance.setPurchaseCost(request.getPurchaseCost());
        instance.setWarrantyExpiryDate(request.getWarrantyExpiryDate());
        if (request.getCondition() != null) {
            instance.setCondition(request.getCondition());
        }
        if (request.getStatus() != null) {
            instance.setStatus(request.getStatus());
        }
        instance.setCurrentLocation(request.getCurrentLocation());
        instance.setNotes(request.getNotes());
        instance.setMetadata(request.getMetadata());

        instance = instanceRepository.save(instance);
        log.info("Asset instance updated successfully: {}", id);

        return mapToResponse(instance);
    }

    @Transactional
    public void deleteInstance(Long id) {
        log.info("Deleting asset instance with ID: {}", id);

        if (!instanceRepository.existsById(id)) {
            throw new RuntimeException("Asset instance not found with ID: " + id);
        }

        instanceRepository.deleteById(id);
        log.info("Asset instance deleted successfully: {}", id);
    }

    private AssetInstanceResponse mapToResponse(AssetInstance instance) {
        AssetInstanceResponse.AssetInstanceResponseBuilder builder = AssetInstanceResponse.builder()
            .id(instance.getId())
            .assetDefinitionId(instance.getAssetDefinition().getId())
            .assetDefinitionName(instance.getAssetDefinition().getName())
            .assetTag(instance.getAssetTag())
            .serialNumber(instance.getSerialNumber())
            .purchaseDate(instance.getPurchaseDate())
            .purchaseCost(instance.getPurchaseCost())
            .warrantyExpiryDate(instance.getWarrantyExpiryDate())
            .condition(instance.getCondition())
            .status(instance.getStatus())
            .currentLocation(instance.getCurrentLocation())
            .notes(instance.getNotes())
            .metadata(instance.getMetadata())
            .createdAt(instance.getCreatedAt())
            .updatedAt(instance.getUpdatedAt());

        Optional<CustodyRecord> currentCustody = custodyRecordRepository.findCurrentCustodyByAssetId(instance.getId());
        if (currentCustody.isPresent()) {
            CustodyRecord custody = currentCustody.get();
            builder.currentCustody(CustodyRecordResponse.builder()
                .id(custody.getId())
                .assetInstanceId(instance.getId())
                .assetTag(instance.getAssetTag())
                .custodianDeptId(custody.getCustodianDeptId())
                .custodianUserId(custody.getCustodianUserId())
                .physicalLocation(custody.getPhysicalLocation())
                .custodyType(custody.getCustodyType())
                .startDate(custody.getStartDate())
                .endDate(custody.getEndDate())
                .assignedByUserId(custody.getAssignedByUserId())
                .returnCondition(custody.getReturnCondition() != null ? custody.getReturnCondition().name() : null)
                .notes(custody.getNotes())
                .createdAt(custody.getCreatedAt())
                .build());
        }

        return builder.build();
    }
}
