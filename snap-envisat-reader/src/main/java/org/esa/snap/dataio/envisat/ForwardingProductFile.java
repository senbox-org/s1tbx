/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.ProductData;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;

/**
 * To be reused by specializations of {@link ProductFile}.
 *
 * @author Ralf Quast
 */
class ForwardingProductFile extends ProductFile {

    protected ForwardingProductFile(File file) throws IOException {
        super(file, new FileImageInputStream(file));
    }

    protected ForwardingProductFile(File file, ImageInputStream iis) throws IOException {
        super(file, iis);
    }

    @Override
    public ProductData.UTC getSceneRasterStartTime() {
        return null;
    }

    @Override
    public ProductData.UTC getSceneRasterStopTime() {
        return null;
    }

    @Override
    public int getSceneRasterWidth() {
        return 0;
    }

    @Override
    public int getSceneRasterHeight() {
        return 0;
    }

    @Override
    public float getTiePointGridOffsetX(int gridWidth) {
        return 0;
    }

    @Override
    public float getTiePointGridOffsetY(int gridWidth) {
        return 0;
    }

    @Override
    public float getTiePointSubSamplingX(int gridWidth) {
        return 0;
    }

    @Override
    public float getTiePointSubSamplingY(int gridWidth) {
        return 0;
    }

    @Override
    public boolean storesPixelsInChronologicalOrder() {
        return false;
    }

    @Override
    public Mask[] createDefaultMasks(String flagDsName) {
        return new Mask[0];
    }

    @Override
    void setInvalidPixelExpression(Band band) {
    }

    @Override
    public String getGADSName() {
        return null;
    }

    @Override
    public float[] getSpectralBandWavelengths() {
        return new float[0];
    }

    @Override
    public float[] getSpectralBandBandwidths() {
        return new float[0];
    }

    @Override
    public float[] getSpectralBandSolarFluxes() {
        return new float[0];
    }
}
