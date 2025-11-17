package com.werkflow.inventory.service;

import com.werkflow.inventory.dto.AssetCategoryRequest;
import com.werkflow.inventory.dto.AssetCategoryResponse;
import com.werkflow.inventory.entity.AssetCategory;
import com.werkflow.inventory.repository.AssetCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetCategoryService {

    private final AssetCategoryRepository categoryRepository;

    @Transactional
    public AssetCategoryResponse createCategory(AssetCategoryRequest request) {
        log.info("Creating asset category: {}", request.getName());

        if (request.getCode() != null && categoryRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Category with code '" + request.getCode() + "' already exists");
        }

        AssetCategory.AssetCategoryBuilder builder = AssetCategory.builder()
            .name(request.getName())
            .code(request.getCode())
            .description(request.getDescription())
            .primaryCustodianDeptId(request.getPrimaryCustodianDeptId())
            .requiresApproval(request.getRequiresApproval() != null ? request.getRequiresApproval() : true)
            .active(request.getActive() != null ? request.getActive() : true);

        if (request.getParentCategoryId() != null) {
            AssetCategory parent = categoryRepository.findById(request.getParentCategoryId())
                .orElseThrow(() -> new RuntimeException("Parent category not found with ID: " + request.getParentCategoryId()));
            builder.parentCategory(parent);
        }

        AssetCategory category = builder.build();
        category = categoryRepository.save(category);
        log.info("Asset category created successfully with ID: {}", category.getId());

        return mapToResponse(category);
    }

    @Transactional(readOnly = true)
    public AssetCategoryResponse getCategoryById(Long id) {
        log.debug("Fetching asset category with ID: {}", id);
        AssetCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Asset category not found with ID: " + id));
        return mapToResponse(category);
    }

    @Transactional(readOnly = true)
    public List<AssetCategoryResponse> getAllCategories() {
        log.debug("Fetching all asset categories");
        return categoryRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetCategoryResponse> getTopLevelCategories() {
        log.debug("Fetching top-level asset categories");
        return categoryRepository.findByParentCategoryIsNull().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetCategoryResponse> getCategoriesByDepartment(Long deptId) {
        log.debug("Fetching asset categories for department ID: {}", deptId);
        return categoryRepository.findByPrimaryCustodianDeptId(deptId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public AssetCategoryResponse updateCategory(Long id, AssetCategoryRequest request) {
        log.info("Updating asset category with ID: {}", id);

        AssetCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Asset category not found with ID: " + id));

        if (request.getCode() != null &&
            !request.getCode().equals(category.getCode()) &&
            categoryRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Category with code '" + request.getCode() + "' already exists");
        }

        category.setName(request.getName());
        category.setCode(request.getCode());
        category.setDescription(request.getDescription());
        category.setPrimaryCustodianDeptId(request.getPrimaryCustodianDeptId());
        if (request.getRequiresApproval() != null) {
            category.setRequiresApproval(request.getRequiresApproval());
        }
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        if (request.getParentCategoryId() != null) {
            AssetCategory parent = categoryRepository.findById(request.getParentCategoryId())
                .orElseThrow(() -> new RuntimeException("Parent category not found with ID: " + request.getParentCategoryId()));
            category.setParentCategory(parent);
        }

        category = categoryRepository.save(category);
        log.info("Asset category updated successfully: {}", id);

        return mapToResponse(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting asset category with ID: {}", id);

        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Asset category not found with ID: " + id);
        }

        categoryRepository.deleteById(id);
        log.info("Asset category deleted successfully: {}", id);
    }

    private AssetCategoryResponse mapToResponse(AssetCategory category) {
        return AssetCategoryResponse.builder()
            .id(category.getId())
            .parentCategoryId(category.getParentCategory() != null ? category.getParentCategory().getId() : null)
            .parentCategoryName(category.getParentCategory() != null ? category.getParentCategory().getName() : null)
            .name(category.getName())
            .code(category.getCode())
            .description(category.getDescription())
            .primaryCustodianDeptId(category.getPrimaryCustodianDeptId())
            .requiresApproval(category.getRequiresApproval())
            .active(category.getActive())
            .childCategories(category.getChildCategories().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()))
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .build();
    }
}
