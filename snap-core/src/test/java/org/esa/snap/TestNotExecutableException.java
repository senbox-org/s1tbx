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
package org.esa.snap;

public class TestNotExecutableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>TestNotExecutableException</code> with no detail message.
     */
    public TestNotExecutableException() {
    }

    /**
     * Constructs a <code>TestNotExecutableException</code> with the specified detail message.
     *
     * @param s the detail message.
     */
    public TestNotExecutableException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>TestNotExecutableException</code> for the givne exception.
     *
     * @param e the exception which caused the test to fail
     */
    public TestNotExecutableException(Exception e) {
        super(e == null ? null : e.getClass().getName() + ": " + e.getMessage());
    }
}
