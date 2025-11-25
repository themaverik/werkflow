package com.werkflow.engine.service;

import com.werkflow.engine.dto.ServiceResolverResponse;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for injecting service URLs as process variables.
 * Resolves service URLs from the service registry and injects them into workflow instances.
 *
 * Uses @Lazy on RuntimeService to help prevent circular dependency issues during
 * Spring bean initialization. The RuntimeService is only retrieved when actually needed.
 */
@Service
@Slf4j
public class ProcessVariableInjector {

    private final RuntimeService runtimeService;
    private final RestTemplate restTemplate;

    /**
     * Constructor with @Lazy RuntimeService injection to avoid circular dependencies.
     * The RuntimeService bean will be lazily initialized when first accessed.
     */
    public ProcessVariableInjector(@Lazy RuntimeService runtimeService, RestTemplate restTemplate) {
        this.runtimeService = runtimeService;
        this.restTemplate = restTemplate;
    }

    @Value("${app.environment:development}")
    private String environment;

    @Value("${app.services.admin-url:http://localhost:8083}")
    private String adminServiceUrl;

    @Value("${app.services.hr-url:http://localhost:8082}")
    private String hrServiceFallbackUrl;

    @Value("${app.services.finance-url:http://localhost:8084}")
    private String financeServiceFallbackUrl;

    @Value("${app.services.procurement-url:http://localhost:8085}")
    private String procurementServiceFallbackUrl;

    @Value("${app.services.inventory-url:http://localhost:8086}")
    private String inventoryServiceFallbackUrl;

    @Value("${app.serviceRegistry.enabled:true}")
    private boolean serviceRegistryEnabled;

    /**
     * Inject service URLs as process variables for a given process instance
     * This method is called when a process starts
     *
     * @param processInstanceId The process instance ID
     */
    public void injectServiceUrls(String processInstanceId) {
        log.info("Injecting service URLs for process instance: {}", processInstanceId);

        Map<String, Object> variables = new HashMap<>();

        // List of services to inject
        String[] services = {"hr-service", "finance-service", "procurement-service", "inventory-service", "admin-service"};

        for (String serviceName : services) {
            try {
                String serviceUrl = resolveServiceUrl(serviceName, environment);
                String variableName = serviceName.replace("-", "_") + "_url";
                variables.put(variableName, serviceUrl);
                log.debug("Injected variable: {} = {}", variableName, serviceUrl);
            } catch (Exception e) {
                log.warn("Failed to resolve URL for service: {}, using fallback", serviceName, e);
                String fallbackUrl = getFallbackUrl(serviceName);
                String variableName = serviceName.replace("-", "_") + "_url";
                variables.put(variableName, fallbackUrl);
                log.debug("Injected fallback variable: {} = {}", variableName, fallbackUrl);
            }
        }

        // Set all variables at once
        if (!variables.isEmpty()) {
            runtimeService.setVariables(processInstanceId, variables);
            log.info("Successfully injected {} service URL variables for process instance: {}",
                    variables.size(), processInstanceId);
        }
    }

    /**
     * Resolve service URL from service registry with caching
     * Cache key: serviceName:environment
     *
     * @param serviceName The service name
     * @param environment The environment
     * @return The resolved service URL
     */
    @Cacheable(value = "serviceUrls", key = "#serviceName + ':' + #environment")
    public String resolveServiceUrl(String serviceName, String environment) {
        log.debug("Resolving URL for service: {} in environment: {}", serviceName, environment);

        if (!serviceRegistryEnabled) {
            log.debug("Service registry is disabled, using fallback URLs");
            return getFallbackUrl(serviceName);
        }

        try {
            String resolverUrl = String.format("%s/api/services/resolve/%s?environment=%s",
                    adminServiceUrl, serviceName, environment);

            log.debug("Calling service registry: {}", resolverUrl);

            ServiceResolverResponse response = restTemplate.getForObject(
                    resolverUrl, ServiceResolverResponse.class);

            if (response != null && response.getFullUrl() != null) {
                log.debug("Resolved URL for {}: {}", serviceName, response.getFullUrl());
                return response.getFullUrl();
            } else {
                log.warn("Service registry returned null response for {}, using fallback", serviceName);
                return getFallbackUrl(serviceName);
            }
        } catch (Exception e) {
            log.error("Error calling service registry for {}: {}", serviceName, e.getMessage());
            return getFallbackUrl(serviceName);
        }
    }

    /**
     * Get fallback URL from application.yml configuration
     *
     * @param serviceName The service name
     * @return The fallback URL
     */
    public String getFallbackUrl(String serviceName) {
        return switch (serviceName) {
            case "hr-service" -> hrServiceFallbackUrl;
            case "finance-service" -> financeServiceFallbackUrl;
            case "procurement-service" -> procurementServiceFallbackUrl;
            case "inventory-service" -> inventoryServiceFallbackUrl;
            case "admin-service" -> adminServiceUrl;
            default -> {
                log.warn("No fallback URL configured for service: {}", serviceName);
                yield "http://localhost:8080";
            }
        };
    }

    /**
     * Clear the cache for a specific service URL
     * Useful for testing or when service URLs change
     *
     * @param serviceName The service name
     */
    public void clearCache(String serviceName) {
        log.info("Clearing cache for service: {}", serviceName);
        // Cache is automatically cleared after 30 seconds
        // This method is here for future manual cache invalidation if needed
    }
}
