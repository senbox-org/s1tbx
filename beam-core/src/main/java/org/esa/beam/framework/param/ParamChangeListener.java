/*
 * $Id: ParamChangeListener.java,v 1.1.1.1 2006/09/11 08:16:46 norman Exp $
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
package org.esa.beam.framework.param;

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


