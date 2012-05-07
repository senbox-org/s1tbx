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
package org.esa.beam.framework.ui;


/**
 * The <code>ExceptionHandler</code> are an alternative way to handle exceptions in the <code>ProductTree</code>.
 * Methods who catch exceptions can use this handler to popup an error Dialog.
 *
 * @author Sabine Embacher
 * @version $Revision$  $Date$
 * @see org.esa.beam.framework.ui.product.ProductTree
 */
public interface ExceptionHandler {

    /**
     * Notifies a client if an exeption occurred on a <code>ProductTree</code>.
     *
     * @param e the exception
     *
     * @return <code>true</code> if the exception was handled successfully, <code>false</code> otherwise
     */
    boolean handleException(Exception e);
}
