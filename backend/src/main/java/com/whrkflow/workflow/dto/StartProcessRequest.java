package com.whrkflow.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for starting a new workflow process instance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartProcessRequest {

    private String processDefinitionKey;
    private String businessKey;
    private Map<String, Object> variables;
}
