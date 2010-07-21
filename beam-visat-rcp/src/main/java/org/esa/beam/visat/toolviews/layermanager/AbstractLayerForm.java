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

package org.esa.beam.visat.toolviews.layermanager;

import org.esa.beam.framework.ui.AppContext;

import javax.swing.JComponent;

/**
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 */
abstract class AbstractLayerForm {
    private final AppContext appContext;

    protected AbstractLayerForm(AppContext appContext) {
        this.appContext = appContext;
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public abstract JComponent getFormControl();

    public abstract void updateFormControl();
}
