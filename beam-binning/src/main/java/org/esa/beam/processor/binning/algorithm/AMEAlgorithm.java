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
package org.esa.beam.processor.binning.algorithm;

import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.processor.binning.L3Constants;
import org.esa.beam.processor.binning.database.Bin;
import org.esa.beam.util.StringUtils;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
final class AMEAlgorithm implements Algorithm {

    // variable indices for accumulation
    private static final int A_SUM = 0;
    private static final int A_SUM_2 = 1;
    private static final int A_COUNT = 2;
    private static final int A_WEIGHT = 3;

    // variable indices for finalization
    private static final int F_MEAN = 0;
    private static final int F_SIGMA = 1;
    private static final int F_COUNT = 2;

    // output band names
    private static final String _SUM_NAME = "sum";
    private static final String _SUM_SQUARE_NAME = "sum_2";
    private static final String _COUNT_NAME = "count";
    private static final String _WEIGHT_NAME = "weight";
    private static final String _MEAN_NAME = "mean";
    private static final String _SIGMA_NAME = "sigma";

    private float _weightCoeff;
    private float _weightCoeffMinusOne;

    /**
     * COnstructs the object with default parameters.
     */
    public AMEAlgorithm() {
    }

    public void init(String algorithmParams) throws ProcessorException {
        final String[] strings = StringUtils.csvToArray(algorithmParams);
        if (strings.length != 1) {
            throw new ProcessorException("The 'algorithmParams' parameter contains more or less than the one expected element");
        }
        _weightCoeff = Float.parseFloat(strings[0]);
        _weightCoeffMinusOne = _weightCoeff - 1.f;
    }

    /**
     * Accumulate the geophysical value passed in to the bin (spatial binning).
     *
     * @param val the value to be accumulated
     * @param bin the bin where the value shall be accumulated to
     */
    public final void accumulateSpatial(float val, Bin bin) {
        float temp;

        // accumulate value
        temp = bin.read(A_SUM);
        temp += val;
        bin.write(A_SUM, temp);

        // accumulate square
        temp = bin.read(A_SUM_2);
        temp += val * val;
        bin.write(A_SUM_2, temp);

        // accumulate count
        temp = bin.read(A_COUNT);
        temp += 1.f;
        bin.write(A_COUNT, temp);
    }

    /**
     * Process the final spatial binning algorithm.
     *
     * @param bin the bin to be finished.
     */
    public final void finishSpatial(Bin bin) {
        float temp;
        float count;
        float weight;

        // calculate weight
        count = bin.read(A_COUNT);
        if (count > 0.f) {
            weight = (float) Math.pow(count, _weightCoeff);
            bin.write(A_WEIGHT, weight);

            weight = (float) Math.pow(count, _weightCoeffMinusOne);
            // normalize sumx
            temp = bin.read(A_SUM);
            temp = temp * weight;
            bin.write(A_SUM, temp);

            // normalize sum of squares
            temp = bin.read(A_SUM_2);
            temp = temp * weight;
            bin.write(A_SUM_2, temp);
        }
    }

    /**
     * Returns whether the algorithm needs to process a fishing stage for the spatial binning - or not.
     */
    public final boolean needsFinishSpatial() {
        return true;
    }

    /**
     * Accumulate the source bin to the target bin (temporal binning).
     *
     * @param source the source bin (spatial binDB)
     * @param target the target bin (temporal binDB)
     */
    public final void accumulateTemporal(Bin source, Bin target) {
        float tempSource;
        float tempTarget;

        tempSource = source.read(A_COUNT);
        // only needs to accumulate if there was a measurement (count > 0)
        if (tempSource > 0.f) {
            tempTarget = target.read(A_COUNT);
            target.write(A_COUNT, tempSource + tempTarget);

            tempSource = source.read(A_SUM);
            tempTarget = target.read(A_SUM);
            target.write(A_SUM, tempSource + tempTarget);

            tempSource = source.read(A_SUM_2);
            tempTarget = target.read(A_SUM_2);
            target.write(A_SUM_2, tempSource + tempTarget);

            tempSource = source.read(A_WEIGHT);
            tempTarget = target.read(A_WEIGHT);
            target.write(A_WEIGHT, tempSource + tempTarget);
        }
    }

    /**
     * Interprete the bin. Final processing stage.
     *
     * @param accumulated the accumulated (spatially and temporally averaged) bin
     * @param interpreted the interpreted measument bin
     */
    public final void interprete(Bin accumulated, Bin interpreted) {
        float temp;
        float weight;

        temp = accumulated.read(A_COUNT);
        weight = accumulated.read(A_WEIGHT);
        // only need to perform interpretation when a measurement is in the bin
        if ((temp > 0.f) && (weight > 0.f)) {
            interpreted.write(F_COUNT, temp);

            // preliminaries
            // ------------
            float invWeight = 1.f / weight;

            // mean
            // ----
            float mean = accumulated.read(A_SUM);
            mean *= invWeight;
            interpreted.write(F_MEAN, mean);

            // sigma
            // -----
            float rootArg;
            temp = accumulated.read(A_SUM_2);
            rootArg = temp * invWeight - mean * mean;
            if (rootArg > 0.f) {
                temp = (float) Math.sqrt(rootArg);
                interpreted.write(F_SIGMA, temp);
            } else {
                interpreted.write(F_SIGMA, -1.f);
            }

        } else {
            // no counts - set everything to zero
            interpreted.write(F_COUNT, 0.f);
            interpreted.write(F_MEAN, 0.f);
            interpreted.write(F_SIGMA, 0.f);
        }
    }

    /**
     * Returns whether the algorithm needs to process an interpretation stage for the temporal binning - or not.
     */
    public final boolean needsInterpretation() {
        return true;
    }

    /**
     * Retrieves the number of variables needed for accumulation.
     */
    public final int getNumberOfAccumulatedVariables() {
        return 4;
    }

    /**
     * Retrieves a name for the accumulated variable at the given index.
     *
     * @param index the variable index
     */
    public final String getAccumulatedVariableNameAt(int index) {
        String strRet = null;

        if (index == A_SUM) {
            strRet = _SUM_NAME;
        } else if (index == A_SUM_2) {
            strRet = _SUM_SQUARE_NAME;
        } else if (index == A_COUNT) {
            strRet = _COUNT_NAME;
        } else if (index == A_WEIGHT) {
            strRet = _WEIGHT_NAME;
        }
        return strRet;
    }

    /**
     * Retrieves the number of variables of the interpreted bin, i.e. in the final product.
     */
    public final int getNumberOfInterpretedVariables() {
        return 3;
    }

    /**
     * Retrieves a name for the interprested variable at the given index.
     *
     * @param index the variable index
     */
    public final String getInterpretedVariableNameAt(int index) {
        String strRet = null;

        if (index == F_MEAN) {
            strRet = _MEAN_NAME;
        } else if (index == F_SIGMA) {
            strRet = _SIGMA_NAME;
        } else if (index == F_COUNT) {
            strRet = _COUNT_NAME;
        }

        return strRet;
    }

    /**
     * Retrieves the short description string for the algorithm (used by algorithm factory)
     */
    public final String getTypeString() {
        return L3Constants.ALGORITHM_VALUE_ARITHMETIC_MEAN;
    }
}
