package com.werkflow.procurement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Flowable Execution Listener to inject service URLs into process variables
 * This makes service URLs available to RestServiceDelegate in BPMN processes
 */
@Slf4j
@Component("processVariableInjector")
@RequiredArgsConstructor
public class ProcessVariableInjector implements ExecutionListener {

    private final ServiceUrlConfiguration serviceUrlConfiguration;

    @Override
    public void notify(DelegateExecution execution) {
        log.debug("Injecting service URLs into process instance: {}", execution.getProcessInstanceId());

        Map<String, String> serviceUrls = serviceUrlConfiguration.getServiceUrlMap();

        for (Map.Entry<String, String> entry : serviceUrls.entrySet()) {
            execution.setVariable(entry.getKey(), entry.getValue());
            log.trace("Injected variable: {} = {}", entry.getKey(), entry.getValue());
        }

        log.info("Successfully injected {} service URLs into process instance {}",
                serviceUrls.size(), execution.getProcessInstanceId());
    }
}
