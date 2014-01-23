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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

        assertFalse(filter.accept(createProduct(DataPeriod.Membership.PREVIOUS_PERIODS, DataPeriod.Membership.PREVIOUS_PERIODS)));
        assertTrue(filter.accept(createProduct(DataPeriod.Membership.PREVIOUS_PERIODS, DataPeriod.Membership.CURRENT_PERIOD)));
        assertTrue(filter.accept(createProduct(DataPeriod.Membership.PREVIOUS_PERIODS, DataPeriod.Membership.SUBSEQUENT_PERIODS)));

        assertTrue(filter.accept(createProduct(DataPeriod.Membership.CURRENT_PERIOD, DataPeriod.Membership.CURRENT_PERIOD)));
        assertTrue(filter.accept(createProduct(DataPeriod.Membership.CURRENT_PERIOD, DataPeriod.Membership.SUBSEQUENT_PERIODS)));

        assertFalse(filter.accept(createProduct(DataPeriod.Membership.SUBSEQUENT_PERIODS, DataPeriod.Membership.SUBSEQUENT_PERIODS)));
    }

    private Product createProduct(DataPeriod.Membership firstPeriod, DataPeriod.Membership lastPeriod) {
        return TestUtils.createProduct(dataPeriod, firstPeriod, lastPeriod);
    }

}
