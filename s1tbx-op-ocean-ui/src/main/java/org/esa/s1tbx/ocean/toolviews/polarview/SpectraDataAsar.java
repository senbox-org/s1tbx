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
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.RasterDataNode;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Spectra Data for ASAR L2 WV
 */
public class SpectraDataAsar extends SpectraDataBase implements SpectraData {

    private MetadataElement spectraMetadataRoot = null;
    private ProductData.UTC zeroDopplerTime = null;

    protected double minSpectrum = 0;
    protected double maxSpectrum = 255;
    protected double minReal = 0;
    protected double maxReal = 0;
    protected double minImaginary = 0;
    protected double maxImaginary = 0;

    private double maxSpecDir = 0;
    private double maxSpecWL = 0;

    private double sarWaveHeight = 0;
    private double sarAzShiftVar = 0;
    private double backscatter = 0;

    public SpectraDataAsar(final Product product, final WaveProductType waveProductType) {
        super(product, waveProductType);

        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement sph = root.getElement("SPH");
        numDirBins = sph.getAttributeInt("NUM_DIR_BINS", 0);
        numWLBins = sph.getAttributeInt("NUM_WL_BINS", 0);
        firstDirBins = (float) sph.getAttributeDouble("FIRST_DIR_BIN", 0);
        dirBinStep = (float) sph.getAttributeDouble("DIR_BIN_STEP", 0);
        firstWLBin = (float) sph.getAttributeDouble("FIRST_WL_BIN", 0);
        lastWLBin = (float) sph.getAttributeDouble("LAST_WL_BIN", 0);

        if (waveProductType == WaveProductType.WAVE_SPECTRA) {
            spectraMetadataRoot = root.getElement("OCEAN_WAVE_SPECTRA_MDS");
        } else {
            spectraMetadataRoot = root.getElement("CROSS_SPECTRA_MDS");
        }
    }

    public float getMinValue(final boolean real) {
        if (waveProductType == SpectraData.WaveProductType.WAVE_SPECTRA) {
            return (float) minSpectrum;
        } else {
            return real ? (float) minReal : (float) minImaginary;
        }
    }

    public float getMaxValue(final boolean real) {
        if (waveProductType == SpectraData.WaveProductType.WAVE_SPECTRA) {
            return (float) maxSpectrum;
        } else {
            return real ? (float) maxReal : (float) maxImaginary;
        }
    }

    public String[] getSpectraMetadata(int rec) {
        try {
            final String elemName = spectraMetadataRoot.getName() + '.' + (rec + 1);
            final MetadataElement spectraMetadata = spectraMetadataRoot.getElement(elemName);

            zeroDopplerTime = spectraMetadata.getAttributeUTC("zero_doppler_time");
            maxSpecDir = spectraMetadata.getAttributeDouble("spec_max_dir", 0);
            maxSpecWL = spectraMetadata.getAttributeDouble("spec_max_wl", 0);

            if (waveProductType == WaveProductType.WAVE_SPECTRA) {
                minSpectrum = spectraMetadata.getAttributeDouble("min_spectrum", 0);
                maxSpectrum = spectraMetadata.getAttributeDouble("max_spectrum", 255);
                windSpeed = spectraMetadata.getAttributeDouble("wind_speed", 0);
                windDirection = spectraMetadata.getAttributeDouble("wind_direction", 0);
                sarWaveHeight = spectraMetadata.getAttributeDouble("SAR_wave_height", 0);
                sarAzShiftVar = spectraMetadata.getAttributeDouble("SAR_az_shift_var", 0);
                backscatter = spectraMetadata.getAttributeDouble("backscatter", 0);
            } else {
                minReal = spectraMetadata.getAttributeDouble("min_real", 0);
                maxReal = spectraMetadata.getAttributeDouble("max_real", 255);
                minImaginary = spectraMetadata.getAttributeDouble("min_imag", 0);
                maxImaginary = spectraMetadata.getAttributeDouble("max_imag", 255);
            }
        } catch (Exception e) {
            System.out.println("Unable to get metadata for " + spectraMetadataRoot.getName());
        }

        final DecimalFormat frmt = new DecimalFormat("0.0000");

        final List<String> metadataList = new ArrayList<>(10);
        metadataList.add("Time: " + zeroDopplerTime.toString());
        metadataList.add("Peak Direction: " + maxSpecDir + " deg");
        metadataList.add("Peak Wavelength: " + frmt.format(maxSpecWL) + " m");

        if (waveProductType == WaveProductType.WAVE_SPECTRA) {
            metadataList.add("Min Spectrum: " + frmt.format(minSpectrum));
            metadataList.add("Max Spectrum: " + frmt.format(maxSpectrum));

            metadataList.add("Wind Speed: " + windSpeed + " m/s");
            metadataList.add("Wind Direction: " + windDirection + " deg");
            metadataList.add("SAR Swell Wave Height: " + frmt.format(sarWaveHeight) + " m");
            metadataList.add("SAR Azimuth Shift Var: " + frmt.format(sarAzShiftVar) + " m^2");
            metadataList.add("Backscatter: " + frmt.format(backscatter) + " dB");
        }

        return metadataList.toArray(new String[metadataList.size()]);
    }

