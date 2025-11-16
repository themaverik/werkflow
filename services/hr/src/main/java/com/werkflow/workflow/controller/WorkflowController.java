package com.werkflow.workflow.controller;

import com.werkflow.workflow.dto.*;
import com.werkflow.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing Flowable BPM workflows.
 * Provides endpoints for starting processes, managing tasks, and querying workflow state.
 */
@Slf4j
@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflow Management", description = "APIs for managing BPM workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    @Operation(summary = "Start a new workflow process",
            description = "Starts a new instance of a workflow process with the provided variables")
    @PostMapping("/processes/start")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER', 'MANAGER')")
    public ResponseEntity<ProcessInstanceResponse> startProcess(
            @RequestBody StartProcessRequest request) {
        log.info("REST request to start process: {}", request.getProcessDefinitionKey());
        ProcessInstanceResponse response = workflowService.startProcess(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get process instance by ID",
            description = "Retrieves detailed information about a specific process instance")
    @GetMapping("/processes/{processInstanceId}")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER', 'MANAGER')")
    public ResponseEntity<ProcessInstanceResponse> getProcessInstance(
            @Parameter(description = "Process instance ID") @PathVariable String processInstanceId) {
        log.info("REST request to get process instance: {}", processInstanceId);
        ProcessInstanceResponse response = workflowService.getProcessInstance(processInstanceId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all process instances for a process definition",
            description = "Retrieves all active process instances for a specific process definition")
    @GetMapping("/processes")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER', 'MANAGER')")
    public ResponseEntity<List<ProcessInstanceResponse>> getProcessInstances(
            @Parameter(description = "Process definition key") @RequestParam String processDefinitionKey) {
        log.info("REST request to get process instances for: {}", processDefinitionKey);
        List<ProcessInstanceResponse> responses = workflowService.getProcessInstances(processDefinitionKey);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Delete a process instance",
            description = "Deletes a running process instance with a specified reason")
    @DeleteMapping("/processes/{processInstanceId}")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Void> deleteProcessInstance(
            @Parameter(description = "Process instance ID") @PathVariable String processInstanceId,
            @Parameter(description = "Deletion reason") @RequestParam(required = false, defaultValue = "Cancelled by user") String deleteReason) {
        log.info("REST request to delete process instance: {}", processInstanceId);
        workflowService.deleteProcessInstance(processInstanceId, deleteReason);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get process variables",
            description = "Retrieves all variables for a specific process instance")
    @GetMapping("/processes/{processInstanceId}/variables")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getProcessVariables(
            @Parameter(description = "Process instance ID") @PathVariable String processInstanceId) {
        log.info("REST request to get variables for process instance: {}", processInstanceId);
        Map<String, Object> variables = workflowService.getProcessVariables(processInstanceId);
        return ResponseEntity.ok(variables);
    }

    @Operation(summary = "Set process variables",
            description = "Sets or updates variables for a specific process instance")
    @PutMapping("/processes/{processInstanceId}/variables")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER', 'MANAGER')")
    public ResponseEntity<Void> setProcessVariables(
            @Parameter(description = "Process instance ID") @PathVariable String processInstanceId,
            @RequestBody Map<String, Object> variables) {
        log.info("REST request to set variables for process instance: {}", processInstanceId);
        workflowService.setProcessVariables(processInstanceId, variables);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get tasks by assignee",
            description = "Retrieves all tasks assigned to a specific user")
    @GetMapping("/tasks/assignee/{assignee}")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<TaskResponse>> getTasksByAssignee(
            @Parameter(description = "Assignee user ID") @PathVariable String assignee) {
        log.info("REST request to get tasks for assignee: {}", assignee);
        List<TaskResponse> tasks = workflowService.getTasksByAssignee(assignee);
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Get tasks by group",
            description = "Retrieves all tasks available to a specific candidate group")
    @GetMapping("/tasks/group/{group}")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER', 'MANAGER')")
    public ResponseEntity<List<TaskResponse>> getTasksByGroup(
            @Parameter(description = "Group name") @PathVariable String group) {
        log.info("REST request to get tasks for group: {}", group);
        List<TaskResponse> tasks = workflowService.getTasksByGroup(group);
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Get tasks for process instance",
            description = "Retrieves all tasks for a specific process instance")
    @GetMapping("/tasks/process/{processInstanceId}")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER', 'MANAGER')")
    public ResponseEntity<List<TaskResponse>> getTasksByProcessInstance(
            @Parameter(description = "Process instance ID") @PathVariable String processInstanceId) {
        log.info("REST request to get tasks for process instance: {}", processInstanceId);
        List<TaskResponse> tasks = workflowService.getTasksByProcessInstance(processInstanceId);
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Get task by ID",
            description = "Retrieves detailed information about a specific task")
    @GetMapping("/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<TaskResponse> getTask(
            @Parameter(description = "Task ID") @PathVariable String taskId) {
        log.info("REST request to get task: {}", taskId);
        TaskResponse task = workflowService.getTask(taskId);
        return ResponseEntity.ok(task);
    }

    @Operation(summary = "Complete a task",
            description = "Completes a task with the provided variables and optional comment")
    @PostMapping("/tasks/complete")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<Void> completeTask(@RequestBody CompleteTaskRequest request) {
        log.info("REST request to complete task: {}", request.getTaskId());
        workflowService.completeTask(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Claim a task",
            description = "Claims a task for a specific user")
    @PostMapping("/tasks/{taskId}/claim")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'HR_MANAGER', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<Void> claimTask(
            @Parameter(description = "Task ID") @PathVariable String taskId,
            @Parameter(description = "User ID") @RequestParam String userId) {
        log.info("REST request to claim task: {} for user: {}", taskId, userId);
        workflowService.claimTask(taskId, userId);
        return ResponseEntity.ok().build();
    }
}
