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
package org.esa.beam.processor.flh_mci;

import org.esa.beam.framework.gpf.OperatorException;

/**
 * Implements a general baseline height algorithm with optional baseline slope calculation.
 */
public final class BaselineAlgorithm {

    public static final float DEFAULT_CLOUD_CORRECT = 1.005f;
    private double lambdaFactor;
    private double inverseDelta;
    private double cloudCorrectionFactor;

    /**
     * Constructs the object with default parameters
     */
    public BaselineAlgorithm() {
        lambdaFactor = 1.0;
        inverseDelta = 1.0;
        cloudCorrectionFactor = DEFAULT_CLOUD_CORRECT;
    }

    /**
     * Sets the center wavelengths of the low- and high baseline bands and of the signal band to be used during
     * calculation.
     *
     * @param low    lower band wavelength in nm
     * @param high   higher band wavelength in nm
     * @param signal signal band wavelength in nm
     */
    public final void setWavelengths(float low, float high, float signal) throws OperatorException {
        double num;
        double denom;

        // check for correct wavelengths
        // -----------------------------
        if (low < 0.f || high < 0.f || signal < 0.f) {
            throw new OperatorException("Negative wavelengths");
        }

        // set numerator and check for validity
        // ------------------------------------
        num = signal - low;
        if (num == 0.0) {
            throw new OperatorException("Numerator is 0, low and signal wavelength are identical!");
        }

        // set denominator and check for validity
        // --------------------------------------
        denom = high - low;
        if (denom == 0.0) {
            throw new OperatorException("Denominator is 0, low and high wavelength are identical");
        }
        // inverse wavelength delta needed for baseline slope calculation
        inverseDelta = 1.0 / denom;

        // wavelength factor
        lambdaFactor = num / denom;
    }

    /**
     * Sets the value of the cloud correction factor to be used.
     *
     * @param factor The cloud correction factor.
     */
    public final void setCloudCorrectionFactor(float factor) {
        cloudCorrectionFactor = factor;
    }

    final double computeLineHeight(double lower, double upper, double peak) {
        return peak - cloudCorrectionFactor * (lower + (upper - lower) * lambdaFactor);
    }

    final double computeSlope(double lower, double upper) {
        return (upper - lower) * inverseDelta;
    }
}
