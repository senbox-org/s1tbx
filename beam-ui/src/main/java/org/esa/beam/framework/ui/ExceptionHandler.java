/*
 * $Id: ExceptionHandler.java,v 1.1 2006/10/10 14:47:38 norman Exp $
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
package org.esa.beam.framework.ui;


/**
 * The <code>ExceptionHandler</code> are an alternative way to handle exceptions in the <code>ProductTree</code>.
 * Methods who catch exceptions can use this handler to popup an error Dialog.
 *
 * @author Sabine embacher
 * @version $Revision$  $Date$
 * @see org.esa.beam.framework.ui.product.ProductTree
 */
public interface ExceptionHandler {

    /**
     * Notifies a client if an exeption occured on a <code>ProductTree</code>.
     *
     * @param e the exception
     *
     * @return <code>true</code> if the exception was handled successfully, <code>false</code> otherwise
     */
    boolean handleException(Exception e);
}
