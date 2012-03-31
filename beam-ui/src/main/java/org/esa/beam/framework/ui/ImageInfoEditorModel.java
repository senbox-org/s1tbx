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

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Scaling;
import org.esa.beam.framework.datamodel.Stx;

import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Unstable interface. Do not use.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.5.1
 */
public interface ImageInfoEditorModel {
    ImageInfo getImageInfo();

    void setDisplayProperties(String name, String unit, Stx stx, Scaling scaling);

    boolean isColorEditable();

    int getSliderCount();

    double getSliderSample(int index);

    void setSliderSample(int index, double sample);

    Color getSliderColor(int index);

    void setSliderColor(int index, Color color);

    void createSliderAfter(int index);

    void removeSlider(int removeIndex);

    Color[] createColorPalette();

    boolean isGammaUsed();

    double getGamma();

    void setGamma(double gamma);

    byte[] getGammaCurve();

    String getParameterName();

    String getParameterUnit();

    Scaling getSampleScaling();

    void setSampleScaling(Scaling scaling);

    Stx getSampleStx();

    double getMinSample();

    double getMaxSample();

    boolean isHistogramAvailable();

    int[] getHistogramBins();

    double getMinHistogramViewSample();

    void setMinHistogramViewSample(double minViewSample);

    double getMaxHistogramViewSample();

    void setMaxHistogramViewSample(double maxViewSample);

    double getHistogramViewGain();

    void setHistogramViewGain(double gain);

    void addChangeListener(ChangeListener l);

    void removeChangeListener(ChangeListener l);

    void fireStateChanged();
}
