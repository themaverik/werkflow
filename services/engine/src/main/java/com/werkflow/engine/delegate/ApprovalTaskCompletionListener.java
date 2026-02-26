package com.werkflow.engine.delegate;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Task listener that captures approval decisions from user task form completions.
 *
 * This listener is triggered when an approval task is completed and:
 * 1. Extracts the approval decision from the form data (approved/rejected)
 * 2. Sets appropriate boolean process variables for decision gateway routing
 * 3. Captures approval comments and metadata for audit trail
 *
 * Process Variables Set:
 * - managerApproved (Boolean): Manager approval decision
 * - vpApproved (Boolean): VP approval decision
 * - cfoApproved (Boolean): CFO approval decision
 * - approvalComments (String): Comments from approver
 * - rejectionReason (String): Reason if rejected
 * - approvedBy (String): Username of approver
 * - approvedAt (Long): Timestamp of approval
 *
 * Expected Form Fields:
 * - decision (String): "APPROVED" or "REJECTED"
 * - comments (String): Approver's comments
 *
 * Usage in BPMN:
 * <userTask id="managerApproval" name="Manager Review">
 *   <extensionElements>
 *     <flowable:taskListener
 *       event="complete"
 *       delegateExpression="${approvalTaskCompletionListener}" />
 *   </extensionElements>
 * </userTask>
 */
@Component("approvalTaskCompletionListener")
public class ApprovalTaskCompletionListener implements TaskListener {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalTaskCompletionListener.class);

    private static final long serialVersionUID = 1L;

    // Form field constants
    private static final String DECISION_FIELD = "decision";
    private static final String COMMENTS_FIELD = "comments";

    // Decision values
    private static final String DECISION_APPROVED = "APPROVED";
    private static final String DECISION_REJECTED = "REJECTED";

    // Task definition key patterns
    private static final String MANAGER_APPROVAL_TASK = "managerApproval";
    private static final String VP_APPROVAL_TASK = "vpApproval";
    private static final String CFO_APPROVAL_TASK = "cfoApproval";

    /**
     * Called when the approval task is completed.
     * Extracts form data and sets process variables.
     *
     * @param delegateTask The completed approval task
     */
    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
            String taskName = delegateTask.getName();

            logger.info("Processing completion of approval task: {} ({})", taskName, taskDefinitionKey);

            // Extract form data from task variables
            String decision = extractDecision(delegateTask);
            String comments = extractComments(delegateTask);

            // Validate decision
            if (decision == null || decision.isEmpty()) {
                logger.error("No decision provided for task: {}", taskName);
                throw new IllegalStateException("Approval decision is required but was not provided");
            }

            // Determine approval status
            boolean approved = DECISION_APPROVED.equalsIgnoreCase(decision);

            // Set approval-specific process variables based on task type
            setApprovalVariable(delegateTask, taskDefinitionKey, approved);

            // Set common process variables
            delegateTask.setVariable("approvedBy", delegateTask.getAssignee());
            delegateTask.setVariable("approvedAt", System.currentTimeMillis());

            if (approved) {
                delegateTask.setVariable("approvalComments", comments);
                logger.info("Task {} approved by {} with comments: {}",
                    taskName, delegateTask.getAssignee(), comments);
            } else {
                delegateTask.setVariable("rejectionReason", comments);
                logger.info("Task {} rejected by {} with reason: {}",
                    taskName, delegateTask.getAssignee(), comments);
            }

            // Log final decision for audit trail
            logger.info("Approval decision captured - Task: {}, Approver: {}, Decision: {}, Comments: {}",
                taskDefinitionKey, delegateTask.getAssignee(), decision, comments);

        } catch (Exception e) {
            logger.error("Error processing approval task completion for task: {}",
                delegateTask.getName(), e);
            // Re-throw to prevent task completion if decision capture fails
            throw new RuntimeException("Failed to capture approval decision", e);
        }
    }

    /**
     * Sets the appropriate approval variable based on the task type.
     *
     * @param task The delegate task
     * @param taskDefinitionKey The task definition key
     * @param approved Whether the decision was approved
     */
    private void setApprovalVariable(DelegateTask task, String taskDefinitionKey, boolean approved) {
        String variableName;

        if (taskDefinitionKey.contains(MANAGER_APPROVAL_TASK)) {
            variableName = "managerApproved";
        } else if (taskDefinitionKey.contains(VP_APPROVAL_TASK)) {
            variableName = "vpApproved";
        } else if (taskDefinitionKey.contains(CFO_APPROVAL_TASK)) {
            variableName = "cfoApproved";
        } else {
            // Fallback for generic approval tasks
            variableName = taskDefinitionKey + "Approved";
            logger.warn("Unknown approval task type: {}, using fallback variable name: {}",
                taskDefinitionKey, variableName);
        }

        task.setVariable(variableName, approved);
        logger.debug("Set process variable {} = {}", variableName, approved);
    }

    /**
     * Extracts the approval decision from task variables.
     * Checks both transient task variables (form data) and process variables.
     *
     * @param task The delegate task
     * @return The decision value (APPROVED/REJECTED)
     */
    private String extractDecision(DelegateTask task) {
        // First check task local variables (form submission data)
        Object decision = task.getVariableLocal(DECISION_FIELD);

        // Fallback to process variables if not in local scope
        if (decision == null) {
            decision = task.getVariable(DECISION_FIELD);
        }

        String decisionStr = decision != null ? decision.toString().trim() : null;

        logger.debug("Extracted decision: {} from task: {}", decisionStr, task.getName());
        return decisionStr;
    }

    /**
     * Extracts approval comments from task variables.
     *
     * @param task The delegate task
     * @return The comments string, or empty string if none provided
     */
    private String extractComments(DelegateTask task) {
        // First check task local variables (form submission data)
        Object comments = task.getVariableLocal(COMMENTS_FIELD);

        // Fallback to process variables if not in local scope
        if (comments == null) {
            comments = task.getVariable(COMMENTS_FIELD);
        }

        String commentsStr = comments != null ? comments.toString().trim() : "";

        logger.debug("Extracted comments: {} from task: {}", commentsStr, task.getName());
        return commentsStr;
    }
}
