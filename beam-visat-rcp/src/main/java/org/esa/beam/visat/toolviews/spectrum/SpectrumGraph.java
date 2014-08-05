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

package org.esa.beam.visat.toolviews.spectrum;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelModel;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.ui.diagram.AbstractDiagramGraph;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.IndexValidator;
import org.esa.beam.util.math.Range;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Comparator;


class SpectrumGraph extends AbstractDiagramGraph {

    private Placemark placemark;
    private Band[] bands;
    private double[] energies;
    private double[] wavelengths;
    private final Range energyRange;
    private final Range wavelengthRange;

    SpectrumGraph(Placemark placemark, Band[] bands) {
        Debug.assertNotNull(bands);
        this.placemark = placemark;
        this.bands = bands;
        energyRange = new Range();
        wavelengthRange = new Range();
        setBands(bands);
    }

    public Placemark getPlacemark() {
        return placemark;
    }

    @Override
    public String getXName() {
        return "Wavelength";
    }

    @Override
    public String getYName() {
        return placemark != null ? placemark.getLabel() : "Cursor";
    }

    @Override
    public int getNumValues() {
        return bands.length;
    }

    @Override
    public double getXValueAt(int index) {
        return wavelengths[index];
    }

    @Override
    public double getYValueAt(int index) {
        if (energies[index] == bands[index].getGeophysicalNoDataValue()) {
            return Double.NaN;
        }
        return energies[index];
    }

    @Override
    public double getXMin() {
        return wavelengthRange.getMin();
    }

    @Override
    public double getXMax() {
        return wavelengthRange.getMax();
    }

    @Override
    public double getYMin() {
        return energyRange.getMin();
    }

    @Override
    public double getYMax() {
        return energyRange.getMax();
    }

    public Band[] getBands() {
        return bands;
    }

    public void setBands(Band[] bands) {
        Debug.assertNotNull(bands);
        this.bands = bands.clone();
        Arrays.sort(this.bands, new Comparator<Band>() {
            @Override
            public int compare(Band band1, Band band2) {
                final float v = band1.getSpectralWavelength() - band2.getSpectralWavelength();
                return v < 0.0F ? -1 : v > 0.0F ? 1 : 0;
            }
        });
        if (wavelengths == null || wavelengths.length != this.bands.length) {
            wavelengths = new double[this.bands.length];
        }
        if (energies == null || energies.length != this.bands.length) {
            energies = new double[this.bands.length];
        }
        for (int i = 0; i < wavelengths.length; i++) {
            wavelengths[i] = this.bands[i].getSpectralWavelength();
            energies[i] = 0.0f;
        }
        Range.computeRangeDouble(wavelengths, IndexValidator.TRUE, wavelengthRange, ProgressMonitor.NULL);
        Range.computeRangeDouble(energies, IndexValidator.TRUE, energyRange, ProgressMonitor.NULL);
    }

    public void readValues() {
        Debug.assertNotNull(bands);
        for (int i = 0; i < bands.length; i++) {
            final Band band = bands[i];
            if (placemark != null) {
                // position of placemark is given in image (L0) coordinates
                // we have to transform them to the current level
                final MultiLevelModel multiLevelModel = ImageManager.getMultiLevelModel(band);
                final AffineTransform i2mTransform = multiLevelModel.getImageToModelTransform(0);
                final AffineTransform m2iTransform = multiLevelModel.getModelToImageTransform(0);
                final Point2D modelPixel = i2mTransform.transform(placemark.getPixelPos(), null);
                final Point2D imagePixel = m2iTransform.transform(modelPixel, null);
                int pixelX = (int) Math.floor(imagePixel.getX());
                int pixelY = (int) Math.floor(imagePixel.getY());
                energies[i] = getSample(band, pixelX, pixelY, 0);
            }
        }
        IndexValidator validator = new IndexValidator() {
            @Override
            public boolean validateIndex(int index) {
                return energies[index] != bands[index].getGeophysicalNoDataValue();
            }
        };
        Range.computeRangeDouble(energies, validator, energyRange, ProgressMonitor.NULL);
        // no invalidate() call here, SpectrumDiagram does this
    }

    private double getSample(Band band, int pixelX, int pixelY, int level) {
        return ProductUtils.getGeophysicalSampleDouble(band, pixelX, pixelY, level);
    }

    @Override
    public void dispose() {
        placemark = null;
        bands = null;
        energies = null;
        wavelengths = null;
        super.dispose();
    }
}
