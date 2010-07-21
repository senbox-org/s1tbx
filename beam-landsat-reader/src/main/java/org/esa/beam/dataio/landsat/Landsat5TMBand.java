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

package org.esa.beam.dataio.landsat;

import org.esa.beam.util.Guardian;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The class <code>Landsat5TMBand</code> is used to store the preferences of one Landsat 5 TM Band
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public final class Landsat5TMBand implements LandsatTMBand {

    private static final int BEFORE1990 = 1;
    private static final int AFTER1990 = 0;

    private final int bandNumber;
    private final LandsatConstants.ConstBand bandConstants;
    private final LandsatConstants.NomRadiance radBefore2003Constants;
    private final LandsatConstants.NomRadiance radAfter2003Constants;
    private final Object imageSource;
    private double maxRadiance;
    private double minRadiance;
    private final LandsatTMFile file;

    /**
     * @param index      the index in the array
     * @param bandNumber the index number of the Band in the Product
     * @param source     the IO Source where the data is comeing from (legal are File or ZipEntry objects)
     * @param radData
     * @param file
     */
    public Landsat5TMBand(final int index, final int bandNumber, final Object source, final RadiometricData radData,
                          final LandsatTMFile file) {
        this.file = file;
        this.bandNumber = bandNumber;
        imageSource = source;
        bandConstants = LandsatConstants.ConstBand.getConstantBandAt(bandNumber);
        radBefore2003Constants = LandsatConstants.NomRadiance.getConstantRadAt(bandNumber,
                                                                               LandsatConstants.NomRadiance.BEFORE_5_MAY_2003);
        radAfter2003Constants = LandsatConstants.NomRadiance.getConstantRadAt(bandNumber,
                                                                              LandsatConstants.NomRadiance.AFTER_5_MAY_2003);
        maxRadiance = 0;
        minRadiance = 0;

        if (radData != null) {
            if (radData.isEnabled()) {
                maxRadiance = radData.getMaxRadianceAt(index);
                minRadiance = radData.getMinRadianceAt(index);
            }
        }
    }

    /**
     * @return the source (File or Zipentry)
     */
    public Object getInputSource() {
        return imageSource;
    }

    /**
     * @return the name of the band
     */
    public final String getBandName() {
        return bandConstants.toString();
    }

    /**
     * @return the description
     */
    public final String getBandDescription() {
        return bandConstants.getDescription();
    }

    /**
     * @return index of the band
     */
    public final int getIndex() {
        return bandNumber;
    }

    public final int getResolution() {
        return bandConstants.getResolution();
    }

    /**
     * @return band's bandwith
     */
    public final float getBandwidth() {
        return bandConstants.getBandwidth();
    }

    /**
     * @return the band's wavelength
     */
    public final float getWavelength() {
        return bandConstants.getWavelength();
    }

    /**
     * @return maximal radiance value
     */
    public final double getMaxRadiance() {
        return maxRadiance;
    }

    /**
     * @return minimal radiance value
     */
    public final double getMinRadiance() {
        return minRadiance;
    }

    /**
     * @return the former nominal max. radiance value
     */
    public final double getFormerNomMaxRadiance() {
        return radBefore2003Constants.getLmax();
    }

    /**
     * @return the former nominal min. Radiance value
     */
    public final double getFormerNomMinRadiance() {
        return radBefore2003Constants.getLmin();
    }

    /**
     * @return the former nominal gain value
     */
    public final double getFormerNominalGain() {
        return radBefore2003Constants.getGain();
    }

    /**
     * @return the actual nominal max. radiance value
     */
    public final double getNewerNomMaxRadiance() {
        return radAfter2003Constants.getLmax();
    }

    /**
     * @return the actual nominal min. radiance value (since 2003)
     */
    public final double getNewerNomMinRadiance() {
        return radAfter2003Constants.getLmin();
    }

    /**
     * @return the actual nominal Gain value (since 2003)
     */
    public final double getNewerNomGain() {
        return radAfter2003Constants.getGain();
    }

    /**
     * @return gain value of the band
     */
    public final double getGain() {
        return calculateGain(minRadiance, maxRadiance, AFTER1990);
    }

    /**
     * @return Bias value of the band
     */
    public final double getBias() {
        return minRadiance;
    }

    /**
     * @return name of band
     */
    @Override
    public final String toString() {
        return bandConstants.toString();
    }

    /**
     * @return Stream of a Landsat5TM image
     *
     * @throws IOException
     */
    public final ImageInputStream createStream() throws
                                                 IOException {

        final Object bandSource = imageSource;
        ImageInputStream inputstream = null;

        if (bandSource instanceof File) {
            final File file = (File) bandSource;
            inputstream = new FileImageInputStream(file);
        }
        if (bandSource instanceof ZipEntry) {
            final ZipEntry entry = (ZipEntry) bandSource;
            ZipFile zipFile = new ZipFile(file.getInputFile());
            final InputStream standardInputStream = zipFile.getInputStream(entry);
            inputstream = ImageIO.createImageInputStream(standardInputStream);
        }
        return inputstream;
    }

    /**
     * @return band's solar flux value
     */
    public final float getSolarFlux() {
        return bandConstants.getSolarFlux();
    }

    /**
     * @param lMin
     * @param lMax
     * @param formular constant value to classify the used formular to calculate the correct gain value of the band
     *
     * @return gain value of the band
     */
    private static double calculateGain(final double lMin, final double lMax, final int formular) {

        Guardian.assertTrue("formular == AFTER1990 || formular == BEFORE1990",
                            formular == AFTER1990 || formular == BEFORE1990);
        double gain = 0;
        if (formular == AFTER1990) {
            gain = ((lMax - lMin) / 255);
        }
        if (formular == BEFORE1990) {
            gain = (lMax / 254) - (lMin / 244);
        }

        return gain;
    }

    /**
     * @return <code>true</code> if the band is a thermal band and <code>false</code> if the band is a spectral band
     */
    public final boolean isThermal() {
        return bandNumber == 6;
    }
}
