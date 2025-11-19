package com.werkflow.finance.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Generic notification delegate for sending emails and system notifications
 */
@Slf4j
@Component("notificationDelegate")
@RequiredArgsConstructor
public class NotificationDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String type = (String) execution.getVariable("notificationType");

        log.info("Executing NotificationDelegate - Type: {}, Process Instance: {}",
                 type, execution.getProcessInstanceId());

        try {
            switch (type) {
                case "BUDGET_SHORTFALL":
                    sendBudgetShortfallNotification(execution);
                    break;
                case "DEPT_HEAD_REJECTED":
                    sendDeptHeadRejectionNotification(execution);
                    break;
                case "EXECUTIVE_REJECTED":
                    sendExecutiveRejectionNotification(execution);
                    break;
                case "LEGAL_REJECTED":
                    sendLegalRejectionNotification(execution);
                    break;
                case "CAPEX_COMPLETED":
                    sendCapExCompletedNotification(execution);
                    break;
                default:
                    log.warn("Unknown notification type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error sending notification", e);
            // Don't fail the process, just log the error
        }
    }

    private void sendBudgetShortfallNotification(DelegateExecution execution) {
        String requester = (String) execution.getVariable("requesterEmail");
        String reason = (String) execution.getVariable("budgetShortfallReason");

        log.info("Sending budget shortfall notification to: {}", requester);

        // TODO: Implement actual email sending
        // EmailService.send(requester, "CapEx Request Rejected - Insufficient Budget",
        //                   "Your CapEx request has been rejected due to: " + reason);
    }

    private void sendDeptHeadRejectionNotification(DelegateExecution execution) {
        String requester = (String) execution.getVariable("requesterEmail");
        String rejectionReason = (String) execution.getVariable("deptHeadRejectionReason");

        log.info("Sending department head rejection notification to: {}", requester);

        // TODO: Implement actual email sending
    }

    private void sendExecutiveRejectionNotification(DelegateExecution execution) {
        String requester = (String) execution.getVariable("requesterEmail");
        String rejectionReason = (String) execution.getVariable("executiveRejectionReason");

        log.info("Sending executive rejection notification to: {}", requester);

        // TODO: Implement actual email sending
    }

    private void sendLegalRejectionNotification(DelegateExecution execution) {
        String requester = (String) execution.getVariable("requesterEmail");
        String rejectionReason = (String) execution.getVariable("legalRejectionReason");

        log.info("Sending legal rejection notification to: {}", requester);

        // TODO: Implement actual email sending
    }

    private void sendCapExCompletedNotification(DelegateExecution execution) {
        String requester = (String) execution.getVariable("requesterEmail");
        String assetId = (String) execution.getVariable("assetId");

        log.info("Sending CapEx completion notification to: {}", requester);

        // TODO: Implement actual email sending
        // Include asset registration details and tracking information
    }
}
