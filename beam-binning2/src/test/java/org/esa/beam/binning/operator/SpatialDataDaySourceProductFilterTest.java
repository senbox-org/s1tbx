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

package org.esa.beam.binning.operator;

import org.esa.beam.binning.DataPeriod;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class SpatialDataDaySourceProductFilterTest {

    private DataPeriod dataPeriod;

    @Before
    public void setUp() throws Exception {
        dataPeriod = TestUtils.createSpatialDataPeriod();
    }

    @Test
    public void testAccept() throws Exception {
        SpatialDataDaySourceProductFilter filter = new SpatialDataDaySourceProductFilter(dataPeriod);

        assertFalse(filter.accept(createProduct(DataPeriod.Membership.PREVIOUS_PERIOD, DataPeriod.Membership.PREVIOUS_PERIOD)));
        assertTrue(filter.accept(createProduct(DataPeriod.Membership.PREVIOUS_PERIOD, DataPeriod.Membership.CURRENT_PERIOD)));
        assertTrue(filter.accept(createProduct(DataPeriod.Membership.PREVIOUS_PERIOD, DataPeriod.Membership.NEXT_PERIOD)));

        assertTrue(filter.accept(createProduct(DataPeriod.Membership.CURRENT_PERIOD, DataPeriod.Membership.CURRENT_PERIOD)));
        assertTrue(filter.accept(createProduct(DataPeriod.Membership.CURRENT_PERIOD, DataPeriod.Membership.NEXT_PERIOD)));

        assertFalse(filter.accept(createProduct(DataPeriod.Membership.NEXT_PERIOD, DataPeriod.Membership.NEXT_PERIOD)));
    }

    private Product createProduct(DataPeriod.Membership firstPeriod, DataPeriod.Membership lastPeriod) {
        return TestUtils.createProduct(dataPeriod, firstPeriod, lastPeriod);
    }

}
