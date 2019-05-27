package org.esa.snap.vfs.preferences.validators;

import java.util.regex.Pattern;

/**
 * Validator for remote file repository schema.
 *
 * @author Adrian Draghici
 */
public class RepositorySchemaValidator {

    /**
     * The validation pattern remote file repository schema.
     */
    private static final String REPOSITORY_SCHEMA_VALIDATOR_PATTERN = "^([a-z0-9]+)$";

    /**
     * Validates remote file repository schema property
     *
     * @param value The value to be validated.
     * @throws IllegalArgumentException If a value validation failure occurs (not valid)
     */
    public void validateValue(Object value) {
        if (!isValid((String) value)) {
            throw new IllegalArgumentException("Invalid VFS repository schema! Please check if it meets following requirements:\n- It must contains only lowercase alphanumeric characters.");
        }
    }

    /**
     * Tells whether the remote file repository schema match the validation pattern.
     *
     * @param repositorySchema The remote file repository schema
     * @return {@code true} if remote file repository schema match the validation pattern
     */
    public boolean isValid(String repositorySchema) {
        return Pattern.compile(REPOSITORY_SCHEMA_VALIDATOR_PATTERN).matcher(repositorySchema).matches();
    }

}
