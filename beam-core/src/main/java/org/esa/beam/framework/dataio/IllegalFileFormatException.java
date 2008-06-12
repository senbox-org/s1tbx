/*
 * $Id: IllegalFileFormatException.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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
package org.esa.beam.framework.dataio;


/**
 * A <code>ProductIOException</code> that is thrown by <code>ProductReader</code>s
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class IllegalFileFormatException extends ProductIOException {

    private static final long serialVersionUID = -3109342364111884614L;

    /**
     * Constructs a new exception with no error message.
     */
    public IllegalFileFormatException() {
        super();
    }

    /**
     * Constructs a new exception with the given error message.
     *
     * @param message the error message
     */
    public IllegalFileFormatException(String message) {
        super(message);
    }
}
