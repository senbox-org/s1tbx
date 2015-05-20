/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.gpf;

import org.esa.s1tbx.utilities.gpf.FilterOperator;
import org.esa.s1tbx.utilities.gpf.ui.FilterOpUI;
import org.esa.snap.framework.gpf.GPF;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import javax.swing.JComponent;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit test for SingleTileOperator.
 */
public class TestFilterOpUI {

    static {
        TestUtils.initTestEnvironment();
    }
    private final static OperatorSpi spi = new FilterOperator.Spi();
    private final static String operatorName = "Image-Filter";

    private FilterOpUI filterOpUI = new FilterOpUI();
    private final Map<String, Object> parameterMap = new HashMap<String, Object>(5);

    @Before
    public void Setup() {
        //if(GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName) != null) {
            GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
        //}
    }

    @Test
    public void testCreateOpTab() {
        JComponent component = filterOpUI.CreateOpTab(operatorName, parameterMap, null);
        assertNotNull(component);
    }

    @Test
    public void testLoadParameters() {

        parameterMap.put("selectedFilterName", "High-Pass 5x5");
        JComponent component = filterOpUI.CreateOpTab(operatorName, parameterMap, null);
        assertNotNull(component);

        FilterOperator.Filter filter = FilterOpUI.getSelectedFilter(filterOpUI.getTree());
        assertNotNull(filter);

        filterOpUI.setSelectedFilter("Arithmetic 5x5 Mean");

        filterOpUI.updateParameters();

        Object o = parameterMap.get("selectedFilterName");
        assertTrue(((String) o).equals("Arithmetic 5x5 Mean"));
    }

}
