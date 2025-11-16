package com.whrkflow.workflow.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Flowable delegate for sending welcome email to new employees.
 * In a production system, this would integrate with email service.
 */
@Slf4j
@Component("sendWelcomeEmailDelegate")
@RequiredArgsConstructor
public class SendWelcomeEmailDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing SendWelcomeEmailDelegate for process instance: {}", execution.getProcessInstanceId());

        Long employeeId = (Long) execution.getVariable("employeeId");
        String employeeName = (String) execution.getVariable("employeeName");
        String employeeEmail = (String) execution.getVariable("employeeEmail");
        String position = (String) execution.getVariable("position");
        LocalDate startDate = (LocalDate) execution.getVariable("startDate");

        // In production, integrate with email service
        log.info("Welcome email sent to new employee: {} ({}) at {}", employeeName, employeeId, employeeEmail);

        // Simulate welcome email
        String subject = String.format("Welcome to the Team, %s!", employeeName);
        String message = String.format(
                "Dear %s,\n\n" +
                        "Welcome to our organization! We're excited to have you join us as a %s.\n\n" +
                        "Your start date is scheduled for %s. All necessary accounts and equipment have been set up for you.\n\n" +
                        "On your first day, please report to the HR department at 9:00 AM for orientation.\n\n" +
                        "If you have any questions before your start date, please don't hesitate to reach out.\n\n" +
                        "Best regards,\nHR Team",
                employeeName, position, startDate
        );

        log.debug("Welcome email: Subject='{}', To='{}', Message='{}'", subject, employeeEmail, message);

        execution.setVariable("welcomeEmailSent", true);
        execution.setVariable("welcomeEmailTimestamp", LocalDate.now());
    }
}
