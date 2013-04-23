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

package org.esa.beam.binning;

import java.util.ArrayList;
import java.util.List;

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

    protected AbstractAggregator(String name, String[] featureNames) {
        this(name, featureNames, featureNames, featureNames);
    }

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
        final List<String> result = new ArrayList<String>(outputFeatureNames.length);
        for (String name : outputFeatureNames) {
            result.add(name.replace("<", "").replace(">", ""));
        }
        return result.toArray(new String[result.size()]);
    }

    public static String[] createFeatureNames(String varName, String... names) {
        String[] featureNames = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            featureNames[i] = varName + "_" + names[i];
        }
        return featureNames;
    }
}
