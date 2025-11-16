package com.werkflow.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for completing a workflow task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteTaskRequest {

    private String taskId;
    private Map<String, Object> variables;
    private String comment;
}
