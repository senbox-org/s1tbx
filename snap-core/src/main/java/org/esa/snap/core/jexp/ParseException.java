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
package org.esa.snap.core.jexp;

/**
 * An exception thrown by a <code>ParserX.parse()</code> call in order to
 * signal a parser error.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class ParseException extends Exception {

    private static final long serialVersionUID = 4479172738937713857L;
    
    /**
     * The source code line number in which the error occurred.
     */
    private final int line;

    /**
     * The column number in the source code in which the error occurred.
     */
    private final int column;

    /**
     * Constructs a new parser exception with the given message.
     * @param message the error message
     */
    public ParseException(final String message) {
        this(-1, -1, message);
    }

    /**
     * Constructs a new parser exception with the given position in source code
     * and with the given message.
     *
     * @param line the source code _line number in which the error occurred
     * @param column the _column number in the source code in which the error occurred
     * @param message the error message
     */
    public ParseException(final int line, final int column, final String message) {
        super(message);
        this.line = line;
        this.column = column;
    }

    /**
     * Gets the source code _line number in which the error occurred.
     * @return the _line number or <code>-1</code> if not available
     */
    public final int getLine() {
        return line;
    }

    /**
     * Gets the _column number in the source code in which the error occurred.
     * @return the _column number or <code>-1</code> if not available
     */
    public final int getColumn() {
        return column;
    }
}
