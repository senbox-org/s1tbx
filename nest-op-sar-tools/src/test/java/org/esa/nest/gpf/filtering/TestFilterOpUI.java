/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf.filtering;

import junit.framework.TestCase;
import org.esa.beam.framework.gpf.GPF;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit test for SingleTileOperator.
 */
public class TestFilterOpUI extends TestCase {

    private FilterOpUI filterOpUI;
    private final Map<String, Object> parameterMap = new HashMap<String, Object>(5);

    @Override
    protected void setUp() throws Exception {
        filterOpUI = new FilterOpUI();

        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new FilterOperator.Spi());
    }

    @Override
    protected void tearDown() throws Exception {
        filterOpUI = null;
    }

    public void testCreateOpTab() {

        JComponent component = filterOpUI.CreateOpTab("Image-Filter", parameterMap, null);
        assertNotNull(component);
    }

    public void testLoadParameters() {

        parameterMap.put("selectedFilterName", "High-Pass 5x5");
        JComponent component = filterOpUI.CreateOpTab("Image-Filter", parameterMap, null);
        assertNotNull(component);

        FilterOperator.Filter filter = FilterOpUI.getSelectedFilter(filterOpUI.getTree());
        assertNotNull(filter);

        filterOpUI.setSelectedFilter("Median 5x5");

        filterOpUI.updateParameters();

        Object o = parameterMap.get("selectedFilterName");
        assertTrue(((String)o).equals("Median 5x5"));
    }

}