    public float[][] getSpectrum(int imageNum, int currentRec, boolean getReal) {

        float[] dataset;
        try {
            final RasterDataNode rasterNode = product.getBandAt(imageNum);
            rasterNode.loadRasterData();
            dataset = new float[recordLength];
            rasterNode.getPixels(0, currentRec, recordLength, 1, dataset);

        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }

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

    public PolarData getPolarData(final int currentRec, final SpectraUnit spectraUnit) {
        final boolean realBand = spectraUnit != SpectraUnit.IMAGINARY;
        final float[][] spectrum = getSpectrum(0, currentRec, realBand);

        float minValue = getMinValue(realBand);
        float maxValue = getMaxValue(realBand);

        if (waveProductType == SpectraData.WaveProductType.WAVE_SPECTRA) {
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
        } else if (spectraUnit == SpectraUnit.AMPLITUDE || spectraUnit == SpectraUnit.INTENSITY) {
            // complex data
            final float imagSpectrum[][] = getSpectrum(1, currentRec, false);
            minValue = Float.MAX_VALUE;
            maxValue = Float.MIN_VALUE;
            for (int i = 0; i < spectrum.length; i++) {
                for (int j = 0; j < spectrum[0].length; j++) {
                    final float realVal = spectrum[i][j];
                    final float imagVal = imagSpectrum[i][j];
                    float val;
                    if (sign(realVal) == sign(imagVal))
                        val = realVal * realVal + imagVal * imagVal;
                    else
                        val = 0.0F;
                    if (spectraUnit == SpectraUnit.AMPLITUDE)
                        val = (float) Math.sqrt(val);
                    spectrum[i][j] = val;
                    minValue = Math.min(minValue, val);
                    maxValue = Math.max(maxValue, val);
                }
            }
        }

        final float rStep = (float) (Math.log(lastWLBin) - Math.log(firstWLBin)) / (float) (numWLBins - 1);
        double logr = Math.log(firstWLBin) - (rStep / 2.0);

        final float thFirst;
        final float thStep;
        if (waveProductType == SpectraData.WaveProductType.WAVE_SPECTRA) {
            thFirst = firstDirBins + 5f;
            thStep = -dirBinStep;
        } else {
            thFirst = firstDirBins - 5f;
            thStep = dirBinStep;
        }

        final float radii[] = new float[spectrum[0].length + 1];
        for (int j = 0; j <= spectrum[0].length; j++) {
            radii[j] = (float) (10000.0 / FastMath.exp(logr));
            logr += rStep;
        }

        return new PolarData(spectrum, 90f + thFirst, thStep, radii, minValue, maxValue);
    }

    private static int sign(final float f) {
        return f < 0.0F ? -1 : 1;
    }

    public String[] updateReadouts(final double rTh[], final int currentRecord) {
        if (spectrum == null)
            return null;

        final float thFirst;
        final int thBin;
        final float thStep;
        final int element;
        final int direction;

        final float rStep = (float) (Math.log(lastWLBin) - Math.log(firstWLBin)) / (float) (numWLBins - 1);
        int wvBin = (int) (((rStep / 2.0 + Math.log(10000.0 / rTh[0])) - Math.log(firstWLBin)) / rStep);
        wvBin = Math.min(wvBin, spectrum[0].length - 1);
        final int wl = (int) Math.round(FastMath.exp((double) wvBin * rStep + Math.log(firstWLBin)));

        if (waveProductType == WaveProductType.CROSS_SPECTRA) {
            thFirst = firstDirBins - 5f;
            thStep = dirBinStep;
            thBin = (int) (((rTh[1] - (double) thFirst) % 360.0) / (double) thStep);
            element = (thBin % (spectrum.length / 2)) * spectrum[0].length + wvBin;
            direction = (int) ((float) thBin * thStep + thStep / 2.0f + thFirst);
        } else {
            thFirst = firstDirBins + 5f;
            thStep = -dirBinStep;
            thBin = (int) ((((360.0 - rTh[1]) + (double) thFirst) % 360.0) / (double) (-thStep));
            element = thBin * spectrum[0].length + wvBin;
            direction = (int) (-((float) thBin * thStep + thStep / 2.0f + thFirst));
        }

        final List<String> readoutList = new ArrayList<>(5);
        readoutList.add("Record: " + (currentRecord + 1) + " of " + (numRecords + 1));
        readoutList.add("Wavelength: " + wl + " m");
        readoutList.add("Direction: " + direction + " deg");
        readoutList.add("Bin: " + (thBin + 1) + "," + (wvBin + 1) + " Element: " + element);
        readoutList.add("Value: " + spectrum[thBin][wvBin]);

        return readoutList.toArray(new String[readoutList.size()]);
    }
}
