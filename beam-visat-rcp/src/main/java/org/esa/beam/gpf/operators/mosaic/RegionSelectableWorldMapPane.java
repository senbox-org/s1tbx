/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.gpf.operators.mosaic;

import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.ui.WorldMapPane;
import org.esa.beam.framework.ui.WorldMapPaneDataModel;

/**
 * TODO fill out or delete
 *
 * @author Thomas Storm
 */
public class RegionSelectableWorldMapPane extends WorldMapPane {

    private final BindingContext bindingContext;

    public RegionSelectableWorldMapPane(WorldMapPaneDataModel dataModel, BindingContext bindingContext) {
        super(dataModel);
        this.bindingContext = bindingContext;
    }

}
