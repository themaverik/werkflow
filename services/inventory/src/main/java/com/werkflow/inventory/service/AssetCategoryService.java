package com.werkflow.inventory.service;

import com.werkflow.inventory.entity.AssetCategory;
import com.werkflow.inventory.repository.AssetCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;

/**
 * Service for AssetCategory operations
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AssetCategoryService {

    private final AssetCategoryRepository categoryRepository;

    /**
     * Create a new asset category
     */
    public AssetCategory createCategory(AssetCategory category) {
        log.info("Creating new asset category: {}", category.getName());

        if (category.getCode() != null) {
            if (categoryRepository.findByCode(category.getCode()).isPresent()) {
                throw new IllegalArgumentException("Category code already exists: " + category.getCode());
            }
        }

        AssetCategory saved = categoryRepository.save(category);
        log.info("Asset category created with id: {}", saved.getId());
        return saved;
    }

    /**
     * Get asset category by ID
     */
    @Transactional(readOnly = true)
    public AssetCategory getCategoryById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Asset category not found with id: " + id));
    }

    /**
     * Get asset category by code
     */
    @Transactional(readOnly = true)
    public AssetCategory getCategoryByCode(String code) {
        return categoryRepository.findByCode(code)
            .orElseThrow(() -> new EntityNotFoundException("Asset category not found with code: " + code));
    }

    /**
     * Get all active categories
     */
    @Transactional(readOnly = true)
    public List<AssetCategory> getActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }

    /**
     * Get all categories
     */
    @Transactional(readOnly = true)
    public List<AssetCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Get root categories
     */
    @Transactional(readOnly = true)
    public List<AssetCategory> getRootCategories() {
        return categoryRepository.findRootCategories();
    }

    /**
     * Get child categories
     */
    @Transactional(readOnly = true)
    public List<AssetCategory> getChildCategories(Long parentId) {
        return categoryRepository.findByParentCategoryIdAndActiveTrue(parentId);
    }

    /**
     * Update asset category
     */
    public AssetCategory updateCategory(Long id, AssetCategory categoryDetails) {
        log.info("Updating asset category with id: {}", id);

        AssetCategory category = getCategoryById(id);

        category.setName(categoryDetails.getName());
        category.setCode(categoryDetails.getCode());
        category.setDescription(categoryDetails.getDescription());
        category.setRequiresApproval(categoryDetails.getRequiresApproval());
        category.setActive(categoryDetails.getActive());
        category.setPrimaryCustodianDeptId(categoryDetails.getPrimaryCustodianDeptId());

        AssetCategory updated = categoryRepository.save(category);
        log.info("Asset category updated with id: {}", id);
        return updated;
    }

    /**
     * Deactivate category
     */
    public AssetCategory deactivateCategory(Long id) {
        log.info("Deactivating asset category with id: {}", id);

        AssetCategory category = getCategoryById(id);
        category.setActive(false);

        return categoryRepository.save(category);
    }

    /**
     * Activate category
     */
    public AssetCategory activateCategory(Long id) {
        log.info("Activating asset category with id: {}", id);

        AssetCategory category = getCategoryById(id);
        category.setActive(true);

        return categoryRepository.save(category);
    }

    /**
     * Delete category
     */
    public void deleteCategory(Long id) {
        log.info("Deleting asset category with id: {}", id);

        if (!categoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Asset category not found with id: " + id);
        }

        categoryRepository.deleteById(id);
        log.info("Asset category deleted with id: {}", id);
    }

    /**
     * Search categories
     */
    @Transactional(readOnly = true)
    public List<AssetCategory> searchCategories(String searchTerm) {
        return categoryRepository.searchCategories(searchTerm);
    }
}
