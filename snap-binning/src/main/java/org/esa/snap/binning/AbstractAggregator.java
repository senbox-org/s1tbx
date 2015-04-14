/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning;

import java.util.ArrayList;

/**
 * Abstract base class that provides the aggregator's static metadata.
 *
 * @author MarcoZ
 * @author Norman
 */
public abstract class AbstractAggregator implements Aggregator {

    private final String name;
    private final String[] spatialFeatureNames;
    private final String[] temporalFeatureNames;
    private final String[] outputFeatureNames;

    protected AbstractAggregator(String name,
                                 String[] spatialFeatureNames,
                                 String[] temporalFeatureNames,
                                 String[] outputFeatureNames) {
        this.name = name;
        this.spatialFeatureNames = spatialFeatureNames;
        this.temporalFeatureNames = temporalFeatureNames;
        this.outputFeatureNames = outputFeatureNames;
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public String[] getSpatialFeatureNames() {
        return spatialFeatureNames;
    }

    @Override
    public String[] getTemporalFeatureNames() {
        return temporalFeatureNames;
    }

    @Override
    public String[] getOutputFeatureNames() {
        return outputFeatureNames;
    }

    /**
     * Helper function that generates feature names by concatenating the given postfixes to the variable name.
     *
     * @param varName   The variable name.
     * @param postfixes Array of postfixes to append. A postfix may be {@code null}, in this case no corresponding feature name is generated.
     *
     * @return Array of feature names. Its length may be less than the length of the postfixes array.
     */
    public static String[] createFeatureNames(String varName, String... postfixes) {
        ArrayList<String> featureNames = new ArrayList<String>(postfixes.length);
        for (final String postfix : postfixes) {
            if (postfix != null) {
                featureNames.add(varName + "_" + postfix);
            }
        }
        return featureNames.toArray(new String[featureNames.size()]);
    }
}
