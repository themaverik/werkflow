package com.werkflow.delegates.form;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic Form Request Delegate for cross-department form-based requests
 *
 * This delegate enables departments to handle form-based requests from other departments
 * without code changes. Each department maintains their own resource catalogs.
 *
 * Configurable via BPMN process variables:
 * - targetDepartment: Department to handle request (IT, Admin, HR, Finance, etc.) (required)
 * - formType: Type of request form (asset, access, facility, equipment, training) (required)
 * - formData: Form submission data (required)
 * - requestorId: User making the request (required)
 * - requestorDepartment: Department of requestor (optional)
 * - priority: Request priority (low, normal, high, urgent) - default: normal
 * - autoAssignToRole: Auto-assign to specific role (optional)
 * - targetServiceUrl: Department service URL (optional, defaults to convention)
 * - notifyOnCreation: Send notification when request created (default: true)
 * - responseVariable: Variable to store response (default: "formRequestResponse")
 *
 * Example use cases:
 * 1. Asset Request (IT Department):
 *    - Employee fills form for laptop/equipment
 *    - Workflow delegates to IT service
 *    - IT maintains asset catalog
 *    - Auto-assigns to IT_ADMIN role
 *
 * 2. Access Request (IT Security):
 *    - Employee requests system access
 *    - Workflow delegates to IT Security service
 *    - IT Security approves/provisions access
 *
 * 3. Facility Request (Admin):
 *    - Employee requests meeting room, parking, etc.
 *    - Workflow delegates to Admin service
 *    - Admin team handles booking
 *
 * Example BPMN configuration:
 * <serviceTask id="submitAssetRequest" flowable:delegateExpression="${formRequestDelegate}">
 *   <extensionElements>
 *     <flowable:field name="targetDepartment">
 *       <flowable:string>IT</flowable:string>
 *     </flowable:field>
 *     <flowable:field name="formType">
 *       <flowable:string>asset</flowable:string>
 *     </flowable:field>
 *     <flowable:field name="formData">
 *       <flowable:expression>#{assetRequestForm}</flowable:expression>
 *     </flowable:field>
 *     <flowable:field name="requestorId">
 *       <flowable:expression>${requestorId}</flowable:expression>
 *     </flowable:field>
 *     <flowable:field name="autoAssignToRole">
 *       <flowable:string>IT_ADMIN</flowable:string>
 *     </flowable:field>
 *     <flowable:field name="priority">
 *       <flowable:string>high</flowable:string>
 *     </flowable:field>
 *   </extensionElements>
 * </serviceTask>
 */
@Slf4j
@Component("formRequestDelegate")
@RequiredArgsConstructor
public class FormRequestDelegate implements JavaDelegate {

    private final WebClient.Builder webClientBuilder;

    private static final Map<String, String> SERVICE_URL_MAP = new HashMap<>();

    static {
        SERVICE_URL_MAP.put("IT", "http://admin-service:8083/api/requests/it");
        SERVICE_URL_MAP.put("ADMIN", "http://admin-service:8083/api/requests/admin");
        SERVICE_URL_MAP.put("HR", "http://hr-service:8082/api/requests/hr");
        SERVICE_URL_MAP.put("FINANCE", "http://finance-service:8084/api/requests/finance");
        SERVICE_URL_MAP.put("PROCUREMENT", "http://procurement-service:8085/api/requests/procurement");
        SERVICE_URL_MAP.put("LEGAL", "http://legal-service:8086/api/requests/legal");
    }

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing FormRequestDelegate for process: {}", execution.getProcessInstanceId());

        // Get configuration
        String targetDepartment = getRequiredVariable(execution, "targetDepartment");
        String formType = getRequiredVariable(execution, "formType");
        Map<String, Object> formData = getRequiredVariable(execution, "formData");
        String requestorId = getRequiredVariable(execution, "requestorId");

        String requestorDepartment = getVariable(execution, "requestorDepartment", "");
        String priority = getVariable(execution, "priority", "normal");
        String autoAssignToRole = getVariable(execution, "autoAssignToRole", null);
        String targetServiceUrl = getVariable(execution, "targetServiceUrl", null);
        Boolean notifyOnCreation = getVariable(execution, "notifyOnCreation", true);
        String responseVariable = getVariable(execution, "responseVariable", "formRequestResponse");

        log.debug("Form request: department={}, type={}, requestor={}",
            targetDepartment, formType, requestorId);

        // Build request payload
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("department", targetDepartment);
        requestPayload.put("formType", formType);
        requestPayload.put("formData", formData);
        requestPayload.put("requestorId", requestorId);
        requestPayload.put("requestorDepartment", requestorDepartment);
        requestPayload.put("priority", priority);
        requestPayload.put("processInstanceId", execution.getProcessInstanceId());
        requestPayload.put("submittedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        if (autoAssignToRole != null) {
            requestPayload.put("autoAssignToRole", autoAssignToRole);
        }

        // Determine target service URL
        String serviceUrl = targetServiceUrl != null ? targetServiceUrl :
            SERVICE_URL_MAP.getOrDefault(targetDepartment.toUpperCase(), null);

        if (serviceUrl == null) {
            throw new IllegalArgumentException("No service URL configured for department: " + targetDepartment);
        }

        try {
            // Submit request to department service
            WebClient webClient = webClientBuilder.build();

            Map<String, Object> response = webClient
                .post()
                .uri(serviceUrl)
                .header("Content-Type", "application/json")
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(java.time.Duration.ofSeconds(30))
                .block();

            log.info("Form request submitted successfully. Request ID: {}", response.get("requestId"));

            // Store response
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("requestId", response.get("requestId"));
            result.put("status", response.get("status"));
            result.put("assignedTo", response.get("assignedTo"));
            result.put("targetDepartment", targetDepartment);
            result.put("formType", formType);
            result.put("submittedAt", requestPayload.get("submittedAt"));

            execution.setVariable(responseVariable, result);
            execution.setVariable("formRequestId", response.get("requestId"));
            execution.setVariable("formRequestStatus", response.get("status"));

            // Trigger notification if enabled
            if (notifyOnCreation) {
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("type", "form_request_created");
                notificationData.put("requestId", response.get("requestId"));
                notificationData.put("department", targetDepartment);
                notificationData.put("formType", formType);
                notificationData.put("assignedTo", response.get("assignedTo"));

                execution.setVariable("sendFormRequestNotification", true);
                execution.setVariable("formRequestNotificationData", notificationData);
            }

        } catch (Exception e) {
            log.error("Failed to submit form request: {}", e.getMessage(), e);

            // Store error
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("targetDepartment", targetDepartment);
            errorResult.put("formType", formType);

            execution.setVariable(responseVariable, errorResult);

            throw new RuntimeException("Form request submission failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getRequiredVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);
        if (value == null) {
            throw new IllegalArgumentException("Required variable '" + variableName + "' is not set");
        }
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    private <T> T getVariable(DelegateExecution execution, String variableName, T defaultValue) {
        Object value = execution.getVariable(variableName);
        return value != null ? (T) value : defaultValue;
    }
}
