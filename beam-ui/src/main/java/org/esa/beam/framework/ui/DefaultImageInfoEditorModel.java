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

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.ImageInfo;

import java.awt.Color;

/**
 * Unstable interface. Do not use.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.5.1
 */
public class DefaultImageInfoEditorModel extends AbstractImageInfoEditorModel {

    public DefaultImageInfoEditorModel(ImageInfo imageInfo) {
        super(imageInfo);
        Assert.argument(imageInfo.getColorPaletteDef() != null, "imageInfo");
    }

    @Override
    public boolean isColorEditable() {
        return true;
    }

    @Override
    public int getSliderCount() {
        return getImageInfo().getColorPaletteDef().getNumPoints();
    }

    @Override
    public double getSliderSample(int index) {
        return getImageInfo().getColorPaletteDef().getPointAt(index).getSample();
    }

    @Override
    public void setSliderSample(int index, double sample) {
        getImageInfo().getColorPaletteDef().getPointAt(index).setSample(sample);
        fireStateChanged();
    }

    @Override
    public Color getSliderColor(int index) {
        return getImageInfo().getColorPaletteDef().getPointAt(index).getColor();
    }

    @Override
    public void setSliderColor(int index, Color color) {
        getImageInfo().getColorPaletteDef().getPointAt(index).setColor(color);
        fireStateChanged();
    }

    @Override
    public void createSliderAfter(int index) {
        final boolean b = getImageInfo().getColorPaletteDef().createPointAfter(index, getSampleScaling());
        if (b) {
            fireStateChanged();
        }
    }

    @Override
    public void removeSlider(int index) {
        getImageInfo().getColorPaletteDef().removePointAt(index);
        fireStateChanged();
    }

    @Override
    public Color[] createColorPalette() {
        return getImageInfo().getColorPaletteDef().createColorPalette(getSampleScaling());
    }

    @Override
    public boolean isGammaUsed() {
        return false;
    }

    @Override
    public double getGamma() {
        return 1;
    }

    @Override
    public void setGamma(double gamma) {
        // no support
    }

    @Override
    public byte[] getGammaCurve() {
        return null;
    }
}
