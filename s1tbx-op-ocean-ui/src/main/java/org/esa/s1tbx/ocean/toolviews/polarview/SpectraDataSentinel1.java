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
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Spectra Data for Sentinel-1 L2 OCN
 */
public class SpectraDataSentinel1 extends SpectraDataBase implements SpectraData {

    private final MetadataElement annotation;
    private float minValue, maxValue;

    public SpectraDataSentinel1(final Product product) {
        super(product);

        //get general metadata
        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        annotation = root.getElement("annotation");

        numRecords = annotation.getNumElements() - 1;
        final RasterDataNode rasterNode = product.getBandAt(0);
        recordLength = rasterNode.getRasterWidth() * rasterNode.getRasterHeight();
    }

    /**
     * Get metadata specific to the record
     *
     * @param rec the current record
     * @return array of readouts
     * @throws Exception
     */
    public String[] getSpectraMetadata(final int rec) throws Exception {

        final MetadataElement[] elems = annotation.getElements();
        if (elems.length <= rec) {
            throw new Exception("OSW Record not found in product");
        }

        final MetadataElement recElem = elems[rec];

        final MetadataElement dimensions = recElem.getElement("Dimensions");
        numWLBins = dimensions.getAttributeInt("oswWavenumberBinSize", 0);
        numDirBins = dimensions.getAttributeInt("oswAngularBinSize", 0);

        firstDirBins = 0;
        dirBinStep = 10;
        firstWLBin = 800;
        lastWLBin = 30;

        final MetadataElement oswWindSpeed = recElem.getElement("oswWindSpeed");
        windSpeed = oswWindSpeed.getElement("Values").getAttributeDouble("data", 0);

        final MetadataElement oswWindDirection = recElem.getElement("oswWindDirection");
        windDirection = oswWindDirection.getElement("Values").getAttributeDouble("data", 0);

        final List<String> metadataList = new ArrayList<>();
        metadataList.add(createMetadataDouble("Wind Speed", recElem, "oswWindSpeed", null));
        metadataList.add(createMetadataDouble("Wind Direction", recElem, "oswWindDirection", "°"));
        metadataList.add(createMetadataDouble("Wind Sea Heights", recElem, "oswWindSeaHs", null));
        metadataList.add(createMetadataDouble("Wave Age", recElem, "oswWaveAge", ""));
        metadataList.add(createMetadataDouble("Az Cut-off Wavelength", recElem, "oswAzCutoff", null));
        metadataList.add(createMetadataDouble("Ra Cut-off Wavelength", recElem, "oswRaCutoff", null));

        metadataList.add(createMetadataDouble("Water Depth", recElem, "oswDepth", null));
        metadataList.add(createMetadataDouble("Incidence Angle", recElem, "oswIncidenceAngle", "°"));
        metadataList.add(createMetadataDouble("Backscatter", recElem, "oswNrcs", null));
        return metadataList.toArray(new String[metadataList.size()]);
    }

    private String createMetadataDouble(final String name, final MetadataElement recElem, final String elemName, String unit) {
        final MetadataElement elem = recElem.getElement(elemName);
        final double value = elem.getElement("Values").getAttributeDouble("data", 0);
        String valueStr = String.valueOf(value);
        if (value - (int) value != 0)
            valueStr = frmt.format(value);
        if (unit == null) {
            unit = elem.getAttributeString("units");
        }

        return name + ": " + valueStr + " " + unit;
    }

