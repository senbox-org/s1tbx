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

import java.util.EventListener;


/**
 * A <code>ParamChangeListener</code> is notified when the value of parameter has changed.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @see Parameter
 * @see ParamChangeEvent
 */
public interface ParamChangeListener extends EventListener {

    /**
     *@link dependency
     * @stereotype use
     */

    /*#ParamChangeEvent lnkParamChangeEvent;*/

    /**
     * Called if the value of a parameter changed.
     *
     * @param event the parameter change event
     */
    void parameterValueChanged(ParamChangeEvent event);
}


