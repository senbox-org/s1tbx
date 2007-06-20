/*
 * $Id: BitmaskExpressionParseException.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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

package org.esa.beam.framework.dataop.bitmask;

/**
 * The <code>BitmaskExpressionParseException</code> is an exception thrown by the <code>BitmaskExpressionParser</code>
 * in order to syntax or sematical errors identified in bitmap expressions.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.1.1.1 $ $Date: 2006/09/11 08:16:45 $
 */
public class BitmaskExpressionParseException extends Exception {

    /**
     * Creates a new parse exception with the given detail message.
     *
     * @param message the detail message
     */
    public BitmaskExpressionParseException(final String message) {
        super(message);
    }
}
