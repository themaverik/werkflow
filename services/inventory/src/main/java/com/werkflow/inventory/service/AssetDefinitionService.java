package com.werkflow.inventory.service;

import com.werkflow.inventory.dto.AssetDefinitionRequest;
import com.werkflow.inventory.dto.AssetDefinitionResponse;
import com.werkflow.inventory.entity.AssetCategory;
import com.werkflow.inventory.entity.AssetDefinition;
import com.werkflow.inventory.repository.AssetCategoryRepository;
import com.werkflow.inventory.repository.AssetDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetDefinitionService {

    private final AssetDefinitionRepository definitionRepository;
    private final AssetCategoryRepository categoryRepository;

    @Transactional
    public AssetDefinitionResponse createDefinition(AssetDefinitionRequest request) {
        log.info("Creating asset definition: {}", request.getName());

        if (request.getSku() != null && definitionRepository.existsBySku(request.getSku())) {
            throw new RuntimeException("Asset definition with SKU '" + request.getSku() + "' already exists");
        }

        AssetCategory category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new RuntimeException("Asset category not found with ID: " + request.getCategoryId()));

        AssetDefinition definition = AssetDefinition.builder()
            .category(category)
            .sku(request.getSku())
            .name(request.getName())
            .manufacturer(request.getManufacturer())
            .model(request.getModel())
            .specifications(request.getSpecifications())
            .unitCost(request.getUnitCost())
            .expectedLifespanMonths(request.getExpectedLifespanMonths())
            .requiresMaintenance(request.getRequiresMaintenance() != null ? request.getRequiresMaintenance() : false)
            .maintenanceIntervalMonths(request.getMaintenanceIntervalMonths())
            .active(request.getActive() != null ? request.getActive() : true)
            .build();

        definition = definitionRepository.save(definition);
        log.info("Asset definition created successfully with ID: {}", definition.getId());

        return mapToResponse(definition);
    }

    @Transactional(readOnly = true)
    public AssetDefinitionResponse getDefinitionById(Long id) {
        log.debug("Fetching asset definition with ID: {}", id);
        AssetDefinition definition = definitionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Asset definition not found with ID: " + id));
        return mapToResponse(definition);
    }

    @Transactional(readOnly = true)
    public List<AssetDefinitionResponse> getAllDefinitions() {
        log.debug("Fetching all asset definitions");
        return definitionRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetDefinitionResponse> getDefinitionsByCategory(Long categoryId) {
        log.debug("Fetching asset definitions for category ID: {}", categoryId);
        return definitionRepository.findByCategoryId(categoryId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetDefinitionResponse> searchDefinitions(String searchTerm) {
        log.debug("Searching asset definitions with term: {}", searchTerm);
        return definitionRepository.searchByNameOrManufacturerOrModel(searchTerm).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public AssetDefinitionResponse updateDefinition(Long id, AssetDefinitionRequest request) {
        log.info("Updating asset definition with ID: {}", id);

        AssetDefinition definition = definitionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Asset definition not found with ID: " + id));

        if (request.getSku() != null &&
            !request.getSku().equals(definition.getSku()) &&
            definitionRepository.existsBySku(request.getSku())) {
            throw new RuntimeException("Asset definition with SKU '" + request.getSku() + "' already exists");
        }

        AssetCategory category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new RuntimeException("Asset category not found with ID: " + request.getCategoryId()));

        definition.setCategory(category);
        definition.setSku(request.getSku());
        definition.setName(request.getName());
        definition.setManufacturer(request.getManufacturer());
        definition.setModel(request.getModel());
        definition.setSpecifications(request.getSpecifications());
        definition.setUnitCost(request.getUnitCost());
        definition.setExpectedLifespanMonths(request.getExpectedLifespanMonths());
        if (request.getRequiresMaintenance() != null) {
            definition.setRequiresMaintenance(request.getRequiresMaintenance());
        }
        definition.setMaintenanceIntervalMonths(request.getMaintenanceIntervalMonths());
        if (request.getActive() != null) {
            definition.setActive(request.getActive());
        }

        definition = definitionRepository.save(definition);
        log.info("Asset definition updated successfully: {}", id);

        return mapToResponse(definition);
    }

    @Transactional
    public void deleteDefinition(Long id) {
        log.info("Deleting asset definition with ID: {}", id);

        if (!definitionRepository.existsById(id)) {
            throw new RuntimeException("Asset definition not found with ID: " + id);
        }

        definitionRepository.deleteById(id);
        log.info("Asset definition deleted successfully: {}", id);
    }

    private AssetDefinitionResponse mapToResponse(AssetDefinition definition) {
        return AssetDefinitionResponse.builder()
            .id(definition.getId())
            .categoryId(definition.getCategory().getId())
            .categoryName(definition.getCategory().getName())
            .sku(definition.getSku())
            .name(definition.getName())
            .manufacturer(definition.getManufacturer())
            .model(definition.getModel())
            .specifications(definition.getSpecifications())
            .unitCost(definition.getUnitCost())
            .expectedLifespanMonths(definition.getExpectedLifespanMonths())
            .requiresMaintenance(definition.getRequiresMaintenance())
            .maintenanceIntervalMonths(definition.getMaintenanceIntervalMonths())
            .active(definition.getActive())
            .createdAt(definition.getCreatedAt())
            .updatedAt(definition.getUpdatedAt())
            .build();
    }
}
