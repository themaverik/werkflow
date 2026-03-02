package com.werkflow.engine.controller;

import com.werkflow.engine.dto.JwtUserContext;
import com.werkflow.engine.util.JwtClaimsExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for dashboard-level workflow queries.
 * Provides /workflows/instances, /workflows/activity, and /api/v1/tasks/summary
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class WorkflowDashboardController {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    /**
     * GET /workflows/instances — list all workflow instances (active + completed)
     */
    @GetMapping("/workflows/instances")
    public ResponseEntity<List<Map<String, Object>>> getWorkflowInstances(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {

        log.info("GET /workflows/instances - status={}, limit={}", status, limit);

        List<Map<String, Object>> results = new ArrayList<>();

        if (status == null || "active".equals(status)) {
            List<ProcessInstance> active = runtimeService.createProcessInstanceQuery()
                    .orderByProcessInstanceId().desc()
                    .listPage(0, limit);
            for (ProcessInstance pi : active) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", pi.getId());
                entry.put("processDefinitionKey", pi.getProcessDefinitionKey());
                entry.put("processDefinitionName", pi.getProcessDefinitionName());
                entry.put("businessKey", pi.getBusinessKey());
                entry.put("startTime", pi.getStartTime() != null ? pi.getStartTime().toInstant().toString() : null);
                entry.put("startedBy", pi.getStartUserId());
                entry.put("status", pi.isSuspended() ? "suspended" : "active");
                entry.put("department", runtimeService.getVariable(pi.getId(), "department"));
                results.add(entry);
            }
        }

        if (status == null || "completed".equals(status) || "failed".equals(status)) {
            List<HistoricProcessInstance> finished = historyService.createHistoricProcessInstanceQuery()
                    .finished()
                    .orderByProcessInstanceEndTime().desc()
                    .listPage(0, limit);
            for (HistoricProcessInstance hpi : finished) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", hpi.getId());
                entry.put("processDefinitionKey", hpi.getProcessDefinitionKey());
                entry.put("processDefinitionName", hpi.getProcessDefinitionName());
                entry.put("businessKey", hpi.getBusinessKey());
                entry.put("startTime", hpi.getStartTime() != null ? hpi.getStartTime().toInstant().toString() : null);
                entry.put("endTime", hpi.getEndTime() != null ? hpi.getEndTime().toInstant().toString() : null);
                entry.put("startedBy", hpi.getStartUserId());
                entry.put("status", hpi.getDeleteReason() != null ? "failed" : "completed");
                results.add(entry);
            }
        }

        return ResponseEntity.ok(results);
    }

    /**
     * GET /workflows/activity — recent activity log from historic activity instances
     */
    @GetMapping("/workflows/activity")
    public ResponseEntity<List<Map<String, Object>>> getActivityLog(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "50") int limit) {

        log.info("GET /workflows/activity - limit={}", limit);

        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .orderByHistoricActivityInstanceEndTime().desc()
                .finished()
                .listPage(0, limit);

        List<Map<String, Object>> results = activities.stream().map(a -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", a.getId());
            entry.put("type", mapActivityType(a.getActivityType()));
            entry.put("message", buildActivityMessage(a));
            entry.put("timestamp", a.getEndTime() != null ? a.getEndTime().toInstant().toString() : null);
            entry.put("user", a.getAssignee() != null ? a.getAssignee() : "system");
            return entry;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/v1/tasks/summary — task counts for dashboard
     */
    @GetMapping("/api/v1/tasks/summary")
    public ResponseEntity<Map<String, Object>> getTaskSummary(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("preferred_username");
        log.info("GET /api/v1/tasks/summary - user={}", userId);

        long myTasks = taskService.createTaskQuery().taskAssignee(userId).count();
        long teamTasks = taskService.createTaskQuery().taskCandidateUser(userId).count();
        long unassigned = taskService.createTaskQuery().taskUnassigned().count();
        long overdue = taskService.createTaskQuery().taskAssignee(userId)
                .taskDueBefore(new Date()).count();
        long highPriority = taskService.createTaskQuery().taskAssignee(userId)
                .taskMinPriority(75).count();

        // dueToday: tasks due between now and end of day
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        long dueToday = taskService.createTaskQuery().taskAssignee(userId)
                .taskDueBefore(endOfDay.getTime()).taskDueAfter(new Date()).count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", myTasks + teamTasks);
        summary.put("myTasks", myTasks);
        summary.put("teamTasks", teamTasks);
        summary.put("unassigned", unassigned);
        summary.put("overdue", overdue);
        summary.put("dueToday", dueToday);
        summary.put("highPriority", highPriority);

        return ResponseEntity.ok(summary);
    }

    private String mapActivityType(String flowableType) {
        if (flowableType == null) return "started";
        return switch (flowableType) {
            case "startEvent" -> "started";
            case "endEvent" -> "completed";
            case "userTask" -> "completed";
            case "serviceTask" -> "completed";
            case "boundaryEvent", "errorEndEvent" -> "failed";
            default -> "started";
        };
    }

    private String buildActivityMessage(HistoricActivityInstance a) {
        String name = a.getActivityName() != null ? a.getActivityName() : a.getActivityId();
        return switch (a.getActivityType()) {
            case "startEvent" -> "Process started";
            case "endEvent" -> "Process completed";
            case "userTask" -> "Task '" + name + "' completed";
            case "serviceTask" -> "Service task '" + name + "' executed";
            default -> "Activity '" + name + "' (" + a.getActivityType() + ")";
        };
    }
}
