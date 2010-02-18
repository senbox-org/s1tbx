/*
 * $Id: ProcessorException.java,v 1.1 2006/10/10 14:47:34 norman Exp $
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
 * General exception class for all failures within the processor framework.
 */
public class ProcessorException extends Exception {

    /**
     * Constructs the object with given error message.
     *
     * @param message the exception message
     */
    public ProcessorException(String message) {
        super(message);
    }

    /**
     * Constructs the object with given error message and cause.
     *
     * @param message the exception message
     * @param cause the exception cause
     */
    public ProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
