/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.cellprocessor;

import org.esa.snap.binning.MyVariableContext;
import org.esa.snap.binning.support.VectorImpl;
import org.junit.Test;

import static org.esa.snap.binning.aggregators.AggregatorTestUtils.*;
import static org.junit.Assert.*;

public class FeatureSelectionTest {

    @Test
    public void testSelection() throws Exception {
        MyVariableContext variableContext = new MyVariableContext("A", "B", "C");
        FeatureSelection selection = new FeatureSelection(variableContext, "B");

        String[] outputFeatureNames = selection.getOutputFeatureNames();
        assertArrayEquals(new String[]{"B"}, outputFeatureNames);

        VectorImpl input = vec(0.1f, 0.2f, 0.3f);
        VectorImpl output= vec(Float.NaN);
        selection.compute(input, output);

        assertEquals(0.2f, output.get(0), 1e-5f);
    }

    @Test
    public void testSelectionWithRenaming() throws Exception {
        MyVariableContext variableContext = new MyVariableContext("A", "B", "C");
        FeatureSelection selection = new FeatureSelection(variableContext, "B", "D=A");

        String[] outputFeatureNames = selection.getOutputFeatureNames();
        assertArrayEquals(new String[]{"B", "D"}, outputFeatureNames);

        VectorImpl input = vec(0.1f, 0.2f, 0.3f);
        VectorImpl output= vec(Float.NaN, Float.NaN);
        selection.compute(input, output);

        assertEquals(0.2f, output.get(0), 1e-5f);
        assertEquals(0.1f, output.get(1), 1e-5f);
    }

}
