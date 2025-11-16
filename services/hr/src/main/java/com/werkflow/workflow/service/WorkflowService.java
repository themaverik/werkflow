package com.werkflow.workflow.service;

import com.werkflow.workflow.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing Flowable BPM workflows.
 * Provides operations for starting processes, managing tasks, and querying workflow state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    /**
     * Start a new process instance.
     *
     * @param request Process start request containing process key, business key, and variables
     * @return Process instance response with process details
     */
    @Transactional
    public ProcessInstanceResponse startProcess(StartProcessRequest request) {
        log.info("Starting process: {} with business key: {}",
                request.getProcessDefinitionKey(), request.getBusinessKey());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                request.getProcessDefinitionKey(),
                request.getBusinessKey(),
                request.getVariables() != null ? request.getVariables() : new HashMap<>()
        );

        log.info("Process instance started: {}", processInstance.getProcessInstanceId());

        return convertToProcessInstanceResponse(processInstance);
    }

    /**
     * Get a process instance by ID.
     *
     * @param processInstanceId Process instance ID
     * @return Process instance response
     */
    public ProcessInstanceResponse getProcessInstance(String processInstanceId) {
        log.debug("Fetching process instance: {}", processInstanceId);

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance != null) {
            return convertToProcessInstanceResponse(processInstance);
        }

        // Check history if not in active instances
        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (historicInstance == null) {
            throw new IllegalArgumentException("Process instance not found: " + processInstanceId);
        }

        return convertToHistoricProcessInstanceResponse(historicInstance);
    }

    /**
     * Get all active process instances for a process definition.
     *
     * @param processDefinitionKey Process definition key
     * @return List of process instance responses
     */
    public List<ProcessInstanceResponse> getProcessInstances(String processDefinitionKey) {
        log.debug("Fetching process instances for: {}", processDefinitionKey);

        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .list();

        return processInstances.stream()
                .map(this::convertToProcessInstanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get tasks assigned to a user.
     *
     * @param assignee User ID
     * @return List of task responses
     */
    public List<TaskResponse> getTasksByAssignee(String assignee) {
        log.debug("Fetching tasks for assignee: {}", assignee);

        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .list();

        return tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get tasks for a candidate group.
     *
     * @param group Group name
     * @return List of task responses
     */
    public List<TaskResponse> getTasksByGroup(String group) {
        log.debug("Fetching tasks for group: {}", group);

        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateGroup(group)
                .list();

        return tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all tasks for a process instance.
     *
     * @param processInstanceId Process instance ID
     * @return List of task responses
     */
    public List<TaskResponse> getTasksByProcessInstance(String processInstanceId) {
        log.debug("Fetching tasks for process instance: {}", processInstanceId);

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();

        return tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific task by ID.
     *
     * @param taskId Task ID
     * @return Task response
     */
    public TaskResponse getTask(String taskId) {
        log.debug("Fetching task: {}", taskId);

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        return convertToTaskResponse(task);
    }

    /**
     * Complete a task.
     *
     * @param request Complete task request containing task ID and variables
     */
    @Transactional
    public void completeTask(CompleteTaskRequest request) {
        log.info("Completing task: {}", request.getTaskId());

        Task task = taskService.createTaskQuery()
                .taskId(request.getTaskId())
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + request.getTaskId());
        }

        // Add comment if provided
        if (request.getComment() != null && !request.getComment().isEmpty()) {
            taskService.addComment(request.getTaskId(), task.getProcessInstanceId(), request.getComment());
        }

        // Complete the task with variables
        taskService.complete(
                request.getTaskId(),
                request.getVariables() != null ? request.getVariables() : new HashMap<>()
        );

        log.info("Task completed: {}", request.getTaskId());
    }

    /**
     * Claim a task for a user.
     *
     * @param taskId Task ID
     * @param userId User ID
     */
    @Transactional
    public void claimTask(String taskId, String userId) {
        log.info("Claiming task: {} for user: {}", taskId, userId);

        taskService.claim(taskId, userId);

        log.info("Task claimed successfully");
    }

    /**
     * Delete a process instance.
     *
     * @param processInstanceId Process instance ID
     * @param deleteReason Reason for deletion
     */
    @Transactional
    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        log.info("Deleting process instance: {} with reason: {}", processInstanceId, deleteReason);

        runtimeService.deleteProcessInstance(processInstanceId, deleteReason);

        log.info("Process instance deleted successfully");
    }

    /**
     * Get process variables.
     *
     * @param processInstanceId Process instance ID
     * @return Map of variables
     */
    public Map<String, Object> getProcessVariables(String processInstanceId) {
        log.debug("Fetching variables for process instance: {}", processInstanceId);

        return runtimeService.getVariables(processInstanceId);
    }

    /**
     * Set process variables.
     *
     * @param processInstanceId Process instance ID
     * @param variables Variables to set
     */
    @Transactional
    public void setProcessVariables(String processInstanceId, Map<String, Object> variables) {
        log.info("Setting variables for process instance: {}", processInstanceId);

        runtimeService.setVariables(processInstanceId, variables);

        log.info("Variables set successfully");
    }

    // Helper methods for converting Flowable objects to DTOs

    private ProcessInstanceResponse convertToProcessInstanceResponse(ProcessInstance processInstance) {
        Map<String, Object> variables = runtimeService.getVariables(processInstance.getProcessInstanceId());

        return ProcessInstanceResponse.builder()
                .processInstanceId(processInstance.getProcessInstanceId())
                .processDefinitionId(processInstance.getProcessDefinitionId())
                .processDefinitionKey(processInstance.getProcessDefinitionKey())
                .businessKey(processInstance.getBusinessKey())
                .suspended(processInstance.isSuspended())
                .ended(processInstance.isEnded())
                .startTime(processInstance.getStartTime() != null ?
                        processInstance.getStartTime().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime() : null)
                .variables(variables)
                .build();
    }

    private ProcessInstanceResponse convertToHistoricProcessInstanceResponse(HistoricProcessInstance historicInstance) {
        return ProcessInstanceResponse.builder()
                .processInstanceId(historicInstance.getId())
                .processDefinitionId(historicInstance.getProcessDefinitionId())
                .processDefinitionKey(historicInstance.getProcessDefinitionKey())
                .businessKey(historicInstance.getBusinessKey())
                .suspended(false)
                .ended(historicInstance.getEndTime() != null)
                .startTime(historicInstance.getStartTime() != null ?
                        historicInstance.getStartTime().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime() : null)
                .endTime(historicInstance.getEndTime() != null ?
                        historicInstance.getEndTime().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime() : null)
                .variables(new HashMap<>())
                .build();
    }

    private TaskResponse convertToTaskResponse(Task task) {
        Map<String, Object> variables = taskService.getVariables(task.getId());

        return TaskResponse.builder()
                .taskId(task.getId())
                .taskName(task.getName())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .createTime(task.getCreateTime() != null ?
                        task.getCreateTime().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime() : null)
                .dueDate(task.getDueDate() != null ?
                        task.getDueDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime() : null)
                .priority(task.getPriority())
                .suspended(task.isSuspended())
                .description(task.getDescription())
                .variables(variables)
                .build();
    }
}
