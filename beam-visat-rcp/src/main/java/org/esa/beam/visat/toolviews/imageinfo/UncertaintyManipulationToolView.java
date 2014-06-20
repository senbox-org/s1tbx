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
package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.JComponent;


/**
 * The color manipulation tool window.
 */
public class UncertaintyManipulationToolView extends AbstractToolView {

    public static final String ID = UncertaintyManipulationToolView.class.getName();

    public UncertaintyManipulationToolView() {
    }

    @Override
    protected JComponent createControl() {
        ColorManipulationForm cmf = new ColorManipulationForm(this, new MyFormModel());
        return cmf.getContentPanel();
    }

    private static class MyFormModel extends FormModel {
        @Override
        public boolean isValid() {
            return super.isValid() && getRaster() != null;
        }

        @Override
        public RasterDataNode getRaster() {
            RasterDataNode raster = getProductSceneView().getRaster();
            RasterDataNode uncertaintyBand;
            uncertaintyBand = raster.getAncillaryBand("uncertainty");
            if (uncertaintyBand != null) {
                return uncertaintyBand;
            }
            uncertaintyBand = raster.getAncillaryBand("variance");
            if (uncertaintyBand != null) {
                return uncertaintyBand;
            }
            return null;
        }

        @Override
        public RasterDataNode[] getRasters() {
            RasterDataNode raster = getRaster();
            if (raster != null) {
                return new RasterDataNode[] {raster};
            }
            return null;
        }

        @Override
        public void setRasters(RasterDataNode[] rasters) {
            // not applicable
        }

        @Override
        public void applyModifiedImageInfo() {
            getProductSceneView().updateImage();
        }
    }
}