/*
 * $Id: DDDBException.java,v 1.1 2006/09/18 06:34:32 marcop Exp $
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
package org.esa.beam.dataio.envisat;

/**
 * An  I/O exception thrown by the <code>DDDB</code> class in order to signal internal I/O errors with origin in the
 * DDDB.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class DDDBException extends RuntimeException {

    private static final long serialVersionUID = 1164442142242614057L;

    /**
     * Constructs a new DDDB exception with the given detail message.
     *
     * @param message the detail message
     */
    public DDDBException(String message) {
        super(message);
    }
}
