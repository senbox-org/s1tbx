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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.jai.ImageManager;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * The color manipulation tool window.
 */
public class UncertaintyVisualisationToolView extends AbstractToolView {

    public static final String ID = UncertaintyVisualisationToolView.class.getName();

    public UncertaintyVisualisationToolView() {
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
            return ImageManager.getUncertaintyBand(raster);
        }

        @Override
        public RasterDataNode[] getRasters() {
            RasterDataNode raster = getRaster();
            if (raster != null) {
                return new RasterDataNode[]{raster};
            }
            return null;
        }

        @Override
        public void setRasters(RasterDataNode[] rasters) {
            // not applicable
        }

        @Override
        public ImageInfo getOriginalImageInfo() {
            return getRaster().getImageInfo(ProgressMonitor.NULL);
        }

        @Override
        public void applyModifiedImageInfo() {
            getProductSceneView().updateImage();
        }

        @Override
        public boolean canUseHistogramMatching() {
            return false;
        }

        @Override
        public void modifyMoreOptionsForm(MoreOptionsForm moreOptionsForm) {

            JComboBox<ImageInfo.UncertaintyVisualisationMode> modeBox = new JComboBox<>(ImageInfo.UncertaintyVisualisationMode.values());
            modeBox.setEditable(false);

            moreOptionsForm.addRow(new JLabel("Mode: "), modeBox);

            Property modeProperty = Property.create("mode", ImageInfo.UncertaintyVisualisationMode.class);
            RasterDataNode uncertaintyRaster = getRaster();
            try {
                if (uncertaintyRaster != null) {
                    modeProperty.setValue(uncertaintyRaster.getImageInfo(ProgressMonitor.NULL).getUncertaintyVisualisationMode());
                }else {
                    modeProperty.setValue(ImageInfo.UncertaintyVisualisationMode.None);
                }
            } catch (ValidationException e) {
                // ok
            }
            moreOptionsForm.getBindingContext().getPropertySet().addProperty(modeProperty);
            moreOptionsForm.getBindingContext().bind(modeProperty.getName(), modeBox);

            moreOptionsForm.getBindingContext().addPropertyChangeListener(modeProperty.getName(), new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    RasterDataNode uncertaintyRaster = getRaster();
                    if (uncertaintyRaster != null) {
                        ImageInfo.UncertaintyVisualisationMode uncertaintyVisualisationMode = (ImageInfo.UncertaintyVisualisationMode) evt.getNewValue();
                        System.out.println("uncertaintyVisualisationMode = " + uncertaintyVisualisationMode);
                        uncertaintyRaster.getImageInfo().setUncertaintyVisualisationMode(uncertaintyVisualisationMode);
                        applyModifiedImageInfo();
                    }
                }
            });
        }
    }
}