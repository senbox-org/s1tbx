/*
 * $Id: SingularMatrixException.java,v 1.1.1.1 2006/09/11 08:16:47 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.util.math;

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
