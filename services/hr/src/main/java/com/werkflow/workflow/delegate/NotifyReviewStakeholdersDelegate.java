package com.werkflow.workflow.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Flowable delegate for notifying stakeholders about performance review completion.
 * In a production system, this would integrate with email/notification service.
 */
@Slf4j
@Component("notifyReviewStakeholdersDelegate")
@RequiredArgsConstructor
public class NotifyReviewStakeholdersDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing NotifyReviewStakeholdersDelegate for process instance: {}", execution.getProcessInstanceId());

        Long reviewId = (Long) execution.getVariable("reviewId");
        Long employeeId = (Long) execution.getVariable("employeeId");
        String employeeName = (String) execution.getVariable("employeeName");
        String overallRating = (String) execution.getVariable("overallRating");
        Boolean hrApproved = (Boolean) execution.getVariable("hrApproved");
        String reviewType = (String) execution.getVariable("reviewType");
        LocalDate completionDate = (LocalDate) execution.getVariable("reviewCompletionDate");

        // In production, integrate with email service
        log.info("Performance review completion notification sent for: {} (ID: {}), Rating: {}",
                employeeName, employeeId, overallRating);

        // Simulate notification to Employee
        String employeeSubject = "Performance Review Complete";
        String employeeMessage = String.format(
                "Dear %s,\n\n" +
                        "Your %s performance review has been completed and approved by HR.\n\n" +
                        "Overall Rating: %s\n" +
                        "Review Date: %s\n\n" +
                        "A copy of your complete review is available in the employee portal. " +
                        "Please schedule a follow-up meeting with your manager to discuss development plans.\n\n" +
                        "Best regards,\nHR Team",
                employeeName, reviewType != null ? reviewType.toLowerCase() : "performance",
                overallRating, completionDate
        );
        log.debug("Employee notification: Subject='{}', Message='{}'", employeeSubject, employeeMessage);

        // Simulate notification to Manager
        String managerSubject = String.format("Performance Review Approved - %s", employeeName);
        String managerMessage = String.format(
                "Dear Manager,\n\n" +
                        "The performance review for %s (ID: %d) has been completed and approved by HR.\n\n" +
                        "Review ID: %d\n" +
                        "Overall Rating: %s\n" +
                        "HR Approved: %s\n" +
                        "Completion Date: %s\n\n" +
                        "Please ensure any agreed-upon development plans or action items are followed up.\n\n" +
                        "Best regards,\nHR Team",
                employeeName, employeeId, reviewId, overallRating,
                hrApproved ? "Yes" : "No", completionDate
        );
        log.debug("Manager notification: Subject='{}', Message='{}'", managerSubject, managerMessage);

        // Simulate notification to HR
        String hrSubject = String.format("Performance Review Process Complete - %s", employeeName);
        String hrMessage = String.format(
                "Dear HR Team,\n\n" +
                        "Performance review workflow completed:\n\n" +
                        "Employee: %s (ID: %d)\n" +
                        "Review ID: %d\n" +
                        "Review Type: %s\n" +
                        "Overall Rating: %s\n" +
                        "HR Approved: %s\n" +
                        "Completion Date: %s\n\n" +
                        "All stakeholders have been notified.\n\n" +
                        "Best regards,\nWorkflow System",
                employeeName, employeeId, reviewId, reviewType, overallRating,
                hrApproved ? "Yes" : "No", completionDate
        );
        log.debug("HR notification: Subject='{}', Message='{}'", hrSubject, hrMessage);

        execution.setVariable("reviewStakeholdersNotified", true);
        execution.setVariable("notificationsSent", 3); // Employee, Manager, HR
    }
}
