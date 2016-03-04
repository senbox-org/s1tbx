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

package org.esa.snap.binning.aggregators;

import org.junit.Test;

import static org.esa.snap.binning.AbstractAggregator.*;
import static org.junit.Assert.*;

public class AggregatorTest {

    @Test
    public void testCreateFeatureNames() {
        testFeaturesNames(createFeatureNames("c", "mean", "sigma", "count"), "c_mean", "c_sigma", "c_count");
        testFeaturesNames(createFeatureNames("c", "mean", "sigma", null), "c_mean", "c_sigma");
        testFeaturesNames(createFeatureNames("c"));
        testFeaturesNames(createFeatureNames("c", null, null, null));
    }

    static void testFeaturesNames(String[] actualFeatureNames, String... expectedFeatureNames) {
        assertArrayEquals(expectedFeatureNames, actualFeatureNames);
    }
}
