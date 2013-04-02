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
import org.esa.beam.util.Guardian;

@Deprecated
/**
 * Creates the three known algorithms.
 *
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class AlgorithmFactory implements AlgorithmCreator {

    /**
     * Retrieves an algorithm object specified by the identifier string passed in.
     *
     * @param algoName the algorithm name
     * @throws IllegalArgumentException if the requested algorithm is unknown
     */
    @Override
    public Algorithm getAlgorithm(String algoName) throws IllegalArgumentException {
        Guardian.assertNotNull("algoName", algoName);

        if (algoName.equalsIgnoreCase(L3Constants.ALGORITHM_VALUE_MAXIMUM_LIKELIHOOD)) {
            return new MLEAlgorithm();
        } else if (algoName.equalsIgnoreCase(L3Constants.ALGORITHM_VALUE_ARITHMETIC_MEAN)) {
            return new AMEAlgorithm();
        } else if (algoName.equalsIgnoreCase(L3Constants.ALGORITHM_VALUE_MINIMUM_MAXIMUM)) {
            return new MINMAXAlgorithm();
        } else {
            throw new IllegalArgumentException("invalid algorithm name: " + algoName);
        }
    }
}
