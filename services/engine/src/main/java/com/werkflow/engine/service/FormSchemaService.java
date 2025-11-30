package com.werkflow.engine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.werkflow.engine.dto.FormSchema;
import com.werkflow.engine.exception.FormNotFoundException;
import com.werkflow.engine.exception.FormValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing form schemas in the database.
 * Handles CRUD operations for form-js schemas stored in PostgreSQL.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FormSchemaService {

    private final JdbcTemplate jdbcTemplate;
    private final FormSchemaValidator formSchemaValidator;
    private final ObjectMapper objectMapper;

    /**
     * Load form schema by form key (latest active version)
     * @param formKey The form key identifier
     * @return FormSchema
     * @throws FormNotFoundException if form is not found
     */
    @Cacheable(value = "formSchemas", key = "#formKey")
    public FormSchema loadFormSchema(String formKey) {
        log.info("Loading latest form schema for key: {}", formKey);

        String sql = """
                SELECT id, form_key, version, schema_json, description, form_type,
                       is_active, created_at, updated_at, created_by, updated_by
                FROM form_schemas
                WHERE form_key = ? AND is_active = true
                ORDER BY version DESC
                LIMIT 1
                """;

        List<FormSchema> schemas = jdbcTemplate.query(sql, new FormSchemaRowMapper(), formKey);

        if (schemas.isEmpty()) {
            throw new FormNotFoundException(formKey);
        }

        return schemas.get(0);
    }

    /**
     * Load specific version of form schema
     * @param formKey The form key identifier
     * @param version The version number
     * @return FormSchema
     * @throws FormNotFoundException if form is not found
     */
    @Cacheable(value = "formSchemas", key = "#formKey + '_v' + #version")
    public FormSchema loadFormSchema(String formKey, Integer version) {
        log.info("Loading form schema for key: {} version: {}", formKey, version);

        String sql = """
                SELECT id, form_key, version, schema_json, description, form_type,
                       is_active, created_at, updated_at, created_by, updated_by
                FROM form_schemas
                WHERE form_key = ? AND version = ?
                """;

        List<FormSchema> schemas = jdbcTemplate.query(sql, new FormSchemaRowMapper(), formKey, version);

        if (schemas.isEmpty()) {
            throw new FormNotFoundException(formKey, version);
        }

        return schemas.get(0);
    }

    /**
     * Save new form schema
     * @param formKey The form key identifier
     * @param schemaJson The form schema JSON
     * @param description Description of the form
     * @param formType Type of form
     * @param createdBy User creating the form
     * @return Created FormSchema
     */
    @Transactional
    @CacheEvict(value = "formSchemas", allEntries = true)
    public FormSchema saveFormSchema(String formKey, JsonNode schemaJson, String description,
                                      FormSchema.FormType formType, String createdBy) {
        log.info("Saving form schema for key: {}", formKey);

        // Validate schema structure
        formSchemaValidator.validateFormSchema(schemaJson);

        // Get next version number
        Integer nextVersion = getNextVersion(formKey);

        // Insert new version
        String sql = """
                INSERT INTO form_schemas (id, form_key, version, schema_json, description, form_type,
                                          is_active, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?::jsonb, ?, ?, true, ?, ?, ?, ?)
                """;

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        try {
            String schemaJsonStr = objectMapper.writeValueAsString(schemaJson);

            jdbcTemplate.update(sql,
                    id,
                    formKey,
                    nextVersion,
                    schemaJsonStr,
                    description,
                    formType.name(),
                    Timestamp.from(now),
                    Timestamp.from(now),
                    createdBy,
                    createdBy
            );

            log.info("Saved form schema: {} version: {}", formKey, nextVersion);

            return loadFormSchema(formKey, nextVersion);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize schema JSON", e);
            throw new FormValidationException("Invalid JSON schema format");
        }
    }

    /**
     * Update existing form schema (creates new version)
     * @param formKey The form key identifier
     * @param schemaJson Updated schema JSON
     * @param description Updated description
     * @param updatedBy User updating the form
     * @return Updated FormSchema
     */
    @Transactional
    @CacheEvict(value = "formSchemas", allEntries = true)
    public FormSchema updateFormSchema(String formKey, JsonNode schemaJson, String description,
                                        String updatedBy) {
        log.info("Updating form schema for key: {}", formKey);

        // Validate schema structure
        formSchemaValidator.validateFormSchema(schemaJson);

        // Load current schema to get form type
        FormSchema currentSchema = loadFormSchema(formKey);

        // Deactivate previous versions
        String deactivateSql = """
                UPDATE form_schemas
                SET is_active = false, updated_at = ?, updated_by = ?
                WHERE form_key = ? AND is_active = true
                """;

        jdbcTemplate.update(deactivateSql, Timestamp.from(Instant.now()), updatedBy, formKey);

        // Create new version
        return saveFormSchema(formKey, schemaJson, description, currentSchema.getFormType(), updatedBy);
    }

    /**
     * Get list of all active forms
     * @return List of FormSchema
     */
    public List<FormSchema> getFormsList() {
        log.info("Getting list of all forms");

        String sql = """
                SELECT DISTINCT ON (form_key) id, form_key, version, schema_json, description, form_type,
                       is_active, created_at, updated_at, created_by, updated_by
                FROM form_schemas
                WHERE is_active = true
                ORDER BY form_key, version DESC
                """;

        return jdbcTemplate.query(sql, new FormSchemaRowMapper());
    }

    /**
     * Get form version history
     * @param formKey The form key identifier
     * @return List of all versions
     */
    public List<FormSchema> getFormHistory(String formKey) {
        log.info("Getting version history for form: {}", formKey);

        String sql = """
                SELECT id, form_key, version, schema_json, description, form_type,
                       is_active, created_at, updated_at, created_by, updated_by
                FROM form_schemas
                WHERE form_key = ?
                ORDER BY version DESC
                """;

        return jdbcTemplate.query(sql, new FormSchemaRowMapper(), formKey);
    }

    /**
     * Archive form (deactivate all versions)
     * @param formKey The form key to archive
     */
    @Transactional
    @CacheEvict(value = "formSchemas", allEntries = true)
    public void archiveForm(String formKey) {
        log.info("Archiving form: {}", formKey);

        String sql = """
                UPDATE form_schemas
                SET is_active = false, updated_at = ?
                WHERE form_key = ?
                """;

        int updated = jdbcTemplate.update(sql, Timestamp.from(Instant.now()), formKey);

        if (updated == 0) {
            throw new FormNotFoundException(formKey);
        }

        log.info("Archived {} versions of form: {}", updated, formKey);
    }

    /**
     * Delete form schema (soft delete by deactivating)
     * @param formKey The form key to delete
     */
    @Transactional
    @CacheEvict(value = "formSchemas", allEntries = true)
    public void deleteForm(String formKey) {
        archiveForm(formKey);
    }

    /**
     * Validate form schema
     * @param schemaJson The schema to validate
     * @return true if valid
     * @throws FormValidationException if invalid
     */
    public boolean validateFormSchema(JsonNode schemaJson) {
        formSchemaValidator.validateFormSchema(schemaJson);
        return true;
    }

    /**
     * Get next version number for a form
     */
    private Integer getNextVersion(String formKey) {
        String sql = """
                SELECT COALESCE(MAX(version), 0) + 1
                FROM form_schemas
                WHERE form_key = ?
                """;

        Integer nextVersion = jdbcTemplate.queryForObject(sql, Integer.class, formKey);
        return nextVersion != null ? nextVersion : 1;
    }

    /**
     * Row mapper for FormSchema
     */
    private class FormSchemaRowMapper implements RowMapper<FormSchema> {
        @Override
        public FormSchema mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                String schemaJsonStr = rs.getString("schema_json");
                JsonNode schemaJson = objectMapper.readTree(schemaJsonStr);

                return FormSchema.builder()
                        .id(UUID.fromString(rs.getString("id")))
                        .formKey(rs.getString("form_key"))
                        .version(rs.getInt("version"))
                        .schemaJson(schemaJson)
                        .description(rs.getString("description"))
                        .formType(FormSchema.FormType.valueOf(rs.getString("form_type")))
                        .isActive(rs.getBoolean("is_active"))
                        .createdAt(rs.getTimestamp("created_at").toInstant())
                        .updatedAt(rs.getTimestamp("updated_at").toInstant())
                        .createdBy(rs.getString("created_by"))
                        .updatedBy(rs.getString("updated_by"))
                        .build();
            } catch (JsonProcessingException e) {
                log.error("Failed to parse schema JSON", e);
                throw new SQLException("Failed to parse schema JSON", e);
            }
        }
    }
}
