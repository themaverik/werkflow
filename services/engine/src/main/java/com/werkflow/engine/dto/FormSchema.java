package com.werkflow.engine.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a Form Schema in the Werkflow system.
 * Contains the form-js schema definition and metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormSchema {

    /**
     * Unique identifier for the form schema
     */
    private UUID id;

    /**
     * Form key - unique identifier for the form (e.g., "capex-request-form")
     */
    private String formKey;

    /**
     * Version number for schema evolution
     */
    private Integer version;

    /**
     * The complete form-js schema definition in JSON format
     */
    private JsonNode schemaJson;

    /**
     * Human-readable description of the form
     */
    private String description;

    /**
     * Type of form: PROCESS_START, TASK_FORM, APPROVAL, CUSTOM
     */
    private FormType formType;

    /**
     * Whether this version is currently active
     */
    private Boolean isActive;

    /**
     * Timestamp when the schema was created
     */
    private Instant createdAt;

    /**
     * Timestamp when the schema was last updated
     */
    private Instant updatedAt;

    /**
     * User who created the schema
     */
    private String createdBy;

    /**
     * User who last updated the schema
     */
    private String updatedBy;

    /**
     * Enumeration for form types
     */
    public enum FormType {
        PROCESS_START,
        TASK_FORM,
        APPROVAL,
        CUSTOM
    }
}
