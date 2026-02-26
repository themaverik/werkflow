package com.werkflow.delegates.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.Expression;
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
 * Supports TWO configuration modes for maximum flexibility:
 *
 * MODE 1: Field Injection (RECOMMENDED - Type-safe, Clean BPMN)
 * Use flowable:field tags with Expression support. Fields are injected at deployment and evaluated at runtime.
 *
 * MODE 2: Process Variables (Legacy - Backward compatible)
 * Use execution.setVariable() in script tasks or previous delegates.
 *
 * Configuration Parameters:
 * - url: Target endpoint URL (required)
 * - method: HTTP method (GET, POST, PUT, DELETE, PATCH) - default: POST
 * - headers: Map of HTTP headers (optional)
 * - body: Request body object (optional, for POST/PUT/PATCH)
 * - responseVariable: Variable name to store response (default: "restResponse")
 * - timeoutSeconds: Request timeout in seconds (default: 30)
 *
 * Example MODE 1 - Field Injection (RECOMMENDED):
 * <serviceTask id="createCapEx" flowable:delegateExpression="${restServiceDelegate}">
 *   <extensionElements>
 *     <flowable:field name="url">
 *       <flowable:expression>${financeServiceUrl}/api/workflow/capex/create-request</flowable:expression>
 *     </flowable:field>
 *     <flowable:field name="method">
 *       <flowable:string>POST</flowable:string>
 *     </flowable:field>
 *     <flowable:field name="body">
 *       <flowable:expression>#{
 *         {
 *           'title': title,
 *           'amount': requestAmount,
 *           'department': departmentName
 *         }
 *       }</flowable:expression>
 *     </flowable:field>
 *     <flowable:field name="responseVariable">
 *       <flowable:string>createRequestResponse</flowable:string>
 *     </flowable:field>
 *   </extensionElements>
 * </serviceTask>
 *
 * Example MODE 2 - Process Variables (Legacy):
 * Use a script task before the service task:
 * <scriptTask scriptFormat="groovy">
 *   execution.setVariable('url', 'http://service/api')
 *   execution.setVariable('body', [title: title, amount: amount])
 * </scriptTask>
 * <serviceTask flowable:delegateExpression="${restServiceDelegate}" />
 */
@Slf4j
@Component("restServiceDelegate")
public class RestServiceDelegate implements JavaDelegate {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Field injection support (MODE 1 - RECOMMENDED)
    private Expression url;
    private Expression method;
    private Expression headers;
    private Expression body;
    private Expression responseVariable;
    private Expression timeoutSeconds;

    public RestServiceDelegate(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing RestServiceDelegate for process: {}", execution.getProcessInstanceId());

        // Get configuration - Fields take priority over variables for explicit control
        String urlValue = getFieldOrVariable(execution, this.url, "url", null);
        if (urlValue == null) {
            throw new IllegalArgumentException("Required field/variable 'url' is not set");
        }

        String methodValue = getFieldOrVariable(execution, this.method, "method", "POST");
        Map<String, String> headersValue = getFieldOrVariable(execution, this.headers, "headers", null);
        Object bodyValue = getFieldOrVariable(execution, this.body, "body", null);
        String responseVariableValue = getFieldOrVariable(execution, this.responseVariable, "responseVariable", "restResponse");
        Integer timeoutSecondsValue = getFieldOrVariable(execution, this.timeoutSeconds, "timeoutSeconds", 30);

        log.debug("REST call configuration: url={}, method={}, responseVariable={}",
            urlValue, methodValue, responseVariableValue);

        try {
            // Build request
            WebClient.RequestBodySpec requestSpec = webClient
                .method(HttpMethod.valueOf(methodValue.toUpperCase()))
                .uri(urlValue)
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    if (headersValue != null) {
                        headersValue.forEach(httpHeaders::add);
                    }
                });

            // Add body if present
            Mono<?> responseMono;
            if (bodyValue != null && (methodValue.equalsIgnoreCase("POST") ||
                                 methodValue.equalsIgnoreCase("PUT") ||
                                 methodValue.equalsIgnoreCase("PATCH"))) {
                responseMono = requestSpec.bodyValue(bodyValue).retrieve().bodyToMono(Map.class);
            } else {
                responseMono = requestSpec.retrieve().bodyToMono(Map.class);
            }

            // Execute request and get response
            Map<String, Object> response = (Map<String, Object>) responseMono
                .timeout(java.time.Duration.ofSeconds(timeoutSecondsValue))
                .block();

            // Store response in process variable
            execution.setVariable(responseVariableValue, response);

            log.info("REST call successful. Response stored in variable: {}", responseVariableValue);

        } catch (Exception e) {
            log.error("REST call failed: {}", e.getMessage(), e);

            // Store error information
            execution.setVariable(responseVariableValue + "Error", e.getMessage());
            execution.setVariable(responseVariableValue + "Success", false);

            throw new RuntimeException("REST service call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get value from Expression field first, fall back to process variable
     * This provides flexibility and backward compatibility
     */
    @SuppressWarnings("unchecked")
    private <T> T getFieldOrVariable(DelegateExecution execution, Expression field, String variableName, T defaultValue) {
        Object value = null;

        // Try field first (MODE 1 - Field Injection)
        if (field != null) {
            value = field.getValue(execution);
            log.trace("Field '{}' resolved to: {}", variableName, value);
        }

        // Fall back to variable (MODE 2 - Process Variables)
        if (value == null) {
            value = execution.getVariable(variableName);
            if (value != null) {
                log.trace("Variable '{}' resolved to: {}", variableName, value);
            }
        }

        return value != null ? (T) value : defaultValue;
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
