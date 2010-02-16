/*
 * $Id: TestNotExecutableException.java,v 1.1 2006/09/18 06:34:20 marcop Exp $
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
package org.esa.beam;

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
