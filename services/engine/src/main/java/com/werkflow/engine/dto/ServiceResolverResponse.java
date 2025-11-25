package com.werkflow.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for service URL resolution from service registry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResolverResponse {
    private String serviceName;
    private String environment;
    private String resolvedUrl;
    private String basePath;
    private String fullUrl;
}
