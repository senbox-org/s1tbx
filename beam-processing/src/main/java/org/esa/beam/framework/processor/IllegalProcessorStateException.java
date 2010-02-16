/*
 * $Id: IllegalProcessorStateException.java,v 1.1 2006/10/10 14:47:34 norman Exp $
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
package org.esa.beam.framework.processor;

/**
 * An exception which occurs if an illegal processor state has been encountered.
 *
 * @author Tom Block
 * @version $revision$ $date$
 */
public class IllegalProcessorStateException extends ProcessorException {

    /**
     * Constructs a new exception with default error message.
     */
    public IllegalProcessorStateException() {
        super("Illegal processor state");
    }

    /**
     * Constructs a new exception with the given error message.
     *
     * @param message the error message
     */
    public IllegalProcessorStateException(String message) {
        super(message);
    }
}
