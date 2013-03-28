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

import org.esa.beam.processor.binning.L3Constants;
import org.esa.beam.processor.binning.database.Bin;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
final class MINMAXAlgorithm implements Algorithm {

    // variable indices
    private static final int MIN = 0;
    private static final int MAX = 1;
    private static final int COUNT = 2;

    // variable names
    private static final String _MIN_NAME = "min";
    private static final String _MAX_NAME = "max";
    private static final String _COUNT_NAME = "count";

    /**
     * COnstructs the object with default parameter.
     */
    public MINMAXAlgorithm() {
    }

    /**
     * Initialize the algorithm given an algorithm parameter string.
     *
     * @param algorithmParams the algorithm parameter string
     */
    public void init(String algorithmParams) {
        // this algorithm does not need any parameters.
    }

    /**
     * Accumulate the geophysical value passed in to the bin (spatial binning).
     *
     * @param val the value to be accumulated
     * @param bin the bin where the value shall be accumulated to
     */
    public final void accumulateSpatial(float val, Bin bin) {
        float temp;
        float count;

        count = bin.read(COUNT);
        if (count != 0.f) {
            // check the minimum
            temp = bin.read(MIN);
            if (temp > val) {
                bin.write(MIN, val);
            }

            // check the maximum
            temp = bin.read(MAX);
            if (temp < val) {
                bin.write(MAX, val);
            }
        } else {
            // first measurement in this bin - min and max are the current value.
            bin.write(MIN, val);
            bin.write(MAX, val);
        }

        // increment measurement count
        count += 1.f;
        bin.write(COUNT, count);
    }

    /**
     * Process the final spatial binning algorithm.
     *
     * @param bin the bin to be finished
     */
    public final void finishSpatial(Bin bin) {
        // this algorithm doesn't need this step to be performed.
    }

    /**
     * Returns whether the algorithm needs to process a fishing stage for the spatial binning - or not.
     */
    public final boolean needsFinishSpatial() {
        return false;
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

        tempSource = source.read(COUNT);
        // only need to accumulate when there is a measurement
        if (tempSource > 0.f) {
            // increment count
            tempTarget = target.read(COUNT);
            target.write(COUNT, tempSource + tempTarget);

            if (tempTarget != 0.f) {
                // standard accumulation - check for min and max
                tempSource = source.read(MAX);
                tempTarget = target.read(MAX);
                if (tempSource > tempTarget) {
                    target.write(MAX, tempSource);
                }

                tempSource = source.read(MIN);
                tempTarget = target.read(MIN);
                if (tempSource < tempTarget) {
                    target.write(MIN, tempSource);
                }
            } else {
                // first measurement for this bin - min and max are the current value
                tempSource = source.read(MAX);
                target.write(MAX, tempSource);
                tempSource = source.read(MIN);
                target.write(MIN, tempSource);
            }
        }
    }

    /**
     * Interprete the bin. Final processing stage.
     *
     * @param accumulated the accumulated (spatially and temporally averaged) bin
     * @param interpreted the interpreted measument bin
     */
    public final void interprete(Bin accumulated, Bin interpreted) {
        // this algorithm does not need a final stage.
    }

    /**
     * Returns whether the algorithm needs to process an interpretation stage for the temporal binning - or not
     */
    public final boolean needsInterpretation() {
        return false;
    }

    /**
     * Retrieves the number of variables needed for accumulation.
     */
    public final int getNumberOfAccumulatedVariables() {
        return 3;
    }

    /**
     * Retrieves a name for the accumulated variable at the given index.
     *
     * @param index the variable index
     */
    public final String getAccumulatedVariableNameAt(int index) {
        String strRet = null;

        if (index == MIN) {
            strRet = _MIN_NAME;
        } else if (index == MAX) {
            strRet = _MAX_NAME;
        } else if (index == COUNT) {
            strRet = _COUNT_NAME;
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

        if (index == MIN) {
            strRet = _MIN_NAME;
        } else if (index == MAX) {
            strRet = _MAX_NAME;
        } else if (index == COUNT) {
            strRet = _COUNT_NAME;
        }
        return strRet;
    }

    /**
     * Retrieves the short description string for the algorithm (used by algorithm factory).
     */
    public final String getTypeString() {
        return L3Constants.ALGORITHM_VALUE_MINIMUM_MAXIMUM;
    }
}
