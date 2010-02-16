/*
 * $Id: BaselineAlgorithm.java,v 1.1.1.1 2006/09/11 08:16:52 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.processor.flh_mci;

import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.util.Guardian;

/**
 * Implements a general baselin height algorithm with optional baseline slope calculation.
 */
public final class BaselineAlgorithm {

    public static final float DEFAULT_CLOUD_CORRECT = 1.005f;
    private double _lambdaFactor;
    private double _invDelta;
    private double _cloudCorrect;
    private float _invalid;

    /**
     * Constructs the object with default parameters
     */
    public BaselineAlgorithm() {
        _lambdaFactor = 1.0;
        _invDelta = 1.0;
        _invalid = 0.f;
        _cloudCorrect = DEFAULT_CLOUD_CORRECT;
    }

    /**
     * Sets the center wavelengths of the low- and high baseline band and of the signal band to be used during
     * calculation.
     *
     * @param low    lower band wavelength in nm
     * @param high   higher band wavelength in nm
     * @param signal signal band wavelength in nm
     */
    public final void setWavelengths(float low, float high, float signal) throws ProcessorException {
        float num;
        float denom;

        // check for correct wavelengths
        // -----------------------------
        if ((low < 0.f) || (high < 0.f) || (signal < 0.f)) {
            throw new ProcessorException(FlhMciConstants.ERROR_MSG_NEGATIVE_WAVELENGTH);
        }

        // set numerator and check for validity
        // ------------------------------------
        num = signal - low;
        if (num == 0.f) {
            throw new ProcessorException(FlhMciConstants.ERROR_MSG_NUMERATOR_ZERO);
        }

        // set denominator and check for validity
        // --------------------------------------
        denom = high - low;
        if (denom == 0.f) {
            throw new ProcessorException(FlhMciConstants.ERROR_MSG_DENOM_ZERO);
        }
        // inverse wavelength delta needed for baseline slope calculation
        _invDelta = 1.0 / denom;

        // wavelength factor
        _lambdaFactor = num / denom;
    }

    /**
     * Sets the value used for invalid pixel.
     */
    public final void setInvalidPixelValue(float invalid) {
        _invalid = invalid;
    }

    /**
     * Sets the value of the cloud correction factor to be used.
     *
     * @param fFactor
     */
    public final void setCloudCorrectionFactor(float fFactor) {
        _cloudCorrect = fFactor;
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
     */
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
                line_ret[n] = _invalid;
                continue;
            }

            // calculate line height
            delta = high[n] - low[n];
            line_ret[n] = (float) (signal[n] - _cloudCorrect * (low[n] + (delta * _lambdaFactor)));
        }

        return line_ret;
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
     */
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
                slope_ret[n] = _invalid;
                continue;
            }

            radianceDelta = high[n] - low[n];
            slope_ret[n] = (float) (radianceDelta * _invDelta);
        }

        return slope_ret;
    }
}
