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
package org.esa.snap.core.util;

/**
 * The <code>AssertionFailure</code> class is an error caused by a failed program assertion.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see Debug
 * @see Debug#assertTrue(boolean)
 * @see Debug#assertTrue(boolean, String)
 */
public class AssertionFailure extends Error {
    private static final long serialVersionUID = 1799159745096232860L;

    /**
     * Constructs a new assertion failure. Calls <code>this(null)</code>
     *
     * @see #AssertionFailure(String)
     */
    public AssertionFailure() {
        this(null);
    }

    /**
     * Constructs a new assertion failure with the given associated error message.
     *
     * @param message the error message, if null the string "AssertionFailure" is used
     */
    public AssertionFailure(String message) {
        super(message == null ? "AssertionFailure" : message);
    }
}


