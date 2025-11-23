package com.werkflow.engine.service;

import com.werkflow.engine.dto.ProcessDefinitionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing BPMN process definitions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessDefinitionService {

    private final RepositoryService repositoryService;

    /**
     * Deploy a new process definition from BPMN XML file
     */
    @Transactional
    public ProcessDefinitionResponse deployProcessDefinition(MultipartFile file) {
        log.info("Deploying process definition from file: {}", file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            Deployment deployment = repositoryService.createDeployment()
                .name(file.getOriginalFilename())
                .addInputStream(file.getOriginalFilename(), inputStream)
                .deploy();

            log.info("Process definition deployed successfully. Deployment ID: {}", deployment.getId());

            // Get the deployed process definition
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

            return mapToResponse(processDefinition);

        } catch (IOException e) {
            log.error("Error deploying process definition", e);
            throw new RuntimeException("Failed to deploy process definition: " + e.getMessage(), e);
        }
    }

    /**
     * Get all process definitions
     */
    public List<ProcessDefinitionResponse> getAllProcessDefinitions() {
        log.debug("Fetching all process definitions");

        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
            .latestVersion()
            .list();

        return definitions.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get process definition by ID
     */
    public ProcessDefinitionResponse getProcessDefinitionById(String id) {
        log.debug("Fetching process definition by ID: {}", id);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionId(id)
            .singleResult();

        if (processDefinition == null) {
            throw new RuntimeException("Process definition not found with ID: " + id);
        }

        return mapToResponse(processDefinition);
    }

    /**
     * Get process definition by key (latest version)
     */
    public ProcessDefinitionResponse getProcessDefinitionByKey(String key) {
        log.debug("Fetching process definition by key: {}", key);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(key)
            .latestVersion()
            .singleResult();

        if (processDefinition == null) {
            throw new RuntimeException("Process definition not found with key: " + key);
        }

        return mapToResponse(processDefinition);
    }

    /**
     * Get all versions of a process definition by key
     */
    public List<ProcessDefinitionResponse> getProcessDefinitionVersions(String key) {
        log.debug("Fetching all versions of process definition: {}", key);

        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(key)
            .orderByProcessDefinitionVersion()
            .desc()
            .list();

        return definitions.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Delete process definition by deployment ID
     */
    @Transactional
    public void deleteProcessDefinition(String deploymentId, boolean cascade) {
        log.info("Deleting process definition deployment: {} (cascade: {})", deploymentId, cascade);

        repositoryService.deleteDeployment(deploymentId, cascade);

        log.info("Process definition deployment deleted successfully");
    }

    /**
     * Suspend process definition
     */
    @Transactional
    public void suspendProcessDefinition(String processDefinitionId) {
        log.info("Suspending process definition: {}", processDefinitionId);

        repositoryService.suspendProcessDefinitionById(processDefinitionId);

        log.info("Process definition suspended successfully");
    }

    /**
     * Activate process definition
     */
    @Transactional
    public void activateProcessDefinition(String processDefinitionId) {
        log.info("Activating process definition: {}", processDefinitionId);

        repositoryService.activateProcessDefinitionById(processDefinitionId);

        log.info("Process definition activated successfully");
    }

    /**
     * Get BPMN XML of a process definition
     */
    public String getProcessDefinitionXml(String processDefinitionId) {
        log.debug("Fetching BPMN XML for process definition: {}", processDefinitionId);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionId(processDefinitionId)
            .singleResult();

        if (processDefinition == null) {
            throw new RuntimeException("Process definition not found with ID: " + processDefinitionId);
        }

        try {
            // Get the BPMN XML using the resource name from process definition
            InputStream resourceStream = repositoryService.getResourceAsStream(
                processDefinition.getDeploymentId(),
                processDefinition.getResourceName()
            );

            if (resourceStream == null) {
                throw new RuntimeException("Resource not found for process definition: " + processDefinitionId);
            }

            String xml = new String(resourceStream.readAllBytes());
            log.debug("Successfully retrieved BPMN XML for process definition: {}", processDefinitionId);
            return xml;
        } catch (IOException e) {
            log.error("Error retrieving BPMN XML for process definition: {}", processDefinitionId, e);
            throw new RuntimeException("Failed to retrieve BPMN XML: " + e.getMessage(), e);
        }
    }

    /**
     * Map ProcessDefinition entity to response DTO
     */
    private ProcessDefinitionResponse mapToResponse(ProcessDefinition pd) {
        return ProcessDefinitionResponse.builder()
            .id(pd.getId())
            .key(pd.getKey())
            .name(pd.getName())
            .description(pd.getDescription())
            .version(pd.getVersion())
            .category(pd.getCategory())
            .deploymentId(pd.getDeploymentId())
            .resourceName(pd.getResourceName())
            .tenantId(pd.getTenantId())
            .suspended(pd.isSuspended())
            .hasStartFormKey(pd.hasStartFormKey())
            .hasGraphicalNotation(pd.hasGraphicalNotation())
            .build();
    }
}
