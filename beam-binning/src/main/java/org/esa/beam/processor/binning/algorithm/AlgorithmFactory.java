/*
 * $Id: AlgorithmFactory.java,v 1.1 2006/09/11 10:47:31 norman Exp $
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
package org.esa.beam.processor.binning.algorithm;

import org.esa.beam.processor.binning.L3Constants;
import org.esa.beam.util.Guardian;

/**
 * Creates the three known algorithms.
 */
public class AlgorithmFactory implements AlgorithmCreator {

    /**
     * Retrieves an algorithm object specified by the identifier string passed in.
     *
     * @param algoName the algorithm name
     * @throws IllegalArgumentException if the requested algorithm is unkown
     */
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
