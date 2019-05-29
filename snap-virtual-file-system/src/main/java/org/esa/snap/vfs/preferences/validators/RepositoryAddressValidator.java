package org.esa.snap.vfs.preferences.validators;

import java.util.regex.Pattern;

/**
 * Validator for remote file repository address.
 *
 * @author Adrian Draghici
 */
public class RepositoryAddressValidator {

    /**
     * The validation pattern remote file repository address.
     */
    private static final String REPOSITORY_ADDRESS_VALIDATOR_PATTERN = "^([a-z0-9/\\\\:.#$%&\\-]+)$";

    /**
     * Validates remote file repository address property
     *
     * @param value The value to be validated.
     * @throws IllegalArgumentException If a value validation failure occurs (not valid)
     */
    public void validateValue(Object value) {
        if (!isValid((String) value)) {
            throw new IllegalArgumentException("Invalid VFS repository address! Please check if it meets following requirements:\n- It must contains URL specific characters");
        }
    }

    /**
     * Tells whether the remote file repository address match the validation pattern.
     *
     * @param repositoryAddress The remote file repository address
     * @return {@code true} if remote file repository address match the validation pattern
     */
    public boolean isValid(String repositoryAddress) {
        return Pattern.compile(REPOSITORY_ADDRESS_VALIDATOR_PATTERN).matcher(repositoryAddress).matches();
    }


}
