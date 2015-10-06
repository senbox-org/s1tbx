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
package org.esa.snap.core.dataio;

import java.io.IOException;

/**
 * A <code>java.io.IOException</code> that is thrown by <code>ProductReader</code>s, <code>ProductWriters</code>s and
 * <code>ProductIO</code>.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ProductIOException extends IOException {

    private static final long serialVersionUID = -8807981283294580325L;

    /**
     * Constructs a new exception with no error message.
     */
    public ProductIOException() {
        super();
    }

    /**
     * Constructs a new exception with the given error message.
     *
     * @param message the error message
     */
    public ProductIOException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the given error message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception 
     *
     */
    public ProductIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