    public PolarData getPolarData(final int currentRec, final SpectraUnit spectraUnit) throws Exception {
        final boolean realBand = spectraUnit != SpectraUnit.IMAGINARY;
        spectrum = getSpectrum(currentRec, realBand);

        if (waveProductType == WaveProductType.WAVE_SPECTRA) {
            minValue = Float.MAX_VALUE;
            maxValue = Float.MIN_VALUE;
            for (int i = 0; i < spectrum.length; i++) {
                for (int j = 0; j < spectrum[0].length; j++) {
                    float val = spectrum[i][j];
                    if (spectraUnit == SpectraUnit.INTENSITY) {
                        val *= val;
                    }
                    spectrum[i][j] = val;
                    minValue = Math.min(minValue, val);
                    maxValue = Math.max(maxValue, val);
                }
            }
        } else if (waveProductType == WaveProductType.CROSS_SPECTRA) {
            if (spectraUnit == SpectraUnit.AMPLITUDE || spectraUnit == SpectraUnit.INTENSITY) {
                // complex data
                final float imagSpectrum[][] = getSpectrum(currentRec, false);
                minValue = Float.MAX_VALUE;
                maxValue = Float.MIN_VALUE;
                for (int i = 0; i < spectrum.length; i++) {
                    for (int j = 0; j < spectrum[0].length; j++) {
                        final float realVal = spectrum[i][j];
                        final float imagVal = imagSpectrum[i][j];
                        float val;
                        if (sign(realVal) == sign(imagVal)) {
                            val = realVal * realVal + imagVal * imagVal;
                        } else {
                            val = 0.0F;
                        }
                        if (spectraUnit == SpectraUnit.AMPLITUDE) {
                            val = (float) Math.sqrt(val);
                        }
                        spectrum[i][j] = val;
                        minValue = Math.min(minValue, val);
                        maxValue = Math.max(maxValue, val);
                    }
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

    private Band getBand(final int currentRec, final boolean getReal) throws Exception {
        for (Band band : product.getBands()) {
            try {
                String bandName = band.getName().toLowerCase();
                String bandRecNumStr = bandName.substring(3, 6);
                Integer bandRecNum = Integer.parseInt(bandRecNumStr);
                if (bandRecNum == currentRec + 1) {
                    if (waveProductType == WaveProductType.WAVE_SPECTRA && bandName.contains("oswpolspec")) {
                        return band;
                    } else if (waveProductType == WaveProductType.CROSS_SPECTRA) {
                        if (getReal && bandName.contains("oswqualitycrossspectrare")) {
                            return band;
                        } else if (!getReal && bandName.contains("oswqualitycrossspectraim")) {
                            return band;
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // continue
            }
        }
        throw new Exception("Band not found for record " + currentRec);
    }

    private float[][] getSpectrum(final int currentRec, final boolean getReal) throws Exception {

        final Band rasterNode = getBand(currentRec, getReal);
        rasterNode.loadRasterData();
        final float[] dataset = new float[rasterNode.getRasterWidth() * rasterNode.getRasterHeight()];
        rasterNode.getPixels(0, 0, rasterNode.getRasterWidth(), rasterNode.getRasterHeight(), dataset);

        minValue = (float) rasterNode.getStx().getMinimum();
        maxValue = (float) rasterNode.getStx().getMaximum();
        final float spectrum[][] = new float[numDirBins][numWLBins];

        int index = 0;
        for (int i = 0; i < numDirBins; i++) {
            for (int j = 0; j < numWLBins; j++) {
                spectrum[i][j] = dataset[index++];
            }
        }
        return spectrum;
    }

    public String[] updateReadouts(final double rTh[], final int currentRecord) {
        if (spectrum == null)
            return null;

        final float rStep = (float) (Math.log(lastWLBin) - Math.log(firstWLBin)) / (float) (numWLBins - 1);
        int wvBin = (int) (((rStep / 2.0 + Math.log(10000.0 / rTh[0])) - Math.log(firstWLBin)) / rStep);
        wvBin = Math.min(wvBin, spectrum[0].length - 1);
        final int wl = (int) Math.round(FastMath.exp((double) wvBin * rStep + Math.log(firstWLBin)));

        final float thFirst = firstDirBins + 5f;
        final float thStep = -dirBinStep;
        final int thBin = (int) ((((360.0 - rTh[1]) + (double) thFirst) % 360.0) / (double) (-thStep));
        final int element = thBin * spectrum[0].length + wvBin;
        final int direction = (int) (-((float) thBin * thStep + thStep / 2.0f + thFirst));

        final List<String> readoutList = new ArrayList<>(5);
        readoutList.add("Wavelength: " + wl + " m");
        readoutList.add("Direction: " + direction + " °");
        readoutList.add("Bin: " + (thBin + 1) + "," + (wvBin + 1) + " Element: " + element);
        readoutList.add("Value: " + spectrum[thBin][wvBin]);

        return readoutList.toArray(new String[readoutList.size()]);
    }
}
