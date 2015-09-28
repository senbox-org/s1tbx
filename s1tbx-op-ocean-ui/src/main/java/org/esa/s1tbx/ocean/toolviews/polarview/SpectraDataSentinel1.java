/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.ocean.toolviews.polarview;

import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.ocean.toolviews.polarview.polarplot.PolarData;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.RasterDataNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Spectra Data for Sentinel-1 L2 OCN
 */
public class SpectraDataSentinel1 extends SpectraDataBase implements SpectraData {

    private final MetadataElement annotation;

    public SpectraDataSentinel1(final Product product, final WaveProductType waveProductType) {
        super(product, waveProductType);

        //get general metadata
        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        annotation = root.getElement("annotation");

        numRecords = 64; //todo
        final RasterDataNode rasterNode = product.getBandAt(0);
        recordLength = rasterNode.getRasterWidth() * rasterNode.getRasterHeight();
    }

    /**
     * Get metadata specific to the record
     * @param rec the current record
     * @return array of readouts
     * @throws Exception
     */
    public String[] getSpectraMetadata(final int rec) throws Exception {

        final MetadataElement[] elems = annotation.getElements();
        if(elems.length <= rec) {
            throw new Exception("OSW Record not found in product");
        }

        final MetadataElement recElem = elems[rec];

        final List<String> metadataList = new ArrayList<>();
        final MetadataElement dimensions = recElem.getElement("Dimensions");

        numWLBins = dimensions.getAttributeInt("oswWavenumberBinSize", 0);
        numDirBins = dimensions.getAttributeInt("oswAngularBinSize", 0);
        firstDirBins = 0;
        dirBinStep = 10;
        firstWLBin = 800;
        lastWLBin = 30;

        return metadataList.toArray(new String[metadataList.size()]);
    }

    public PolarData getPolarData(final int currentRec, final SpectraUnit spectraUnit) throws Exception {
        final boolean realBand = true;
        final float[][] spectrum = getSpectrum(0, currentRec, realBand);

        float minValue = getMinValue(realBand);
        float maxValue = getMaxValue(realBand);

        if (spectraUnit == SpectraUnit.INTENSITY) {
            minValue = Float.MAX_VALUE;
            maxValue = Float.MIN_VALUE;
            for (int i = 0; i < spectrum.length; i++) {
                for (int j = 0; j < spectrum[0].length; j++) {
                    final float realVal = spectrum[i][j];
                    final float val = realVal * realVal;
                    spectrum[i][j] = val;
                    minValue = Math.min(minValue, val);
                    maxValue = Math.max(maxValue, val);
                }
            }
        }

        final float rStep = (float) (Math.log(lastWLBin) - Math.log(firstWLBin)) / (float) (numWLBins - 1);
        double logr = Math.log(firstWLBin) - (rStep / 2.0);

        final float thFirst = firstDirBins + 5f;
        final float thStep = -dirBinStep;

        final float radii[] = new float[spectrum[0].length + 1];
        for (int j = 0; j <= spectrum[0].length; j++) {
            radii[j] = (float) (10000.0 / FastMath.exp(logr));
            logr += rStep;
        }

        return new PolarData(spectrum, 90f + thFirst, thStep, radii, minValue, maxValue);
    }

    private float[][] getSpectrum(final int imageNum, final int currentRec, final boolean getReal) throws Exception {

        final RasterDataNode rasterNode = product.getBandAt(currentRec);
        rasterNode.loadRasterData();
        final float[] dataset = new float[rasterNode.getRasterWidth() * rasterNode.getRasterHeight()];
        rasterNode.getPixels(0, 0, rasterNode.getRasterWidth(), rasterNode.getRasterHeight(), dataset);

        final float minValue = getMinValue(getReal);
        final float maxValue = getMaxValue(getReal);
        final float scale = (maxValue - minValue) / 255f;
        final float spectrum[][] = new float[numDirBins][numWLBins];

        int index = 0;
        if (waveProductType == WaveProductType.WAVE_SPECTRA) {
            for (int i = 0; i < numDirBins; i++) {
                for (int j = 0; j < numWLBins; j++) {
                    spectrum[i][j] = dataset[index++] * scale + minValue;
                }
            }
        } else {
            final int Nd2 = numDirBins / 2;
            for (int i = 0; i < Nd2; i++) {
                for (int j = 0; j < numWLBins; j++) {
                    spectrum[i][j] = dataset[index++] * scale + minValue;
                }
            }

            if (getReal) {
                for (int i = 0; i < Nd2; i++) {
                    System.arraycopy(spectrum[i], 0, spectrum[i + Nd2], 0, numWLBins);
                }
            } else {
                for (int i = 0; i < Nd2; i++) {
                    for (int j = 0; j < numWLBins; j++) {
                        spectrum[i + Nd2][j] = -spectrum[i][j];
                    }
                }
            }
        }
        return spectrum;
    }

    public String[] updateReadouts(final double rTh[], final int currentRecord) {
        return null;
    }

    public float getMinValue(final boolean real) {
        return 0;
    }

    public float getMaxValue(final boolean real) {
        return 170;
    }
}
