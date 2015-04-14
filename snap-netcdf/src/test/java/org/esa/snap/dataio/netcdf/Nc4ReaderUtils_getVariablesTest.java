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
package org.esa.snap.dataio.netcdf;

import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Variable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class Nc4ReaderUtils_getVariablesTest {

    private ArrayList<Variable> variables;

    @Before
    public void before() throws Exception {
        variables = new ArrayList<Variable>();
        final Variable v1 = mock(Variable.class);
        when(v1.getFullName()).thenReturn("wasweißich");
        variables.add(v1);
        final Variable v2 = mock(Variable.class);
        when(v2.getFullName()).thenReturn("ade");
        variables.add(v2);
        final Variable v3 = mock(Variable.class);
        when(v3.getFullName()).thenReturn("welcome");
        variables.add(v3);
    }

    @Test
    public void testOK_ContainsAllVariables() {
        final String[] names = {"ade", "welcome"};
        final Variable[] result = ReaderUtils.getVariables(variables, names);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertSame(variables.get(1), result[0]);
        assertSame(variables.get(2), result[1]);
    }

    @Test
    public void testOK_ContainsAllVariables_ReverseNames() {
        final String[] names = {"welcome", "ade"};
        final Variable[] result = ReaderUtils.getVariables(variables, names);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertSame(variables.get(2), result[0]);
        assertSame(variables.get(1), result[1]);
    }

    @Test
    public void testNotAllVariables() {
        final String[] names = {"ade", "lsmf"};
        final Variable[] result = ReaderUtils.getVariables(variables, names);
        assertNull(result);
    }

    @Test
    public void testListIsNull() {
        final String[] names = {"a", "b"};
        final List<Variable> variables = null;
        final Variable[] result = ReaderUtils.getVariables(variables, names);
        assertNull(result);
    }

    @Test
    public void testNamesAreNull() {
        final String[] names = null;
        final Variable[] result = ReaderUtils.getVariables(variables, names);
        assertNull(result);
    }

    @Test
    public void testListIsSmallerThanNames() {
        final String[] names = {"ade", "welcome", "wasWeißIch", "undNochEins"};
        final Variable[] result = ReaderUtils.getVariables(variables, names);
        assertNull(result);
    }
}
