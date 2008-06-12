/*
 * $Id: ProductIOException.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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

import java.io.IOException;

/**
 * A <code>java.io.IOException</code> that is thrown by <code>ProductReader</code>s, <code>ProductWriters</code>s and
 * <code>ProductIO</code>.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ProductIOException extends IOException {

    private static final long serialVersionUID = -8807981283294580325L;

    /**
     * Constructs a new exception with no error message.
     */
    public ProductIOException() {
        super();
    }

    /**
     * Constructs a new exception with the given error message.
     *
     * @param message the error message
     */
    public ProductIOException(String message) {
        super(message);
    }
}
