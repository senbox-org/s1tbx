/*
 * $Id$
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
package org.esa.beam.dataio.netcdf4;

import junit.framework.TestCase;
import ucar.nc2.Variable;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class Nc4ReaderUtils_getVariablesTest extends TestCase {

    private ArrayList<Variable> variables;

    protected void setUp() throws Exception {
        variables = new ArrayList<Variable>();
        final Variable v1 = mock(Variable.class);
        when(v1.getName()).thenReturn("wasweißich");
        variables.add(v1);
        final Variable v2 = mock(Variable.class);
        when(v2.getName()).thenReturn("ade");
        variables.add(v2);
        final Variable v3 = mock(Variable.class);
        when(v3.getName()).thenReturn("welcome");
        variables.add(v3);
    }

    protected void tearDown() throws Exception {
    }

    public void testOK_ContainsAllVariables() {
        final String[] names = {"ade", "welcome"};
        final Variable[] result = Nc4ReaderUtils.getVariables(variables, names);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertSame(variables.get(1), result[0]);
        assertSame(variables.get(2), result[1]);
    }

    public void testOK_ContainsAllVariables_ReverseNames() {
        final String[] names = {"welcome", "ade"};
        final Variable[] result = Nc4ReaderUtils.getVariables(variables, names);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertSame(variables.get(2), result[0]);
        assertSame(variables.get(1), result[1]);
    }

    public void testNotAllVariables() {
        final String[] names = {"ade", "lsmf"};
        final Variable[] result = Nc4ReaderUtils.getVariables(variables, names);
        assertNull(result);
    }

    public void testListIsNull() {
        final String[] names = {"a", "b"};
        final List<Variable> variables = null;
        final Variable[] result = Nc4ReaderUtils.getVariables(variables, names);
        assertNull(result);
    }

    public void testNamesAreNull() {
        final String[] names = null;
        final Variable[] result = Nc4ReaderUtils.getVariables(variables, names);
        assertNull(result);
    }

    public void testListIsSmallerThanNames() {
        final String[] names = {"ade", "welcome", "wasWeißIch", "undNochEins"};
        final Variable[] result = Nc4ReaderUtils.getVariables(variables, names);
        assertNull(result);
    }
}
