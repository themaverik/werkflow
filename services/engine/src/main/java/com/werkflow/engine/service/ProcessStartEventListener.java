package com.werkflow.engine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.ProcessEngine;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Flowable event listener that injects service URLs when a process starts.
 * Listens for PROCESS_STARTED events and calls ProcessVariableInjector.
 *
 * This listener registers itself programmatically after ApplicationReadyEvent
 * to avoid circular dependency issues during Spring bean initialization.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessStartEventListener implements FlowableEventListener {

    private final ProcessVariableInjector processVariableInjector;
    private final ProcessEngine processEngine;

    /**
     * Registers this event listener with the Flowable process engine after
     * the application context is fully initialized.
     *
     * This approach avoids circular dependency issues:
     * - FlowableConfig needs this listener
     * - This listener needs ProcessVariableInjector
     * - ProcessVariableInjector needs RuntimeService
     * - RuntimeService needs FlowableConfig to be initialized
     *
     * By deferring registration until ApplicationReadyEvent, we ensure all
     * beans are fully initialized before the listener is registered.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerEventListener() {
        log.info("Registering ProcessStartEventListener with Flowable engine");
        try {
            processEngine.getRuntimeService()
                    .addEventListener(this);
            log.info("Successfully registered ProcessStartEventListener");
        } catch (Exception e) {
            log.error("Failed to register ProcessStartEventListener", e);
            throw new RuntimeException("Failed to register process event listener", e);
        }
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (event.getType() == FlowableEngineEventType.PROCESS_STARTED) {
            try {
                // Cast to FlowableEngineEntityEvent to access process instance details
                if (event instanceof FlowableEngineEntityEvent) {
                    FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) event;
                    String processInstanceId = entityEvent.getExecutionId();
                    String processDefinitionId = entityEvent.getProcessDefinitionId();

                    log.info("Process started - Definition ID: {}, Instance ID: {}",
                            processDefinitionId, processInstanceId);

                    // Inject service URLs as process variables
                    processVariableInjector.injectServiceUrls(processInstanceId);

                    log.info("Successfully injected service URLs for process instance: {}", processInstanceId);
                }
            } catch (Exception e) {
                log.error("Error injecting service URLs for process: {}", event, e);
                // Don't throw exception - we don't want to break the process start
            }
        }
    }

    @Override
    public boolean isFailOnException() {
        // Return false to prevent process start failure if variable injection fails
        return false;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        // Execute after transaction commits
        return false;
    }

    @Override
    public String getOnTransaction() {
        // Execute immediately, not tied to transaction lifecycle
        return null;
    }
}
