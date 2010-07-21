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

package org.esa.beam.framework.ui.layer;

import com.bc.ceres.glayer.Layer;

import org.esa.beam.framework.ui.AppContext;

import javax.swing.JComponent;

/**
 * An editor for a specific layer type.
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public interface LayerEditor {

    /**
     * Creates the control for the user interface which is displayed
     * in the Layer Editor Toolview.
     * 
     * @param appContext the application context 
     * @param layer The layer to create the control for.
     * @return The control.
     */
    JComponent createControl(AppContext appContext, Layer layer);

    /**
     * It is called whenever the control must be updated.
     */
    void updateControl();
}
