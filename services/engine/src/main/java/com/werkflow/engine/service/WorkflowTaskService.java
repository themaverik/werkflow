package com.werkflow.engine.service;

import com.werkflow.engine.dto.JwtUserContext;
import com.werkflow.engine.dto.TaskListResponse;
import com.werkflow.engine.dto.TaskQueryParams;
import com.werkflow.engine.dto.TaskResponse;
import com.werkflow.engine.exception.UnauthorizedTaskAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing workflow tasks with user context and authorization
 * Provides task list retrieval with pagination, filtering, and sorting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowTaskService {

    private final org.flowable.engine.TaskService flowableTaskService;
    private final RepositoryService repositoryService;

    /**
     * Get tasks assigned to the authenticated user
     * @param userContext User context from JWT
     * @param params Query parameters for filtering, sorting, and pagination
     * @return Paginated task list response
     */
    public TaskListResponse getMyTasks(JwtUserContext userContext, TaskQueryParams params) {
        log.debug("Fetching my tasks for user: {}", userContext.getUserId());

        // Build base query for user-assigned tasks
        TaskQuery query = flowableTaskService.createTaskQuery()
                .taskAssignee(userContext.getUserId())
                .active();

        // Apply filters
        query = applyFilters(query, params);

        // Apply sorting
        query = applySorting(query, params);

        // Execute query with pagination
        long totalCount = query.count();
        int offset = params.getPage() * params.getSize();
        List<Task> tasks = query.listPage(offset, params.getSize());

        log.info("Found {} my tasks (total: {}) for user: {}",
                tasks.size(), totalCount, userContext.getUserId());

        // Transform to DTOs
        List<TaskResponse> responses = tasks.stream()
                .map(this::mapToEnhancedResponse)
                .collect(Collectors.toList());

        // Build paginated response
        return buildTaskListResponse(responses, params, totalCount, "/workflows/tasks/my-tasks");
    }

    /**
     * Get tasks available to the user's groups (candidate tasks)
     * @param userContext User context from JWT
     * @param params Query parameters for filtering, sorting, and pagination
     * @return Paginated task list response
     */
    public TaskListResponse getGroupTasks(JwtUserContext userContext, TaskQueryParams params) {
        log.debug("Fetching group tasks for user: {} (groups: {})",
                userContext.getUserId(), userContext.getGroups());

        // Validate user has groups
        if (userContext.getGroups() == null || userContext.getGroups().isEmpty()) {
            log.warn("User {} has no groups, returning empty task list", userContext.getUserId());
            return buildEmptyTaskListResponse();
        }

        // Build base query
        TaskQuery query;

        // Filter by specific group if requested
        if (params.getGroupId() != null && !params.getGroupId().isEmpty()) {
            // Ensure user is in the requested group
            if (!userContext.isInGroup(params.getGroupId())) {
                throw new UnauthorizedTaskAccessException(
                        "User is not a member of group: " + params.getGroupId()
                );
            }
            query = flowableTaskService.createTaskQuery()
                    .taskCandidateGroup(params.getGroupId())
                    .active();
        } else {
            // Query all groups user belongs to
            query = flowableTaskService.createTaskQuery()
                    .taskCandidateGroupIn(userContext.getGroups())
                    .active();
        }

        // Optionally exclude assigned tasks (default behavior)
        if (!params.getIncludeAssigned()) {
            query.taskUnassigned();
        }

        // Apply filters
        query = applyFilters(query, params);

        // Apply sorting
        query = applySorting(query, params);

        // Execute query with pagination
        long totalCount = query.count();
        int offset = params.getPage() * params.getSize();
        List<Task> tasks = query.listPage(offset, params.getSize());

        log.info("Found {} group tasks (total: {}) for user: {} (groups: {})",
                tasks.size(), totalCount, userContext.getUserId(), userContext.getGroups());

        // Transform to DTOs
        List<TaskResponse> responses = tasks.stream()
                .map(this::mapToEnhancedResponse)
                .collect(Collectors.toList());

        // Build paginated response
        return buildTaskListResponse(responses, params, totalCount, "/workflows/tasks/group-tasks");
    }

    /**
     * Apply filters to task query
     * @param query Task query
     * @param params Query parameters
     * @return Filtered query
     */
    private TaskQuery applyFilters(TaskQuery query, TaskQueryParams params) {
        // Search filter (task name or description)
        if (params.getSearch() != null && !params.getSearch().isBlank()) {
            query.or()
                    .taskNameLikeIgnoreCase("%" + params.getSearch() + "%")
                    .taskDescriptionLikeIgnoreCase("%" + params.getSearch() + "%")
                    .endOr();
        }

        // Priority filter
        if (params.getPriority() != null) {
            query.taskPriority(params.getPriority());
        }

        // Process definition filter
        if (params.getProcessDefinitionKey() != null && !params.getProcessDefinitionKey().isEmpty()) {
            query.processDefinitionKey(params.getProcessDefinitionKey());
        }

        // Due date filters
        if (params.getDueBefore() != null) {
            query.taskDueBefore(Date.from(params.getDueBefore()));
        }
        if (params.getDueAfter() != null) {
            query.taskDueAfter(Date.from(params.getDueAfter()));
        }

        // Status filter (active vs suspended)
        if (params.getStatus() != null) {
            if (params.getStatus() == TaskQueryParams.TaskStatus.SUSPENDED) {
                query.suspended();
            }
            // Active is already applied by default in getMyTasks/getGroupTasks
        }

        return query;
    }

    /**
     * Apply sorting to task query
     * @param query Task query
     * @param params Query parameters
     * @return Sorted query
     */
    private TaskQuery applySorting(TaskQuery query, TaskQueryParams params) {
        String[] sortParts = params.getSort().split(",");
        String sortField = sortParts[0];
        boolean ascending = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]);

        switch (sortField) {
            case "name":
                query.orderByTaskName();
                break;
            case "priority":
                query.orderByTaskPriority();
                break;
            case "dueDate":
                query.orderByTaskDueDate();
                break;
            case "createTime":
            default:
                query.orderByTaskCreateTime();
        }

        if (ascending) {
            query.asc();
        } else {
            query.desc();
        }

        return query;
    }

    /**
     * Map Flowable Task to enhanced TaskResponse DTO
     * @param task Flowable task entity
     * @return TaskResponse DTO
     */
    private TaskResponse mapToEnhancedResponse(Task task) {
        // Get process definition details
        String processDefinitionKey = extractProcessDefinitionKey(task.getProcessDefinitionId());
        String processDefinitionName = getProcessDefinitionName(task.getProcessDefinitionId());

        // Get candidate groups (from identity links)
        List<String> candidateGroups = flowableTaskService.getIdentityLinksForTask(task.getId())
                .stream()
                .filter(link -> "candidate".equals(link.getType()) && link.getGroupId() != null)
                .map(IdentityLink::getGroupId)
                .collect(Collectors.toList());

        // Get candidate users
        List<String> candidateUsers = flowableTaskService.getIdentityLinksForTask(task.getId())
                .stream()
                .filter(link -> "candidate".equals(link.getType()) && link.getUserId() != null)
                .map(IdentityLink::getUserId)
                .collect(Collectors.toList());

        // Get task variables
        Map<String, Object> variables = flowableTaskService.getVariables(task.getId());

        // Calculate execution duration
        long executionDuration = task.getCreateTime() != null
                ? System.currentTimeMillis() - task.getCreateTime().getTime()
                : 0L;

        // Extract department from variables if available
        String department = variables.containsKey("department")
                ? String.valueOf(variables.get("department"))
                : null;

        return TaskResponse.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .processDefinitionKey(processDefinitionKey)
                .processDefinitionName(processDefinitionName)
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .assignee(task.getAssignee())
                .owner(task.getOwner())
                .priority(task.getPriority())
                .createTime(toInstant(task.getCreateTime()))
                .dueDate(toInstant(task.getDueDate()))
                .claimTime(toInstant(task.getClaimTime()))
                .suspended(task.isSuspended())
                .formKey(task.getFormKey())
                .category(task.getCategory())
                .tenantId(task.getTenantId())
                .candidateGroups(candidateGroups)
                .candidateUsers(candidateUsers)
                .executionDuration(executionDuration)
                .department(department)
                .variables(variables)
                .build();
    }

    /**
     * Get process definition name from cache
     * @param processDefinitionId Process definition ID
     * @return Process definition name
     */
    @Cacheable(value = "processDefinitionNames", key = "#processDefinitionId")
    public String getProcessDefinitionName(String processDefinitionId) {
        try {
            ProcessDefinition procDef = repositoryService
                    .createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            return procDef != null ? procDef.getName() : null;
        } catch (Exception e) {
            log.error("Error fetching process definition name for ID: {}", processDefinitionId, e);
            return null;
        }
    }

    /**
     * Extract process definition key from process definition ID
     * Format: "{key}:{version}:{id}"
     * @param processDefinitionId Process definition ID
     * @return Process definition key
     */
    private String extractProcessDefinitionKey(String processDefinitionId) {
        if (processDefinitionId == null) {
            return null;
        }
        String[] parts = processDefinitionId.split(":");
        return parts.length > 0 ? parts[0] : null;
    }

    /**
     * Build task list response with pagination
     * @param tasks List of task responses
     * @param params Query parameters
     * @param totalCount Total number of tasks
     * @param basePath Base path for pagination links
     * @return TaskListResponse
     */
    private TaskListResponse buildTaskListResponse(
            List<TaskResponse> tasks,
            TaskQueryParams params,
            long totalCount,
            String basePath) {

        int totalPages = (int) Math.ceil((double) totalCount / params.getSize());

        TaskListResponse.PageInfo pageInfo = TaskListResponse.PageInfo.builder()
                .size(params.getSize())
                .number(params.getPage())
                .totalElements(totalCount)
                .totalPages(totalPages)
                .build();

        Map<String, String> links = buildPaginationLinks(basePath, params, totalPages);

        return TaskListResponse.builder()
                .content(tasks)
                .page(pageInfo)
                .links(links)
                .build();
    }

    /**
     * Build HATEOAS pagination links
     * @param basePath Base path for links
     * @param params Query parameters
     * @param totalPages Total number of pages
     * @return Map of link relations to URLs
     */
    private Map<String, String> buildPaginationLinks(String basePath, TaskQueryParams params, int totalPages) {
        Map<String, String> links = new HashMap<>();

        String baseUrl = basePath + "?size=" + params.getSize();

        // Self link
        links.put("self", baseUrl + "&page=" + params.getPage());

        // First link
        links.put("first", baseUrl + "&page=0");

        // Previous link (if not on first page)
        if (params.getPage() > 0) {
            links.put("prev", baseUrl + "&page=" + (params.getPage() - 1));
        }

        // Next link (if not on last page)
        if (params.getPage() < totalPages - 1) {
            links.put("next", baseUrl + "&page=" + (params.getPage() + 1));
        }

        // Last link
        if (totalPages > 0) {
            links.put("last", baseUrl + "&page=" + (totalPages - 1));
        }

        return links;
    }

    /**
     * Build empty task list response
     * @return Empty TaskListResponse
     */
    private TaskListResponse buildEmptyTaskListResponse() {
        TaskListResponse.PageInfo pageInfo = TaskListResponse.PageInfo.builder()
                .size(0)
                .number(0)
                .totalElements(0)
                .totalPages(0)
                .build();

        return TaskListResponse.builder()
                .content(Collections.emptyList())
                .page(pageInfo)
                .links(Collections.emptyMap())
                .build();
    }

    /**
     * Convert Date to Instant
     * @param date Date to convert
     * @return Instant or null
     */
    private Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }
}
