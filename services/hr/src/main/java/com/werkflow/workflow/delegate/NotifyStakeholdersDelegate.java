package com.werkflow.workflow.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Flowable delegate for notifying stakeholders about onboarding completion.
 * In a production system, this would integrate with email/notification service.
 */
@Slf4j
@Component("notifyStakeholdersDelegate")
@RequiredArgsConstructor
public class NotifyStakeholdersDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing NotifyStakeholdersDelegate for process instance: {}", execution.getProcessInstanceId());

        Long employeeId = (Long) execution.getVariable("employeeId");
        String employeeName = (String) execution.getVariable("employeeName");
        String position = (String) execution.getVariable("position");
        Long managerId = (Long) execution.getVariable("managerId");
        Boolean orientationCompleted = (Boolean) execution.getVariable("orientationCompleted");

        // In production, integrate with email service to notify multiple stakeholders
        log.info("Onboarding complete notification sent for employee: {} (ID: {}), Position: {}",
                employeeName, employeeId, position);

        // Simulate notifications to various stakeholders
        String subject = String.format("Onboarding Complete - %s", employeeName);

        // Notify Manager
        String managerMessage = String.format(
                "Dear Manager,\n\n" +
                        "The onboarding process for %s (Position: %s) has been completed successfully.\n\n" +
                        "The employee is now active in the system and ready to begin work.\n\n" +
                        "Best regards,\nHR Team",
                employeeName, position
        );
        log.debug("Manager notification: {}", managerMessage);

        // Notify HR
        String hrMessage = String.format(
                "Dear HR Team,\n\n" +
                        "Onboarding completed for:\n" +
                        "Employee: %s (ID: %d)\n" +
                        "Position: %s\n" +
                        "Orientation Completed: %s\n" +
                        "Status: ACTIVE\n\n" +
                        "All onboarding tasks have been completed.\n\n" +
                        "Best regards,\nWorkflow System",
                employeeName, employeeId, position, orientationCompleted ? "Yes" : "No"
        );
        log.debug("HR notification: {}", hrMessage);

        // Notify IT
        String itMessage = String.format(
                "Dear IT Team,\n\n" +
                        "Onboarding completed for employee: %s (ID: %d)\n\n" +
                        "Please ensure all system access is active and monitor for any issues.\n\n" +
                        "Best regards,\nHR System",
                employeeName, employeeId
        );
        log.debug("IT notification: {}", itMessage);

        execution.setVariable("stakeholdersNotified", true);
        execution.setVariable("notificationsSent", 3); // Manager, HR, IT
    }
}
