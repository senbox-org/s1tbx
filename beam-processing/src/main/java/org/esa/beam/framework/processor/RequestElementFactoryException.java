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
 * Exceptions of this type are thrown within a <code>RequestElementFactory</code> if an element of a request could not
 * be created. This can have multiple reasons: <ld> <li>The external format is which the request is stored, is invalid
 * (syntax errors)</li> <li>A request parameter has an unknown data type</li> <li>A request parameter value is not of
 * the desired data type</li> <li>Missing or incomplete information in general</li> </ld>
 *
 * @author Norman Fomferra
 * @version $Date$
 *
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class RequestElementFactoryException extends ProcessorException {

    /**
     * Constructs a new <code>RequestElementFactoryException</code> with given detail message.
     *
     * @param message the exception detail message
     */
    public RequestElementFactoryException(String message) {
        super(message);
    }

    /**
     * Constructs the object with given error message and cause.
     *
     * @param message the exception message
     * @param cause the exception cause
     */
    public RequestElementFactoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
