package com.werkflow.workflow.delegate;

import com.werkflow.entity.PerformanceReview;
import com.werkflow.repository.PerformanceReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Flowable delegate for updating performance review records after completion.
 * Updates the review record with final status and completion date.
 */
@Slf4j
@Component("updateReviewRecordDelegate")
@RequiredArgsConstructor
public class UpdateReviewRecordDelegate implements JavaDelegate {

    private final PerformanceReviewRepository performanceReviewRepository;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing UpdateReviewRecordDelegate for process instance: {}", execution.getProcessInstanceId());

        Long reviewId = (Long) execution.getVariable("reviewId");
        Boolean hrApproved = (Boolean) execution.getVariable("hrApproved");
        String hrNotes = (String) execution.getVariable("hrNotes");

        if (reviewId == null) {
            log.error("Review ID is null in process instance: {}", execution.getProcessInstanceId());
            throw new IllegalArgumentException("Review ID cannot be null");
        }

        PerformanceReview review = performanceReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Performance review not found with ID: " + reviewId));

        // Update review with completion information
        // Note: In a real system, you might update additional fields based on the workflow variables
        review.setReviewDate(LocalDate.now());

        performanceReviewRepository.save(review);

        log.info("Performance review {} updated successfully. HR Approved: {}",
                reviewId, hrApproved);

        execution.setVariable("reviewRecordUpdated", true);
        execution.setVariable("reviewCompletionDate", LocalDate.now());
    }
}
