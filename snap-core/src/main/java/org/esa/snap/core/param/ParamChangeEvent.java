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

import java.util.EventObject;

/**
 * A <code>ParamChangeEvent</code> occurs when the value of parameter has changed.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @see Parameter
 */
public class ParamChangeEvent extends EventObject {

    private final Parameter _parameter;
    private final Object _oldValue;


    /**
     * Constructs a new event for the given source, parameter and the parameter's old value.
     *
     * @param source    the source which fired this event
     * @param parameter the parameter whose value changed
     * @param oldValue  the parameter's old value
     */
    public ParamChangeEvent(final Object source, final Parameter parameter, final Object oldValue) {
        super(source);
        _parameter = parameter;
        _oldValue = oldValue;
    }


    /**
     * Returns the parameter whose value changed.
     */
    public Parameter getParameter() {
        return _parameter;
    }

    /**
     * Returns the parameter's old value.
     */
    public Object getOldValue() {
        return _oldValue;
    }
}

