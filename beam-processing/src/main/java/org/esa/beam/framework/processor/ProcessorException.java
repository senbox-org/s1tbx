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
package org.esa.beam.framework.processor;

/**
 * General exception class for all failures within the processor framework.
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class ProcessorException extends Exception {

    /**
     * Constructs the object with given error message.
     *
     * @param message the exception message
     */
    public ProcessorException(String message) {
        super(message);
    }

    /**
     * Constructs the object with given error message and cause.
     *
     * @param message the exception message
     * @param cause   the exception cause
     */
    public ProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
