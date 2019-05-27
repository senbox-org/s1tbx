package org.esa.snap.vfs.remote.swift;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * AuthorizationToken class for Swift Authorization Token
 *
 * @author Adrian Drăghici
 */
final class SwiftAuthorizationToken {

    /**
     * The date-time format used.
     */
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

    private String token;
    private LocalDateTime expirationDate = null; //no expires

    /**
     * Creates the new SwiftAuthorizationToken using given token and expiration date
     *
     * @param token          The swift authorization token
     * @param expirationDate The expiration date string of swift authorization token
     */
    SwiftAuthorizationToken(String token, String expirationDate) {
        this.token = token;
        if (expirationDate != null && !expirationDate.isEmpty()) {
            this.expirationDate = LocalDateTime.parse(expirationDate, ISO_DATE_TIME);
        }
    }

    /**
     * Check whether the token is valid (not expired)
     *
     * @return {@code true} if the token is valid (expiration date is after now)
     */
    boolean isValid() {
        return this.expirationDate != null && this.expirationDate.isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Return the swift authorization token
     *
     * @return The swift authorization token
     */
    String getToken() {
        return this.token;
    }
}
