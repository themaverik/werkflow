package com.whrkflow.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for workflow task information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private String taskId;
    private String taskName;
    private String taskDefinitionKey;
    private String processInstanceId;
    private String processDefinitionId;
    private String assignee;
    private String owner;
    private LocalDateTime createTime;
    private LocalDateTime dueDate;
    private int priority;
    private boolean suspended;
    private String description;
    private Map<String, Object> variables;
}
