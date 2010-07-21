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

import org.esa.beam.dataio.landsat.LandsatConstants.NomRadiance;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * The class <code>RadiometricData</code> is used to store the data of the radiometric calibration
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public final class RadiometricData {

    private static final int BEFORE1990 = 1;
    private static final int AFTER1990 = 0;

    private static RadiometricData instance = null;

    private double[] maxRadiance;                 // in milliwatts/(square cm-steradian)
    private double[] minRadiance;                 // = BIAS
    private final int [] bandsPresent;

    /**
     * @param maxRadOffset   an array with the offsets (you will find them in the specification) of all max Radiance values.
     * @param minRadOffset   an array with the offsets (you will find them in the specification) of all min Radiance values.
     * @param size           the size of the elementary radiance value
     * @param input          the inputstream of the header file where the radiance values are stored
     * @param bandsAvailable the indices of the stored band
     *
     * @throws IOException if the arrays (<code>maxRadOffset</code> and <code>minRadOffset</code>) have a different array size the shortest size will be chosen for both arrays.
     *                     The extended data will be cut off.
     */
    private RadiometricData(final int[] maxRadOffset, final int [] minRadOffset, final int size,
                            final LandsatImageInputStream input, int[] bandsAvailable) throws
                                                                                       IOException {
        Guardian.assertTrue("maxRadOffset.length == minRadOffset.length", maxRadOffset.length == minRadOffset.length);
        Guardian.assertTrue("maxRadOffset.length >= bandsAvailable.length",
                            maxRadOffset.length >= bandsAvailable.length);
        final int length = bandsAvailable.length;
        maxRadiance = new double[length];
        minRadiance = new double[length];
        bandsPresent = bandsAvailable;

        for (int i = 0; i < length; i++) {
            minRadiance[i] = Double.parseDouble(
                    LandsatUtils.getValueFromLandsatFile(input, minRadOffset[i], size));
            maxRadiance[i] = Double.parseDouble(
                    LandsatUtils.getValueFromLandsatFile(input, maxRadOffset[i], size));
        }

        List nominalRadVals = LandsatConstants.NomRadiance.getAfter2003Radiances();

        if (isGainBias(maxRadiance, minRadiance, nominalRadVals)) {
            maxRadiance = calculateLmax(maxRadiance, minRadiance, AFTER1990);
        } else if (isLmaxLmin(maxRadiance, minRadiance, nominalRadVals)) {
            maxRadiance = trans2DefaultForm(maxRadiance);
            minRadiance = trans2DefaultForm(minRadiance);
        } else {
            Debug.trace("The found radiance data can't be read in");
            maxRadiance = null;
            minRadiance = null;
        }
    }

    /**
     * TODO if only thermal band is present
     *
     * @param maxRad
     * @param minRad
     * @param nominalRadVals
     *
     * @return <code>true</code> if the stored data in the radiance fields are minimal and maximal radiance values
     */
    private boolean isLmaxLmin(final double[] maxRad, final double[] minRad, List nominalRadVals) {
        Guardian.assertTrue("maxRad.length == minRad.length", maxRad.length == minRad.length);
        double [] nominalLmax;
        double [] nominalLmin;

        if (isThermalBandpresent(bandsPresent)) {
            final int[] bandsWithoutThermal = getBandsWithoutThermal();
            nominalLmax = getNomLmax(nominalRadVals, bandsWithoutThermal);
            nominalLmin = getNomLmin(nominalRadVals, bandsWithoutThermal);
            double[] maxRadWithoutThermal = getRadianceWithoutThermal(trans2DefaultForm(maxRad));
            double[] minRadWithoutThermal = getRadianceWithoutThermal(trans2DefaultForm(minRad));

            if (getDistance(nominalLmin, minRadWithoutThermal) < 0.5) {
                if (getDistance(nominalLmax, maxRadWithoutThermal) < 10) {
                    return true;
                }
            }
            return false;
        } else {
            nominalLmax = getNomLmax(nominalRadVals, bandsPresent);
            nominalLmin = getNomLmin(nominalRadVals, bandsPresent);
            if (getDistance(nominalLmin, minRad) < 0.5) {
                if (getDistance(nominalLmax, maxRad) < 0.5) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * @return <code>true</code> if there are valide geometric data are available in the landsat product
     */
    public final boolean isEnabled() {
        return !(maxRadiance == null || minRadiance == null);
    }

    /**
     * checks if the found data are with a big likelihood GAIN BIAS data
     * note! Lmin = BIAS
     *
     * @param maxRad         the found value at the maxRad field
     * @param minRad         the found value at the minRad fiedl
     * @param nominalRadVals
     *
     * @return <code>true</code> if the founded values are Gain Bias (Lmin) values <code>false</code> if the found data are not Gain/Bias values
     */
    private boolean isGainBias(final double[] maxRad, final double[] minRad, List nominalRadVals) {

        double [] nominalGain = getNomGains(nominalRadVals, bandsPresent);
        double [] nominalLmin = getNomLmin(nominalRadVals, bandsPresent);

        if (getDistance(nominalGain, maxRad) < 0.5) {
            if (getDistance(nominalLmin, minRad) < 0.5) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param maxRadOffset   an array with the offsets (you will find them in the specification) of all max Radiance values.
     * @param minRadOffset   an array with the offsets (you will find them in the specification) of all min Radiance values.
     * @param size           the size of the elementary radiance value
     * @param input          the inputstream of the header file where the radiance values are stored
     * @param bandsAvailable
     *
     * @return an instance with the radiometric data values, stored in the header file. Returns <code>null</code> if the <code>maxRadOffset</code> and the <code>minRadOffset</code> arrays haven't the same size.
     *
     * @throws IOException creates a complete radiometric object
     */
    public static RadiometricData createRadiometricData(final int[] maxRadOffset, final int [] minRadOffset,
                                                        final int size, final LandsatImageInputStream input,
                                                        final int [] bandsAvailable) throws
                                                                                     IOException {

        if (maxRadOffset.length == minRadOffset.length) {
            if (instance == null) {
                instance = new RadiometricData(maxRadOffset, minRadOffset, size, input, bandsAvailable);
                return instance;
            }
            return instance;
        } else {
            return null;
        }
    }

    /**
     * @return the instance value, if the Radiometric data weren't created before the function returns <code>null</code>
     *         Singleton access.
     */
    public static RadiometricData getRadiometricData() {
        return instance;
    }

    /**
     * @param bandnumber
     *
     * @return the max Radiance Value for the given bandnumber
     */
    public final double getMaxRadianceAt(final int bandnumber) {
        return maxRadiance[bandnumber];
    }

    /**
     * @param bandnumber
     *
     * @return minRadValue the min Radiance Value for the the given bandnumber
     */
    public final double getMinRadianceAt(final int bandnumber) {
        return minRadiance[bandnumber];
    }

    /**
     * @param radiances
     *
     * @return a array of radiances in the right unit
     */
    private double[] trans2DefaultForm(final double[] radiances) {
        return trans2DefaultForm(radiances, bandsPresent);
    }

    /**
     * @param radiances
     * @param bandsPresents
     *
     * @return a array of radiances in the right unit
     */
    private static double[] trans2DefaultForm(final double[] radiances, final int [] bandsPresents) {
        double [] transRad = new double[bandsPresents.length];
        for (int i = 0; i < radiances.length; i++) {
            LandsatConstants.ConstBand band = LandsatConstants.ConstBand.getConstantBandAt(bandsPresents[i]);
            transRad[i] = ((radiances[i] * 10000) / band.getBandwidth());
        }
        return transRad;
    }

    /**
     * @param nominalRadVals
     * @param bandsPresent   an array with the indices of the present bands on the storeage medium
     *
     * @return the nominal Gain values of the present bands
     */
    private static double[]getNomGains(final List nominalRadVals, final int [] bandsPresent) {
        double nomGains[] = new double[bandsPresent.length];
        int i = 0;
        for (int j = 0; j < bandsPresent.length; j++) {
            for (Iterator iter = nominalRadVals.iterator(); iter.hasNext();) {
                NomRadiance element = (NomRadiance) iter.next();
                if (element.getBandNumber() == bandsPresent[j]) {
                    nomGains[i] = element.getGain();
                    i++;
                }
            }
        }
        return nomGains;
    }

    /**
     * TODO
     *
     * @param nominalRadVals
     * @param bandsPresent   an array with the indices of the present bands on the storeage medium
     *
     * @return the nominal Lmin values of the present bands
     */
    private static double[]getNomLmin(final List nominalRadVals, final int [] bandsPresent) {
        double nomLmin[] = new double[bandsPresent.length];
        int i = 0;

        for (int j = 0; j < bandsPresent.length; j++) {
            for (Iterator iter = nominalRadVals.iterator(); iter.hasNext();) {
                NomRadiance element = (NomRadiance) iter.next();
                if (element.getBandNumber() == bandsPresent[j]) {
                    nomLmin[i] = element.getLmin();
                    i++;
                }
            }
        }
        return nomLmin;
    }

    /**
     * TODO
     *
     * @param nominalRadVals
     * @param bandsPresent   an array with the indices of the present bands on the storeage medium
     *
     * @return the nominal Lmax values of the present bands
     */
    private static double[]getNomLmax(final List nominalRadVals, final int [] bandsPresent) {
        double nomLmax[] = new double[bandsPresent.length];
        int i = 0;
        for (int j = 0; j < bandsPresent.length; j++) {
            for (Iterator iter = nominalRadVals.iterator(); iter.hasNext();) {
                NomRadiance element = (NomRadiance) iter.next();
                if (bandsPresent[j] == element.getBandNumber()) {
                    nomLmax[i] = element.getLmax();
                    i++;
                }
            }
        }
        return nomLmax;
    }

    /**
     * @param foundRadiometricValues
     * @param nominalRadiometricValues
     *
     * @return caluclates the euclidian distance between found and nominal values to classifier if the values stored in the header are bias, gain, lmin, lmax
     */
    private static double getDistance(final double[] foundRadiometricValues, final double[] nominalRadiometricValues) {
        Guardian.assertTrue("foundRadiometricValues.length == nominalRadiometricValues.length",
                            foundRadiometricValues.length == nominalRadiometricValues.length);
        double distance = 0;
        for (int i = 0; i < nominalRadiometricValues.length; i++) {
            //euclidian distance
            distance = distance + Math.pow(foundRadiometricValues[i] - nominalRadiometricValues[i], 2);
        }
        return Math.sqrt(distance);
    }

    /**
     * @param gain
     * @param Lmin
     * @param formularUsed
     *
     * @return an array with calculated maximale radiances from the given gain and minimal radiance values.
     */
    private static double [] calculateLmax(final double [] gain, final double [] Lmin, int formularUsed) {
        Guardian.assertTrue("Lmin.length == gain.length", Lmin.length == gain.length);
        Guardian.assertTrue("formularUsed == AFTER1990 || formularUsed == BEFORE1990",
                            formularUsed == AFTER1990 || formularUsed == BEFORE1990);
        double [] Lmax = new double [Lmin.length];
        for (int i = 0; i < Lmax.length; i++) {
            if (formularUsed == AFTER1990) {
                Lmax[i] = gain[i] * 255 + Lmin[i];
            } else if (formularUsed == BEFORE1990) {
                Lmax[i] = gain[i] * 254 + (254 * Lmin[i]) / 255;
            }
        }
        return Lmax;
    }

    /**
     * @param bandspresent
     *
     * @return <code>true</code> if thermal band is present on the medium, <code>false</code> if the thermal band doesn't exist on the medium
     */
    private static boolean isThermalBandpresent(final int [] bandspresent) {
        for (int i = 0; i < bandspresent.length; i++) {
            if (bandspresent[i] == 6) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the collection of all bands present without the thermal band
     */
    private int[] getBandsWithoutThermal() {
        int [] withoutThermal = new int[bandsPresent.length - 1];
        int j = 0;
        for (int i = 0; i < bandsPresent.length; i++) {
            if (bandsPresent[i] != 6) {
                withoutThermal[j++] = bandsPresent[i];
            }
        }
        return withoutThermal;
    }

    /**
     * TODO
     *
     * @param radiances
     *
     * @return an array with all radiances of the Landsat TM bands but without the thermal band
     */
    private double[] getRadianceWithoutThermal(final double [] radiances) {
        double [] radianceWithoutThermal = new double [bandsPresent.length - 1];
        int j = 0;
        for (int i = 0; i < bandsPresent.length; i++) {
            if (bandsPresent[i] != 6) {
                radianceWithoutThermal[j++] = radiances[i];
            }
        }
        return radianceWithoutThermal;
    }
}
