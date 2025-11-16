package com.werkflow.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for workflow process instance information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessInstanceResponse {

    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String businessKey;
    private boolean suspended;
    private boolean ended;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Map<String, Object> variables;
}
