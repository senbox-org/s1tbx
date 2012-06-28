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

package org.esa.beam.statistics;

import com.bc.ceres.binding.ConversionException;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class StatisticsOpTest {

    static {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
    }

    @Test
    public void testThatStatisticsOpIsRegistered() throws Exception {
        assertNotNull(GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("StatisticsOp"));
    }

    @Test
    public void testUtcConverter() throws Exception {
        final StatisticsOp.UtcConverter utcConverter = new StatisticsOp.UtcConverter();
        assertEquals(ProductData.UTC.class, utcConverter.getValueType());

        final ProductData.UTC actual = utcConverter.parse("2010-01-31 14:46:22");
        final long expected = ProductData.UTC.parse("2010-01-31 14:46:22", "yyyy-MM-dd hh:mm:ss").getAsDate().getTime();

        assertEquals(expected, actual.getAsDate().getTime());

        expectException(utcConverter, "2010-01-31'T'14:46:22.1234");
        expectException(utcConverter, "2010-31-01'T'14:46:22.123");
        expectException(utcConverter, "2010-01-31T14:46:22.123");
        expectException(utcConverter, "2010-01-31'T'14.46.22.123");

        try {
            utcConverter.format(ProductData.UTC.parse("2010-JAN-01 10:37:22"));
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Not implemented"));
        }
    }

    private static void expectException(StatisticsOp.UtcConverter utcConverter, String text) {
        try {
            utcConverter.parse(text);
            fail();
        } catch (ConversionException e) {}
    }
}
