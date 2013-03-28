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
final class MLEAlgorithm implements Algorithm {

    // indices of accumulating variables
    private static final int A_SUM = 0;
    private static final int A_SUM_2 = 1;
    private static final int A_COUNT = 2;
    private static final int A_WEIGHT = 3;

    // indices of final variables
    private static final int F_MEAN = 0;
    private static final int F_SIGMA = 1;
    private static final int F_MEDIAN = 2;
    private static final int F_MODE = 3;
    private static final int F_COUNT = 4;

    // names of accumulating variables
    private static final String _SUM_NAME = "sum";
    private static final String _SUM_SQUARE_NAME = "sum_2";
    private static final String _COUNT_NAME = "count";
    private static final String _WEIGHT_NAME = "weight";

    // names of final variables
    private static final String _MEAN_NAME = "mean";
    private static final String _SIGMA_NAME = "sigma";
    private static final String _MEDIAN_NAME = "median";
    private static final String _MODE_NAME = "mode";

    private float _weightCoeff;
    private float _weightCoeffMinusOne;

    /**
     * Constructs the object with default parameter
     */
    public MLEAlgorithm() {
    }

    public void init(String algorithmParams) throws ProcessorException {
        final String[] strings = StringUtils.csvToArray(algorithmParams);
        if (strings.length != 1) {
            throw new ProcessorException("Illegal parameter length. Unable to extrakt the "
                                         + L3Constants.WEIGHT_COEFFICIENT_PARAMETER_NAME +
                                         " from the given algorithmParams string '" + algorithmParams + "'");
        }
        _weightCoeff = Float.parseFloat(algorithmParams);
        _weightCoeffMinusOne = _weightCoeff - 1.f;
    }

    /**
     * Accumulate the geophysical value passed in to the bin (spatial binning).
     * <p/>
     * param val the value to be accumulated
     *
     * @param bin the bin where the value shall be accumulated to
     */
    public final void accumulateSpatial(float val, Bin bin) {
        double logX;
        float temp;

        // check for positive and greater than zero
        if (val > 0.f) {
            logX = Math.log(val);

            // accumulate x
            temp = bin.read(A_SUM);
            temp = temp + (float) logX;
            bin.write(A_SUM, temp);

            // accumulate x^2
            temp = bin.read(A_SUM_2);
            temp = temp + (float) (logX * logX);
            bin.write(A_SUM_2, temp);

            // increment count
            temp = bin.read(A_COUNT);
            temp += 1;
            bin.write(A_COUNT, temp);
        }
    }

    /**
     * Process the final spatial binning algorithm.
     *
     * @param bin the bin to be finished.
     */
    public final void finishSpatial(Bin bin) {
        float temp;
        float weight;
        float binWeight;

        // calculate weight
        temp = bin.read(A_COUNT);
        // just need to do something when a measurement is in this bin
        if (temp > 0.f) {
            weight = (float) Math.pow(temp, _weightCoeff);
            bin.write(A_WEIGHT, weight);

            binWeight = (float) Math.pow(temp, _weightCoeffMinusOne);

            // normalize sumx
            temp = bin.read(A_SUM);
            temp = temp * binWeight;
            bin.write(A_SUM, temp);

            // normalize sum of squares
            temp = bin.read(A_SUM_2);
            temp = temp * binWeight;
            bin.write(A_SUM_2, temp);
        }
    }

    /**
     * Returns whether the algorithm needs to process a fishing stage for the spatial binning - or not
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

        temp = accumulated.read(A_COUNT);
        // interpretation only needed when data is present
        if (temp > 0.f) {
            double rootArg;
            interpreted.write(F_COUNT, temp);

            // preliminaries
            // -------------
            float avlogs = accumulated.read(A_SUM);
            float weight = accumulated.read(A_WEIGHT);
            float invWeight = 1.f / weight;
            avlogs *= invWeight;

            float vrlogs = accumulated.read(A_SUM_2);
            vrlogs *= invWeight;
            vrlogs -= avlogs * avlogs;

            // calculate the output
            // -------------------
            // mean
            temp = (float) Math.exp(avlogs + vrlogs * 0.5);
            interpreted.write(F_MEAN, temp);

            // sigma
            rootArg = Math.exp(vrlogs) - 1;
            if (rootArg >= 0.f) {
                temp = temp * (float) Math.sqrt(rootArg);
                interpreted.write(F_SIGMA, temp);
            } else {
                interpreted.write(F_SIGMA, -1.f);
            }

            // median
            temp = (float) Math.exp(avlogs);
            interpreted.write(F_MEDIAN, temp);

            // mode
            temp = (float) Math.exp(avlogs - vrlogs);
            interpreted.write(F_MODE, temp);
        } else {
            // no counts - zero output
            interpreted.write(F_COUNT, 0.f);
            interpreted.write(F_MEAN, 0.f);
            interpreted.write(F_SIGMA, 0.f);
            interpreted.write(F_MEDIAN, 0.f);
            interpreted.write(F_MODE, 0.f);
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
        return 5;
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
        } else if (index == F_MEDIAN) {
            strRet = _MEDIAN_NAME;
        } else if (index == F_MODE) {
            strRet = _MODE_NAME;
        } else if (index == F_COUNT) {
            strRet = _COUNT_NAME;
        }

        return strRet;
    }

    /**
     * Retrieves the short description string for the algorithm (used by algorithm factory)
     */
    public final String getTypeString() {
        return L3Constants.ALGORITHM_VALUE_MAXIMUM_LIKELIHOOD;
    }
}
