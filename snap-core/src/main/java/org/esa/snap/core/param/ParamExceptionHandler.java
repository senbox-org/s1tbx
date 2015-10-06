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
package org.esa.snap.core.param;

/**
 * The <code>ParamExceptionHandler</code> are an alternative way to handle parameter exceptions. Multiple methods in the
 * <code>Parameter</code> class accept a <code>ParamExceptionHandler</code> argument for this purpose.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @see Parameter
 */
public interface ParamExceptionHandler {

    /**
     * Notifies a client if an exeption occurred on a <code>Parameter</code>.
     *
     * @param e the exception
     *
     * @return <code>true</code> if the exception was handled successfully, <code>false</code> otherwise
     */
    boolean handleParamException(ParamException e);
}
