/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.gpf;

/**
 * A general exception class for all failures within an {@link Operator}
 * implementation.
 */
public class OperatorException extends RuntimeException {


    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public OperatorException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public OperatorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception by delegating to
     * {@link #OperatorException(String,Throwable) this(cause.getMessage(), cause)}.
     *
     * @param cause the cause
     */
    public OperatorException(Throwable cause) {
        this(cause.getMessage(), cause);
    }
}
