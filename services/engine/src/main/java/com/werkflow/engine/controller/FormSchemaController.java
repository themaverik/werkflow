package com.werkflow.engine.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.werkflow.engine.dto.FormSchema;
import com.werkflow.engine.dto.JwtUserContext;
import com.werkflow.engine.service.FormSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing form schemas.
 * Provides endpoints for CRUD operations on form-js schemas.
 */
@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Form Schemas", description = "Form schema management API")
public class FormSchemaController {

    private final FormSchemaService formSchemaService;

    /**
     * Get list of all forms
     * @return List of form schemas
     */
    @GetMapping
    @Operation(summary = "Get all forms", description = "Retrieve list of all active form schemas")
    public ResponseEntity<List<FormSchema>> getAllForms() {
        log.info("Getting all forms");
        List<FormSchema> forms = formSchemaService.getFormsList();
        return ResponseEntity.ok(forms);
    }

    /**
     * Get form schema by key (latest version)
     * @param formKey The form key
     * @return Form schema
     */
    @GetMapping("/{formKey}")
    @Operation(summary = "Get form by key", description = "Retrieve latest active version of form schema")
    public ResponseEntity<FormSchema> getFormByKey(
            @Parameter(description = "Form key identifier")
            @PathVariable String formKey) {
        log.info("Getting form by key: {}", formKey);
        FormSchema form = formSchemaService.loadFormSchema(formKey);
        return ResponseEntity.ok(form);
    }

    /**
     * Get specific version of form schema
     * @param formKey The form key
     * @param version The version number
     * @return Form schema
     */
    @GetMapping("/{formKey}/versions/{version}")
    @Operation(summary = "Get specific form version", description = "Retrieve specific version of form schema")
    public ResponseEntity<FormSchema> getFormByVersion(
            @Parameter(description = "Form key identifier")
            @PathVariable String formKey,
            @Parameter(description = "Version number")
            @PathVariable Integer version) {
        log.info("Getting form by key: {} version: {}", formKey, version);
        FormSchema form = formSchemaService.loadFormSchema(formKey, version);
        return ResponseEntity.ok(form);
    }

    /**
     * Get form version history
     * @param formKey The form key
     * @return List of all versions
     */
    @GetMapping("/{formKey}/versions")
    @Operation(summary = "Get form version history", description = "Retrieve all versions of a form schema")
    public ResponseEntity<List<FormSchema>> getFormVersions(
            @Parameter(description = "Form key identifier")
            @PathVariable String formKey) {
        log.info("Getting version history for form: {}", formKey);
        List<FormSchema> versions = formSchemaService.getFormHistory(formKey);
        return ResponseEntity.ok(versions);
    }

    /**
     * Create new form schema
     * @param request Form creation request
     * @param authentication User authentication
     * @return Created form schema
     */
    @PostMapping
    @Operation(summary = "Create form", description = "Create a new form schema")
    public ResponseEntity<FormSchema> createForm(
            @RequestBody CreateFormRequest request,
            Authentication authentication) {

        String createdBy = extractUsername(authentication);
        log.info("Creating form: {} by user: {}", request.getFormKey(), createdBy);

        FormSchema form = formSchemaService.saveFormSchema(
                request.getFormKey(),
                request.getSchemaJson(),
                request.getDescription(),
                request.getFormType(),
                createdBy
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(form);
    }

    /**
     * Update form schema (creates new version)
     * @param formKey The form key
     * @param request Form update request
     * @param authentication User authentication
     * @return Updated form schema
     */
    @PutMapping("/{formKey}")
    @Operation(summary = "Update form", description = "Update form schema (creates new version)")
    public ResponseEntity<FormSchema> updateForm(
            @Parameter(description = "Form key identifier")
            @PathVariable String formKey,
            @RequestBody UpdateFormRequest request,
            Authentication authentication) {

        String updatedBy = extractUsername(authentication);
        log.info("Updating form: {} by user: {}", formKey, updatedBy);

        FormSchema form = formSchemaService.updateFormSchema(
                formKey,
                request.getSchemaJson(),
                request.getDescription(),
                updatedBy
        );

        return ResponseEntity.ok(form);
    }

    /**
     * Delete/archive form
     * @param formKey The form key
     * @return Success response
     */
    @DeleteMapping("/{formKey}")
    @Operation(summary = "Delete form", description = "Archive/deactivate form schema")
    public ResponseEntity<Map<String, String>> deleteForm(
            @Parameter(description = "Form key identifier")
            @PathVariable String formKey) {
        log.info("Deleting form: {}", formKey);
        formSchemaService.deleteForm(formKey);
        return ResponseEntity.ok(Map.of("message", "Form archived successfully", "formKey", formKey));
    }

    /**
     * Validate form schema
     * @param schemaJson The schema to validate
     * @return Validation result
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate form schema", description = "Validate form-js schema structure")
    public ResponseEntity<Map<String, Object>> validateSchema(
            @RequestBody JsonNode schemaJson) {
        log.info("Validating form schema");
        boolean isValid = formSchemaService.validateFormSchema(schemaJson);
        return ResponseEntity.ok(Map.of("valid", isValid, "message", "Schema is valid"));
    }

    /**
     * Extract username from authentication
     */
    private String extractUsername(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            JwtUserContext userContext = new JwtUserContext(jwt);
            return userContext.getUserId();
        }
        return "system";
    }

    /**
     * Request DTO for creating forms
     */
    public record CreateFormRequest(
            String formKey,
            JsonNode schemaJson,
            String description,
            FormSchema.FormType formType
    ) {}

    /**
     * Request DTO for updating forms
     */
    public record UpdateFormRequest(
            JsonNode schemaJson,
            String description
    ) {}
}
