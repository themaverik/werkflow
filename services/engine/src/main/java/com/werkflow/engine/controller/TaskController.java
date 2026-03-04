package com.werkflow.engine.controller;

import com.werkflow.engine.dto.CompleteTaskRequest;
import com.werkflow.engine.dto.TaskResponse;
import com.werkflow.engine.service.TaskService;
import org.flowable.engine.HistoryService;
import org.flowable.task.api.history.HistoricTaskInstance;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing user tasks
 */
@RestController
@RequestMapping({"/api/tasks", "/api/v1/tasks"})
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "User task management")
@SecurityRequirement(name = "bearer-jwt")
public class TaskController {

    private final TaskService taskService;
    private final HistoryService historyService;

    @GetMapping
    @Operation(summary = "List tasks for current user (assigned + candidate)")
    public ResponseEntity<Map<String, Object>> listTasks(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "0") int start,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createTime") String sort,
        @RequestParam(defaultValue = "desc") String order,
        @RequestParam(required = false) String assignee,
        @RequestParam(required = false) String candidateUser,
        @RequestParam(required = false) String candidateGroups,
        @RequestParam(required = false) Boolean unassigned
    ) {
        String userId = jwt.getClaimAsString("preferred_username");
        List<TaskResponse> tasks;

        if (assignee != null && !assignee.isEmpty()) {
            // My Tasks: assigned to specific user
            tasks = taskService.getTasksForUser(assignee);
        } else if (candidateGroups != null && !candidateGroups.isEmpty()) {
            // Team/Group Tasks: candidate group query
            List<String> groups = List.of(candidateGroups.split(","));
            tasks = taskService.getTasksForCandidateGroups(groups);
        } else if (unassigned != null && unassigned) {
            // Unassigned tasks
            tasks = taskService.getUnassignedTasks();
        } else {
            // Default: assigned to user + candidate tasks for user's groups
            List<String> userGroups = jwt.getClaimAsStringList("groups");
            tasks = taskService.getTasksForUserOrGroups(userId, userGroups);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", tasks);
        result.put("total", tasks.size());
        result.put("start", start);
        result.put("size", size);
        result.put("sort", sort);
        result.put("order", order);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-tasks")
    @Operation(summary = "Get tasks assigned to current user")
    public ResponseEntity<List<TaskResponse>> getMyTasks(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("preferred_username");
        List<TaskResponse> responses = taskService.getTasksForUser(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get tasks for a group/role")
    public ResponseEntity<List<TaskResponse>> getTasksForGroup(
        @Parameter(description = "Group/Role ID") @PathVariable String groupId
    ) {
        List<TaskResponse> responses = taskService.getTasksForGroup(groupId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<TaskResponse> getTaskById(
        @Parameter(description = "Task ID") @PathVariable String id
    ) {
        TaskResponse response = taskService.getTaskById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/process-instance/{processInstanceId}")
    @Operation(summary = "Get tasks for a process instance")
    public ResponseEntity<List<TaskResponse>> getTasksByProcessInstanceId(
        @Parameter(description = "Process instance ID") @PathVariable String processInstanceId
    ) {
        List<TaskResponse> responses = taskService.getTasksByProcessInstanceId(processInstanceId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/claim")
    @Operation(summary = "Claim a task")
    public ResponseEntity<Void> claimTask(
        @Parameter(description = "Task ID") @PathVariable String id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("preferred_username");
        taskService.claimTask(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unclaim")
    @Operation(summary = "Unclaim a task")
    public ResponseEntity<Void> unclaimTask(
        @Parameter(description = "Task ID") @PathVariable String id
    ) {
        taskService.unclaimTask(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign a task to a user")
    public ResponseEntity<Void> assignTask(
        @Parameter(description = "Task ID") @PathVariable String id,
        @Parameter(description = "User ID to assign") @RequestParam String userId
    ) {
        taskService.assignTask(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a task")
    public ResponseEntity<Void> completeTask(
        @Parameter(description = "Task ID") @PathVariable String id,
        @RequestBody(required = false) CompleteTaskRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("preferred_username");
        CompleteTaskRequest finalRequest = request != null ? request : new CompleteTaskRequest();
        taskService.completeTask(id, finalRequest, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/variables")
    @Operation(summary = "Get task variables")
    public ResponseEntity<Map<String, Object>> getTaskVariables(
        @Parameter(description = "Task ID") @PathVariable String id
    ) {
        Map<String, Object> variables = taskService.getTaskVariables(id);
        return ResponseEntity.ok(variables);
    }

    @PutMapping("/{id}/variables")
    @Operation(summary = "Set task variables")
    public ResponseEntity<Void> setTaskVariables(
        @Parameter(description = "Task ID") @PathVariable String id,
        @RequestBody Map<String, Object> variables
    ) {
        taskService.setTaskVariables(id, variables);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get task history (audit trail)")
    public ResponseEntity<List<Map<String, Object>>> getTaskHistory(
        @Parameter(description = "Task ID") @PathVariable String id
    ) {
        List<Map<String, Object>> history = new java.util.ArrayList<>();

        // Get historic task instances related to this task's process instance
        // First try to find the task (active or historic) to get the process instance ID
        String processInstanceId = null;

        // Check active tasks
        org.flowable.task.api.Task activeTask = taskService.findActiveTask(id);
        if (activeTask != null) {
            processInstanceId = activeTask.getProcessInstanceId();
        } else {
            // Check historic tasks
            HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery()
                    .taskId(id)
                    .singleResult();
            if (historicTask != null) {
                processInstanceId = historicTask.getProcessInstanceId();
            }
        }

        if (processInstanceId != null) {
            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricTaskInstanceStartTime().asc()
                    .list();

            for (HistoricTaskInstance task : tasks) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", task.getId());
                entry.put("taskId", task.getId());
                entry.put("action", task.getEndTime() != null ? "completed" : "active");
                entry.put("userId", task.getAssignee());
                entry.put("taskName", task.getName());
                entry.put("timestamp", task.getEndTime() != null ?
                        task.getEndTime().toInstant().toString() :
                        task.getStartTime().toInstant().toString());
                entry.put("startTime", task.getStartTime() != null ?
                        task.getStartTime().toInstant().toString() : null);
                entry.put("endTime", task.getEndTime() != null ?
                        task.getEndTime().toInstant().toString() : null);
                history.add(entry);
            }
        }

        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to a task")
    public ResponseEntity<Void> addComment(
        @Parameter(description = "Task ID") @PathVariable String id,
        @Parameter(description = "Process instance ID") @RequestParam String processInstanceId,
        @Parameter(description = "Comment message") @RequestBody String message
    ) {
        taskService.addComment(id, processInstanceId, message);
        return ResponseEntity.noContent().build();
    }
}
