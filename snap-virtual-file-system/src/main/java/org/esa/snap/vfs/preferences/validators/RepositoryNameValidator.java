package org.esa.snap.vfs.preferences.validators;

import java.util.regex.Pattern;

/**
 * Validator for remote file repository name.
 *
 * @author Adrian Draghici
 */
public class RepositoryNameValidator {

    /**
     * The validation pattern for remote file repository name.
     */
    private static final String REPOSITORY_NAME_VALIDATOR_PATTERN = "^([\\w\\-]{3,25})$";

    /**
     * Validates remote file repository name property
     *
     * @param value The value to be validated.
     * @throws IllegalArgumentException If a value validation failure occurs (not valid)
     */
    public void validateValue(Object value) {
        if (!isValid((String) value)) {
            throw new IllegalArgumentException("Invalid VFS repository name! Please check if it meets following requirements:\n- It must be alphanumeric.\n- Underscores and hyphens are allowed.\n- Length is between 3 and 25 characters.");
        }
    }

    /**
     * Tells whether the remote file repository name match the validation pattern.
     *
     * @param repositoryName The remote file repository name
     * @return {@code true} if remote file repository name match the validation pattern
     */
    public boolean isValid(String repositoryName) {
        return Pattern.compile(REPOSITORY_NAME_VALIDATOR_PATTERN).matcher(repositoryName).matches();
    }

}
