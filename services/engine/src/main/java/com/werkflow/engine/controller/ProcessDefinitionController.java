package com.werkflow.engine.controller;

import com.werkflow.engine.dto.ProcessDefinitionResponse;
import com.werkflow.engine.service.ProcessDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for managing BPMN process definitions
 */
@RestController
@RequestMapping("/api/process-definitions")
@RequiredArgsConstructor
@Tag(name = "Process Definitions", description = "BPMN process definition management")
@SecurityRequirement(name = "bearer-jwt")
public class ProcessDefinitionController {

    private final ProcessDefinitionService processDefinitionService;

    @PostMapping(value = "/deploy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('WORKFLOW_DESIGNER', 'SUPER_ADMIN')")
    @Operation(summary = "Deploy a new process definition", description = "Upload and deploy a BPMN 2.0 XML file")
    public ResponseEntity<ProcessDefinitionResponse> deployProcessDefinition(
        @Parameter(description = "BPMN XML file") @RequestParam("file") MultipartFile file
    ) {
        ProcessDefinitionResponse response = processDefinitionService.deployProcessDefinition(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all process definitions", description = "Retrieve all process definitions (latest versions)")
    public ResponseEntity<List<ProcessDefinitionResponse>> getAllProcessDefinitions() {
        List<ProcessDefinitionResponse> responses = processDefinitionService.getAllProcessDefinitions();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get process definition by ID")
    public ResponseEntity<ProcessDefinitionResponse> getProcessDefinitionById(
        @Parameter(description = "Process definition ID") @PathVariable String id
    ) {
        ProcessDefinitionResponse response = processDefinitionService.getProcessDefinitionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/key/{key}")
    @Operation(summary = "Get process definition by key", description = "Get latest version of process definition by key")
    public ResponseEntity<ProcessDefinitionResponse> getProcessDefinitionByKey(
        @Parameter(description = "Process definition key") @PathVariable String key
    ) {
        ProcessDefinitionResponse response = processDefinitionService.getProcessDefinitionByKey(key);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/key/{key}/versions")
    @Operation(summary = "Get all versions of a process definition")
    public ResponseEntity<List<ProcessDefinitionResponse>> getProcessDefinitionVersions(
        @Parameter(description = "Process definition key") @PathVariable String key
    ) {
        List<ProcessDefinitionResponse> responses = processDefinitionService.getProcessDefinitionVersions(key);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/deployment/{deploymentId}")
    @PreAuthorize("hasAnyRole('WORKFLOW_DESIGNER', 'SUPER_ADMIN')")
    @Operation(summary = "Delete process definition deployment")
    public ResponseEntity<Void> deleteProcessDefinition(
        @Parameter(description = "Deployment ID") @PathVariable String deploymentId,
        @Parameter(description = "Cascade delete (delete running instances)") @RequestParam(defaultValue = "false") boolean cascade
    ) {
        processDefinitionService.deleteProcessDefinition(deploymentId, cascade);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('WORKFLOW_DESIGNER', 'SUPER_ADMIN')")
    @Operation(summary = "Suspend process definition")
    public ResponseEntity<Void> suspendProcessDefinition(
        @Parameter(description = "Process definition ID") @PathVariable String id
    ) {
        processDefinitionService.suspendProcessDefinition(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('WORKFLOW_DESIGNER', 'SUPER_ADMIN')")
    @Operation(summary = "Activate process definition")
    public ResponseEntity<Void> activateProcessDefinition(
        @Parameter(description = "Process definition ID") @PathVariable String id
    ) {
        processDefinitionService.activateProcessDefinition(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/xml")
    @Operation(summary = "Get process definition BPMN XML", description = "Retrieve the BPMN XML representation of a process definition")
    public ResponseEntity<String> getProcessDefinitionXml(
        @Parameter(description = "Process definition ID") @PathVariable String id
    ) {
        String xml = processDefinitionService.getProcessDefinitionXml(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(xml);
    }
}
