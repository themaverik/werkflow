package com.whrkflow.workflow.delegate;

import com.whrkflow.entity.Leave;
import com.whrkflow.entity.LeaveStatus;
import com.whrkflow.repository.LeaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Flowable delegate for approving leave requests.
 * Updates leave status to APPROVED in the database.
 */
@Slf4j
@Component("leaveApprovalDelegate")
@RequiredArgsConstructor
public class LeaveApprovalDelegate implements JavaDelegate {

    private final LeaveRepository leaveRepository;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing LeaveApprovalDelegate for process instance: {}", execution.getProcessInstanceId());

        Long leaveId = (Long) execution.getVariable("leaveId");
        String managerComments = (String) execution.getVariable("managerComments");

        if (leaveId == null) {
            log.error("Leave ID is null in process instance: {}", execution.getProcessInstanceId());
            throw new IllegalArgumentException("Leave ID cannot be null");
        }

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found with ID: " + leaveId));

        leave.setStatus(LeaveStatus.APPROVED);
        leaveRepository.save(leave);

        log.info("Leave request {} approved successfully", leaveId);

        // Store result in process variable
        execution.setVariable("leaveApprovalComplete", true);
        execution.setVariable("finalStatus", "APPROVED");
    }
}
