package com.werkflow.finance.controller;

import com.werkflow.dto.CapExRequestDto;
import com.werkflow.dto.CapExResponseDto;
import com.werkflow.service.CapExService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for CapEx Workflow Integration
 *
 * These endpoints are designed to be called by the Flowable BPMN process
 * via RestServiceDelegate. They handle workflow-specific operations for
 * CapEx approval processes.
 *
 * @author Werkflow Development Team
 * @since Phase 5B - CapEx Workflow Migration
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/capex")
@RequiredArgsConstructor
@Tag(name = "CapEx Workflow", description = "Workflow integration APIs for CapEx approval process")
public class CapExWorkflowController {

    private final CapExService capexService;

    /**
     * Create a new CapEx request from workflow
     * Called by BPMN process start event
     *
     * @param requestBody Workflow variables including CapEx request data
     * @return Created CapEx request with ID and request number
     */
    @PostMapping("/create-request")
    @Operation(
        summary = "Create CapEx request (Workflow)",
        description = "Create a new CapEx request initiated from BPMN workflow"
    )
    public ResponseEntity<Map<String, Object>> createRequest(
            @RequestBody Map<String, Object> requestBody) {

        String requestedBy = (String) requestBody.get("requestedBy");
        String departmentName = (String) requestBody.get("departmentName");

        log.info("Workflow: Creating CapEx request for department: {}, requestedBy: {}",
            departmentName, requestedBy);

        try {
            // Build CapExRequestDto from workflow variables
            CapExRequestDto request = CapExRequestDto.builder()
                .title((String) requestBody.get("title"))
                .description((String) requestBody.get("description"))
                .category(com.werkflow.entity.CapExCategory.valueOf((String) requestBody.get("category")))
                .amount(new java.math.BigDecimal(requestBody.get("amount").toString()))
                .priority(com.werkflow.entity.Priority.valueOf((String) requestBody.get("priority")))
                .approvalLevel(com.werkflow.entity.ApprovalLevel.valueOf((String) requestBody.get("approvalLevel")))
                .businessJustification((String) requestBody.get("businessJustification"))
                .expectedBenefits((String) requestBody.get("expectedBenefits"))
                .expectedCompletionDate(java.time.LocalDate.parse((String) requestBody.get("expectedCompletionDate")))
                .budgetYear((Integer) requestBody.get("budgetYear"))
                .departmentName(departmentName)
                .build();

            // Create the CapEx request
            CapExResponseDto response = capexService.createCapExRequest(request, requestedBy);

            // Prepare workflow response with variables
            Map<String, Object> workflowResponse = new HashMap<>();
            workflowResponse.put("capexId", response.getId());
            workflowResponse.put("requestNumber", response.getRequestNumber());
            workflowResponse.put("requestAmount", response.getAmount());
            workflowResponse.put("departmentName", response.getDepartmentName());
            workflowResponse.put("requestedBy", response.getRequestedBy());
            workflowResponse.put("status", response.getStatus().name());
            workflowResponse.put("success", true);
            workflowResponse.put("message", "CapEx request created successfully");

            log.info("Workflow: CapEx request created - ID: {}, Number: {}",
                response.getId(), response.getRequestNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(workflowResponse);

        } catch (Exception e) {
            log.error("Workflow: Failed to create CapEx request", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Failed to create CapEx request");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Check budget availability for CapEx request
     * Called by BPMN process service task
     *
     * @param requestBody Contains capexId and requestAmount
     * @return Budget check result with available/allocated amounts
     */
    @PostMapping("/check-budget")
    @Operation(
        summary = "Check budget availability (Workflow)",
        description = "Verify budget availability for CapEx request amount"
    )
    public ResponseEntity<Map<String, Object>> checkBudget(
            @RequestBody Map<String, Object> requestBody) {

        Long capexId = Long.valueOf(requestBody.get("capexId").toString());
        Double requestAmount = Double.valueOf(requestBody.get("requestAmount").toString());
        String departmentName = requestBody.get("departmentName").toString();

        log.info("Workflow: Checking budget for CapEx ID: {}, Amount: ${}, Department: {}",
            capexId, requestAmount, departmentName);

        try {
            // Get the CapEx request
            CapExResponseDto capex = capexService.getCapExRequestById(capexId);

            // For MVP: Simple budget check logic
            // In production: This would query budget service for actual budget data
            boolean budgetAvailable = true;
            double availableBudget = 1000000.0; // Mock value
            double allocatedBudget = 0.0;       // Mock value

            Map<String, Object> workflowResponse = new HashMap<>();
            workflowResponse.put("budgetAvailable", budgetAvailable);
            workflowResponse.put("availableBudget", availableBudget);
            workflowResponse.put("allocatedBudget", allocatedBudget);
            workflowResponse.put("requestAmount", requestAmount);
            workflowResponse.put("departmentName", departmentName);
            workflowResponse.put("success", true);
            workflowResponse.put("message", budgetAvailable ?
                "Budget available for request" : "Insufficient budget");

            log.info("Workflow: Budget check result - Available: {}, Amount: ${}",
                budgetAvailable, availableBudget);

            return ResponseEntity.ok(workflowResponse);

        } catch (Exception e) {
            log.error("Workflow: Failed to check budget for CapEx ID: {}", capexId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("budgetAvailable", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Failed to check budget availability");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Allocate budget for approved CapEx request
     * Called by BPMN process after approval
     *
     * @param requestBody Contains capexId and requestAmount
     * @return Budget allocation result
     */
    @PostMapping("/allocate")
    @Operation(
        summary = "Allocate budget (Workflow)",
        description = "Reserve and allocate budget for approved CapEx request"
    )
    public ResponseEntity<Map<String, Object>> allocateBudget(
            @RequestBody Map<String, Object> requestBody) {

        Long capexId = Long.valueOf(requestBody.get("capexId").toString());
        Double requestAmount = Double.valueOf(requestBody.get("requestAmount").toString());
        String departmentName = requestBody.get("departmentName").toString();

        log.info("Workflow: Allocating budget for CapEx ID: {}, Amount: ${}",
            capexId, requestAmount);

        try {
            // Get the CapEx request
            CapExResponseDto capex = capexService.getCapExRequestById(capexId);

            // For MVP: Simple allocation logic
            // In production: This would create budget allocation records
            boolean allocationSuccessful = true;
            String allocationId = "ALLOC-" + capexId + "-" + System.currentTimeMillis();

            Map<String, Object> workflowResponse = new HashMap<>();
            workflowResponse.put("allocationSuccessful", allocationSuccessful);
            workflowResponse.put("allocationId", allocationId);
            workflowResponse.put("allocatedAmount", requestAmount);
            workflowResponse.put("departmentName", departmentName);
            workflowResponse.put("success", true);
            workflowResponse.put("message", "Budget allocated successfully");

            log.info("Workflow: Budget allocated - Allocation ID: {}", allocationId);

            return ResponseEntity.ok(workflowResponse);

        } catch (Exception e) {
            log.error("Workflow: Failed to allocate budget for CapEx ID: {}", capexId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("allocationSuccessful", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Failed to allocate budget");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Update CapEx request status from workflow
     * Called by BPMN process at various stages
     *
     * @param requestBody Contains capexId, status, and optional comments
     * @return Updated CapEx request status
     */
    @PutMapping("/update-status")
    @Operation(
        summary = "Update CapEx status (Workflow)",
        description = "Update CapEx request status during workflow execution"
    )
    public ResponseEntity<Map<String, Object>> updateStatus(
            @RequestBody Map<String, Object> requestBody) {

        Long capexId = Long.valueOf(requestBody.get("capexId").toString());
        String statusStr = requestBody.get("status").toString();
        String comments = requestBody.getOrDefault("comments", "").toString();

        log.info("Workflow: Updating CapEx ID: {} to status: {}", capexId, statusStr);

        try {
            // Parse status from workflow variable
            com.werkflow.entity.CapExStatus status =
                com.werkflow.entity.CapExStatus.valueOf(statusStr);

            // Update the status
            CapExResponseDto response = capexService.updateCapExRequestStatus(capexId, status);

            Map<String, Object> workflowResponse = new HashMap<>();
            workflowResponse.put("capexId", response.getId());
            workflowResponse.put("requestNumber", response.getRequestNumber());
            workflowResponse.put("status", response.getStatus().name());
            workflowResponse.put("success", true);
            workflowResponse.put("message", "CapEx status updated successfully");

            log.info("Workflow: Status updated - CapEx ID: {}, New Status: {}",
                capexId, status);

            return ResponseEntity.ok(workflowResponse);

        } catch (Exception e) {
            log.error("Workflow: Failed to update status for CapEx ID: {}", capexId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Failed to update CapEx status");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Get CapEx request details for workflow
     * Called by BPMN process to retrieve request data
     *
     * @param capexId CapEx request ID
     * @return CapEx request details formatted for workflow
     */
    @GetMapping("/{capexId}")
    @Operation(
        summary = "Get CapEx details (Workflow)",
        description = "Retrieve CapEx request details during workflow execution"
    )
    public ResponseEntity<Map<String, Object>> getCapExForWorkflow(
            @PathVariable Long capexId) {

        log.info("Workflow: Retrieving CapEx details for ID: {}", capexId);

        try {
            CapExResponseDto capex = capexService.getCapExRequestById(capexId);

            Map<String, Object> workflowResponse = new HashMap<>();
            workflowResponse.put("capexId", capex.getId());
            workflowResponse.put("requestNumber", capex.getRequestNumber());
            workflowResponse.put("requestAmount", capex.getAmount());
            workflowResponse.put("departmentName", capex.getDepartmentName());
            workflowResponse.put("requestedBy", capex.getRequestedBy());
            workflowResponse.put("status", capex.getStatus().name());
            workflowResponse.put("category", capex.getCategory().name());
            workflowResponse.put("priority", capex.getPriority().name());
            workflowResponse.put("title", capex.getTitle());
            workflowResponse.put("description", capex.getDescription());
            workflowResponse.put("success", true);

            return ResponseEntity.ok(workflowResponse);

        } catch (Exception e) {
            log.error("Workflow: Failed to retrieve CapEx ID: {}", capexId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Failed to retrieve CapEx details");

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
}
