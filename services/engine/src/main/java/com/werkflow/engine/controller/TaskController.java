package com.werkflow.engine.controller;

import com.werkflow.engine.dto.CompleteTaskRequest;
import com.werkflow.engine.dto.TaskResponse;
import com.werkflow.engine.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing user tasks
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "User task management")
@SecurityRequirement(name = "bearer-jwt")
public class TaskController {

    private final TaskService taskService;

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
