package com.werkflow.delegates.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Generic REST Service Delegate for making HTTP calls to external services
 *
 * Configurable via BPMN process variables:
 * - url: Target endpoint URL (required)
 * - method: HTTP method (GET, POST, PUT, DELETE, PATCH) - default: POST
 * - headers: Map of HTTP headers (optional)
 * - body: Request body object (optional, for POST/PUT/PATCH)
 * - responseVariable: Variable name to store response (default: "restResponse")
 * - timeoutSeconds: Request timeout in seconds (default: 30)
 *
 * Example BPMN configuration:
 * <serviceTask id="callHRService" flowable:delegateExpression="${restServiceDelegate}">
 *   <extensionElements>
 *     <flowable:field name="url">
 *       <flowable:string>http://hr-service:8082/api/employees</flowable:string>
 *     </flowable:field>
 *     <flowable:field name="method">
 *       <flowable:string>POST</flowable:string>
 *     </flowable:field>
 *     <flowable:field name="responseVariable">
 *       <flowable:string>employeeData</flowable:string>
 *     </flowable:field>
 *   </extensionElements>
 * </serviceTask>
 */
@Slf4j
@Component("restServiceDelegate")
public class RestServiceDelegate implements JavaDelegate {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public RestServiceDelegate(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing RestServiceDelegate for process: {}", execution.getProcessInstanceId());

        // Get configuration from process variables
        String url = getRequiredVariable(execution, "url");
        String method = getVariable(execution, "method", "POST");
        Map<String, String> headers = getVariable(execution, "headers", null);
        Object body = getVariable(execution, "body", null);
        String responseVariable = getVariable(execution, "responseVariable", "restResponse");
        Integer timeoutSeconds = getVariable(execution, "timeoutSeconds", 30);

        log.debug("REST call configuration: url={}, method={}, responseVariable={}",
            url, method, responseVariable);

        try {
            // Build request
            WebClient.RequestBodySpec requestSpec = webClient
                .method(HttpMethod.valueOf(method.toUpperCase()))
                .uri(url)
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    if (headers != null) {
                        headers.forEach(httpHeaders::add);
                    }
                });

            // Add body if present
            Mono<?> responseMono;
            if (body != null && (method.equalsIgnoreCase("POST") ||
                                 method.equalsIgnoreCase("PUT") ||
                                 method.equalsIgnoreCase("PATCH"))) {
                responseMono = requestSpec.bodyValue(body).retrieve().bodyToMono(Map.class);
            } else {
                responseMono = requestSpec.retrieve().bodyToMono(Map.class);
            }

            // Execute request and get response
            Map<String, Object> response = (Map<String, Object>) responseMono
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .block();

            // Store response in process variable
            execution.setVariable(responseVariable, response);

            log.info("REST call successful. Response stored in variable: {}", responseVariable);

        } catch (Exception e) {
            log.error("REST call failed: {}", e.getMessage(), e);

            // Store error information
            execution.setVariable(responseVariable + "Error", e.getMessage());
            execution.setVariable(responseVariable + "Success", false);

            throw new RuntimeException("REST service call failed: " + e.getMessage(), e);
        }
    }

    private String getRequiredVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);
        if (value == null) {
            throw new IllegalArgumentException("Required variable '" + variableName + "' is not set");
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private <T> T getVariable(DelegateExecution execution, String variableName, T defaultValue) {
        Object value = execution.getVariable(variableName);
        return value != null ? (T) value : defaultValue;
    }
}
