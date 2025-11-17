package com.werkflow.finance.service;

import com.werkflow.finance.dto.BudgetLineItemRequest;
import com.werkflow.finance.dto.BudgetLineItemResponse;
import com.werkflow.finance.entity.BudgetLineItem;
import com.werkflow.finance.repository.BudgetLineItemRepository;
import com.werkflow.finance.repository.BudgetPlanRepository;
import com.werkflow.finance.repository.BudgetCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetLineItemService {
    private final BudgetLineItemRepository lineItemRepository;
    private final BudgetPlanRepository budgetPlanRepository;
    private final BudgetCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<BudgetLineItemResponse> getLineItemsByBudgetPlan(Long budgetPlanId) {
        return lineItemRepository.findByBudgetPlanId(budgetPlanId).stream()
            .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public BudgetLineItemResponse createLineItem(BudgetLineItemRequest request) {
        var budgetPlan = budgetPlanRepository.findById(request.getBudgetPlanId())
            .orElseThrow(() -> new RuntimeException("Budget plan not found"));
        var category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new RuntimeException("Category not found"));

        BudgetLineItem lineItem = BudgetLineItem.builder()
            .budgetPlan(budgetPlan)
            .category(category)
            .description(request.getDescription())
            .allocatedAmount(request.getAllocatedAmount())
            .spentAmount(BigDecimal.ZERO)
            .notes(request.getNotes())
            .build();
        return toResponse(lineItemRepository.save(lineItem));
    }

    private BudgetLineItemResponse toResponse(BudgetLineItem item) {
        return BudgetLineItemResponse.builder()
            .id(item.getId())
            .budgetPlanId(item.getBudgetPlan().getId())
            .categoryId(item.getCategory().getId())
            .categoryName(item.getCategory().getName())
            .description(item.getDescription())
            .allocatedAmount(item.getAllocatedAmount())
            .spentAmount(item.getSpentAmount())
            .notes(item.getNotes())
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }
}
