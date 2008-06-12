/*
 * $Id: HeaderParseException.java,v 1.1 2006/09/18 06:34:32 marcop Exp $
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
 * An exception that can occur while parsing the ENVISAT MPH or SPH.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.beam.dataio.envisat.Header
 */
public class HeaderParseException extends Exception {

    private static final long serialVersionUID = 2631398493013961975L;

    /**
     * Constructs a new header parse exception with the given error message.
     */
    public HeaderParseException(String message) {
        super(message);
    }
}
