/*
 * $Id: ParseException.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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
package com.bc.jexp;

/**
 * An exception thrown by a <code>ParserX.parse()</code> call in order to
 * signal a parser error.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class ParseException extends Exception {

    private static final long serialVersionUID = 4479172738937713857L;
    
    /**
     * The source code _line number in which the error occured.
     */
    private final int _line;
    /**
     * The _column number in the source code in which the error occured.
     */
    private final int _column;

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
     * @param line the source code _line number in which the error occured
     * @param column the _column number in the source code in which the error occured
     * @param message the error message
     */
    public ParseException(final int line, final int column, final String message) {
        super(message);
        _line = line;
        _column = column;
    }

    /**
     * Gets the source code _line number in which the error occured.
     * @return the _line number or <code>-1</code> if not available
     */
    public final int getLine() {
        return _line;
    }

    /**
     * Gets the _column number in the source code in which the error occured.
     * @return the _column number or <code>-1</code> if not available
     */
    public final int getColumn() {
        return _column;
    }
}
