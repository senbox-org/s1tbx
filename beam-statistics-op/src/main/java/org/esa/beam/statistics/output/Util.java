/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.statistics.output;

import org.opengis.feature.simple.SimpleFeature;

import javax.media.jai.Histogram;

/**
 * Provides some utility functions.
 *
 * @author Thomas Storm
 */
public class Util {

    /**
     * Returns the 'best' name of the given feature.
     *
     * @param simpleFeature The feature to inquire the name of.
     *
     * @return The name if such an attribute exists in the feature, or the feature's id as last resort.
     */
    public static String getFeatureName(SimpleFeature simpleFeature) {
        if (simpleFeature.getAttribute("name") != null) {
            return simpleFeature.getAttribute("name").toString();
        } else if (simpleFeature.getAttribute("NAME") != null) {
            return simpleFeature.getAttribute("NAME").toString();
        }
        return simpleFeature.getID();
    }

    /**
     * Returns the width of the bin cells in the given histogram.
     * @param histogram The histogram to inquire the bin cell width of.
     * @return The width of the bin cells in the histogram.
     */
    public static double getBinWidth(Histogram histogram) {
        return (histogram.getHighValue(0) - histogram.getLowValue(0)) / histogram.getNumBins(0);
    }
}
