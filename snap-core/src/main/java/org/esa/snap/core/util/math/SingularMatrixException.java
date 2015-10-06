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
package org.esa.snap.core.util.math;

/**
 * Thrown, if a matrix is singular and processing cannot be continued.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class SingularMatrixException extends ArithmeticException {

    private static final long serialVersionUID = -6472145334613236151L;

    /**
     * Constructs a <code>SingularMatrixException</code> with no detail  message.
     */
    public SingularMatrixException() {
    }

    /**
     * Constructs a <code>SingularMatrixException</code> with the specified detail message.
     *
     * @param s the detail message.
     */
    public SingularMatrixException(String s) {
        super(s);
    }
}
