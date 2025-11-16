package com.whrkflow.workflow.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Flowable delegate for notifying employees about leave request decisions.
 * In a production system, this would integrate with email/notification service.
 */
@Slf4j
@Component("notifyEmployeeDelegate")
@RequiredArgsConstructor
public class NotifyEmployeeDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing NotifyEmployeeDelegate for process instance: {}", execution.getProcessInstanceId());

        Long employeeId = (Long) execution.getVariable("employeeId");
        String employeeName = (String) execution.getVariable("employeeName");
        String finalStatus = (String) execution.getVariable("finalStatus");
        String managerComments = (String) execution.getVariable("managerComments");

        // In production, integrate with email service (e.g., Spring Mail, SendGrid, AWS SES)
        log.info("Notification sent to employee {} (ID: {}): Leave request {}",
                employeeName, employeeId, finalStatus);

        if (managerComments != null && !managerComments.isEmpty()) {
            log.info("Manager comments: {}", managerComments);
        }

        // Simulate email notification
        String subject = "Leave Request " + finalStatus;
        String message = String.format(
                "Dear %s,\n\nYour leave request has been %s.\n\n%s\n\nBest regards,\nHR Team",
                employeeName, finalStatus.toLowerCase(),
                managerComments != null ? "Comments: " + managerComments : ""
        );

        log.debug("Email notification: Subject='{}', Message='{}'", subject, message);

        execution.setVariable("employeeNotified", true);
    }
}
