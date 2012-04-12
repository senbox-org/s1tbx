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

import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.util.Guardian;

/**
 * Implements a general baseline height algorithm with optional baseline slope calculation.
 */
public final class BaselineAlgorithm {

    public static final float DEFAULT_CLOUD_CORRECT = 1.005f;
    private double lambdaFactor;
    private double inverseDelta;
    private double cloudCorrectionFactor;
    private float invalidValue;

    /**
     * Constructs the object with default parameters
     */
    public BaselineAlgorithm() {
        lambdaFactor = 1.0;
        inverseDelta = 1.0;
        invalidValue = 0.f;
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
    public final void setWavelengths(float low, float high, float signal) throws ProcessorException {
        double num;
        double denom;

        // check for correct wavelengths
        // -----------------------------
        if (low < 0.f || high < 0.f || signal < 0.f) {
            throw new ProcessorException(FlhMciConstants.ERROR_MSG_NEGATIVE_WAVELENGTH);
        }

        // set numerator and check for validity
        // ------------------------------------
        num = signal - low;
        if (num == 0.0) {
            throw new ProcessorException(FlhMciConstants.ERROR_MSG_NUMERATOR_ZERO);
        }

        // set denominator and check for validity
        // --------------------------------------
        denom = high - low;
        if (denom == 0.0) {
            throw new ProcessorException(FlhMciConstants.ERROR_MSG_DENOM_ZERO);
        }
        // inverse wavelength delta needed for baseline slope calculation
        inverseDelta = 1.0 / denom;

        // wavelength factor
        lambdaFactor = num / denom;
    }

    /**
     * Sets the value used for invalid pixel.
     */
    public final void setInvalidValue(float invalid) {
        this.invalidValue = invalid;
    }

    /**
     * Sets the value of the cloud correction factor to be used.
     *
     * @param factor The cloud correction factor.
     */
    public final void setCloudCorrectionFactor(float factor) {
        cloudCorrectionFactor = factor;
    }

    /**
     * Processes the baseline height algorithm.
     *
     * @param low     array of low baseline wavelength radiances
     * @param high    array of high baseline wavelength radiances
     * @param signal  array of signal wavelength radiances
     * @param process array of boolean determining the pixels to be processed
     * @param recycle if not <code>null</code> and of correct size this array will be reused for the return values
     *
     * @return array of baseline height values
     *
     * @deprecated since BEAM 4.10 - no replacement.
     */
    @Deprecated
    public final float[] process(float[] low, float[] high, float[] signal, boolean[] process, float[] recycle) {
        Guardian.assertNotNull("low data", low);
        Guardian.assertNotNull("high data", high);
        Guardian.assertNotNull("signal data", signal);
        Guardian.assertNotNull("process data", process);

        float[] line_ret;
        double delta;

        // try to reuse the recyle array to prevent memory waste. We can reuse if
        // a) it's present and
        // b) has the same size one of the input vectors
        if ((recycle == null) || (recycle.length != low.length)) {
            line_ret = new float[low.length];
        } else {
            line_ret = recycle;
        }

        // now loop over vector
        for (int n = 0; n < low.length; n++) {
            // check if the pixel shall be processed
            if (!process[n]) {
                line_ret[n] = invalidValue;
                continue;
            }

            // calculate line height
            delta = high[n] - low[n];
            line_ret[n] = (float) (signal[n] - cloudCorrectionFactor * (low[n] + (delta * lambdaFactor)));
        }

        return line_ret;
    }

    final double computeLineHeight(double lower, double upper, double peak) {
        return peak - cloudCorrectionFactor * (lower + (upper - lower) * lambdaFactor);
    }

    /**
     * Processes the baseline slope of the linear equation y = a * x + b. [a] = 1 / nm
     *
     * @param low     array of low band radiances
     * @param high    array of high band radiances
     * @param process array of boolean determining the pixels to be processed
     * @param recycle if not <code>null</code> and of correct size this array will be reused for the return values
     *
     * @return array of baseline height values
     *
     * @deprecated since BEAM 4.10 - no replacement.
     */
    @Deprecated
    public final float[] processSlope(float[] low, float[] high, boolean[] process, float[] recycle) {
        Guardian.assertNotNull("low data", low);
        Guardian.assertNotNull("high data", high);
        Guardian.assertNotNull("process data", process);

        float[] slope_ret;
        double radianceDelta;

        // try to reuse the recyle array to prevent memory waste. We can reuse if
        // a) it's present and
        // b) has the same size one of the input vectors
        if ((recycle == null) || (recycle.length != low.length)) {
            slope_ret = new float[low.length];
        } else {
            slope_ret = recycle;
        }

        // loop over vectors
        for (int n = 0; n < low.length; n++) {
            // check if the pixel shall be processed
            if (!process[n]) {
                slope_ret[n] = invalidValue;
                continue;
            }

            radianceDelta = high[n] - low[n];
            slope_ret[n] = (float) (radianceDelta * inverseDelta);
        }

        return slope_ret;
    }

    final double computeSlope(double lower, double upper) {
        return (upper - lower) * inverseDelta;
    }
}
