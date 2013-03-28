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
 * An exception which occurs if an illegal processor argument or parameter has been encountered.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class IllegalProcessorParamException extends ProcessorException {

    /**
     * Constructs a new exception with default error message.
     */
    public IllegalProcessorParamException() {
        super("Illegal processor state");
    }

    /**
     * Constructs a new exception with the given error message.
     *
     * @param message the error message
     */
    public IllegalProcessorParamException(String message) {
        super(message);
    }
}
