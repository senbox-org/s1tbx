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

import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Storm
 */
public class DateRangeParserTest {

    @Test
    public void testTryToGetDateRangeFromValidDailyProduct() throws Exception {
        ProductData.UTC[] dateRange = DateRangeParser.tryToGetDateRange("20111103_est_wac_wew_1200.dim");
        assertEquals(dateRange.length, 2);
        assertEquals(ProductData.UTC.parse("2011-11-03", "yyyy-MM-dd").getAsDate().getTime(), dateRange[0].getAsDate().getTime());
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());
    }

    @Test
    public void testTryToGetDateRangeFromValidDailyProductWithLeadingCharacters() throws Exception {
        ProductData.UTC[] dateRange = DateRangeParser.tryToGetDateRange("leading_characters_20111103_est_wac_wew_1200.dim");
        assertEquals(dateRange.length, 2);
        assertEquals(ProductData.UTC.parse("2011-11-03", "yyyy-MM-dd").getAsDate().getTime(), dateRange[0].getAsDate().getTime());
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());
    }

    @Test
    public void testTryToGetDateRangeFromValidWeeklyProduct() throws Exception {
        ProductData.UTC[] dateRange = DateRangeParser.tryToGetDateRange("20110917_20110923_bas_wac_acr_1200.dim");
        assertEquals(dateRange.length, 2);
        assertEquals(ProductData.UTC.parse("2011-09-17", "yyyy-MM-dd").getAsDate().getTime(), dateRange[0].getAsDate().getTime());
        assertEquals(ProductData.UTC.parse("2011-09-23", "yyyy-MM-dd").getAsDate().getTime(), dateRange[1].getAsDate().getTime());
    }

    @Test
    public void testTryToGetDateRangeFromValidMonthlyProduct() throws Exception {
        ProductData.UTC[] dateRange = DateRangeParser.tryToGetDateRange("201106_bas_wac_acr_1200.dim");
        assertEquals(dateRange.length, 2);
        assertEquals(ProductData.UTC.parse("2011-06", "yyyy-MM").getAsDate().getTime(), dateRange[0].getAsDate().getTime());
        assertEquals(dateRange[0].getAsDate().getTime(), dateRange[1].getAsDate().getTime());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotMatchAnyPattern() throws Exception {
        DateRangeParser.tryToGetDateRange("doesNotMatchAnyPattern");
    }
}
