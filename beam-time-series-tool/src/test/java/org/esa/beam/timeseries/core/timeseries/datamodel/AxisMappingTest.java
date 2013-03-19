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

package org.esa.beam.timeseries.core.timeseries.datamodel;

import org.junit.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class AxisMappingTest {

    private AxisMapping axisMapping;

    @Before
    public void setUp() throws Exception {
        axisMapping = new AxisMapping();
    }

    @Test
    public void testGetRasterNames() throws Exception {
        axisMapping.addRasterName("algal", "algal_1");
        axisMapping.addRasterName("algal", "algal_2");

        final List<String> rasterNames = axisMapping.getRasterNames("algal");

        final List<String> expectedRasterNames = new ArrayList<String>(2);
        expectedRasterNames.add("algal_1");
        expectedRasterNames.add("algal_2");
        assertEquals(expectedRasterNames, rasterNames);
    }

    @Test
    public void testGetInsituNames() throws Exception {
        axisMapping.addInsituName("chl", "chl_1");
        axisMapping.addInsituName("chl", "chl2");

        final List<String> insituNames = axisMapping.getInsituNames("chl");

        final List<String> expectedInsituNames = new ArrayList<String>(2);
        expectedInsituNames.add("chl2");
        expectedInsituNames.add("chl_1");
        assertEquals(expectedInsituNames, insituNames);
    }

    @Test
    public void testRemoveAlias() throws Exception {
        final String alias = "chl";
        axisMapping.addInsituName(alias, "chl_1");
        axisMapping.addInsituName(alias, "chl2");
        axisMapping.addRasterName(alias, "chl_a");
        axisMapping.addRasterName(alias, "chl_b");

        axisMapping.removeAlias(alias);

        assertTrue(axisMapping.getInsituNames(alias).isEmpty());
        assertTrue(axisMapping.getRasterNames(alias).isEmpty());

    }

    @Test
    public void testRemoveInsitu() throws Exception {
        axisMapping.addInsituName("chl", "chl_1");
        axisMapping.addInsituName("chl", "chl2");

        axisMapping.removeInsituName("chl", "chl2");
        final List<String> insituNames = axisMapping.getInsituNames("chl");

        final List<String> expectedInsituNames = new ArrayList<String>(2);
        expectedInsituNames.add("chl_1");
        assertEquals(expectedInsituNames, insituNames);
    }

    @Test
    public void testRemoveRaster() throws Exception {
        axisMapping.addRasterName("algal", "algal_1");
        axisMapping.addRasterName("algal", "algal2");

        axisMapping.removeRasterName("algal", "algal2");
        final List<String> rasterNames = axisMapping.getRasterNames("algal");

        final List<String> expectedRasterNames = new ArrayList<String>(1);
        expectedRasterNames.add("algal_1");
        assertEquals(expectedRasterNames, rasterNames);
    }

    @Test
    public void testGetAliasNames() throws Exception {
        axisMapping.addRasterName("ra", "rn");
        axisMapping.addInsituName("ia", "in");

        final Set<String> names = axisMapping.getAliasNames();

        assertTrue(names instanceof SortedSet);
        final HashSet<String> expectedNames = new HashSet<String>();
        expectedNames.add("ra");
        expectedNames.add("ia");
        assertEquals(expectedNames, names);
    }

    @Test
    public void testAddAlias() throws Exception {
        axisMapping.addAlias("chl");

        assertEquals("chl", axisMapping.getAliasNames().iterator().next());
    }

    @Test
    public void testNoAliasNamesAreAddedAsSideEffect() throws Exception {
        axisMapping.getRasterNames("alias");
        axisMapping.getInsituNames("alias");
        assertTrue(axisMapping.getAliasNames().isEmpty());
    }

    @Test
    public void testReplaceAlias() throws Exception {
        axisMapping.addRasterName("alias", "RName");
        axisMapping.addInsituName("alias", "IName");

        axisMapping.replaceAlias("alias", "replaced");

        final Set<String> aliasNames = axisMapping.getAliasNames();
        assertEquals(1, aliasNames.size());
        assertEquals("replaced", aliasNames.iterator().next());
        assertEquals("RName", axisMapping.getRasterNames("replaced").iterator().next());
        assertEquals("IName", axisMapping.getInsituNames("replaced").iterator().next());
    }

    @Test
    public void testGetAliasNameForRasterName() {
        axisMapping.addRasterName("alias1", "rasterName1");
        axisMapping.addRasterName("alias2", "rasterName2");

        assertEquals("alias1", axisMapping.getRasterAlias("rasterName1"));
        assertNull(axisMapping.getRasterAlias("rasterName3"));
    }

    @Test
    public void testGetAliasNameForInsituName() {
        axisMapping.addInsituName("alias1", "insituName1");
        axisMapping.addInsituName("alias2", "insituName2");

        assertEquals("alias1", axisMapping.getInsituAlias("insituName1"));
        assertNull(axisMapping.getInsituAlias("insituName3"));
    }

    @Test
    public void testGetRasterCount() throws Exception {
        final int rasterCount = axisMapping.getRasterCount();
        axisMapping.addRasterName("alias", "raster1");
        axisMapping.addRasterName("alias", "raster2");
        axisMapping.addRasterName("alias1", "raster1_1");
        axisMapping.addRasterName("alias1", "raster2");
        final int rasterCount2 = axisMapping.getRasterCount();

        assertEquals(0, rasterCount);
        assertEquals(4, rasterCount2);
    }

    @Test
    public void testGetInsituCount() throws Exception {
        final int insituCount = axisMapping.getInsituCount();
        axisMapping.addInsituName("alias", "insitu1");
        axisMapping.addInsituName("alias", "insitu2");
        axisMapping.addInsituName("alias1", "insitu1_1");
        axisMapping.addInsituName("alias1", "insitu2");
        final int insituCount2 = axisMapping.getInsituCount();

        assertEquals(0, insituCount);
        assertEquals(4, insituCount2);
    }
}

