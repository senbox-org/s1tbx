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
package org.esa.beam.framework.datamodel;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestCase;


public class ProductDataUTCTest extends TestCase {

    public void testParse() throws ParseException {
        final ProductData.UTC utc = ProductData.UTC.parse("05-Jan-2000 00:00:06.000007");
        assertEquals(4, utc.getElemIntAt(0));
        assertEquals(6, utc.getElemIntAt(1));
        assertEquals(7, utc.getElemIntAt(2));
    }

    public void testFormat() {
        final ProductData.UTC utc = new ProductData.UTC(4, 6, 7);
        assertEquals(4, utc.getElemIntAt(0));
        assertEquals(6, utc.getElemIntAt(1));
        assertEquals(7, utc.getElemIntAt(2));
        assertEquals(4, utc.getDaysFraction());
        assertEquals(6, utc.getSecondsFraction());
        assertEquals(7, utc.getMicroSecondsFraction());
        assertEquals("05-JAN-2000 00:00:06.000007", utc.format());
    }

    public void testMerisDateParsingErrors() throws ParseException {
        try {
            ProductData.UTC.parse(null);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            assertNull(ProductData.UTC.parse(""));
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testMerisDateParsing() throws ParseException {
        String _jan = "03-JAN-2003 01:02:03.3456";
        String _feb = "05-FEB-2002 02:03:04.67890";
        String _mar = "06-MAR-2002 02:03:04.67890";
        String _apr = "07-APR-2004 04:06:22.32311";
        String _mai = "08-MAY-2005 12:33:57.32311";
        String _dec = "23-DEC-2004 22:16:43.556677";

        final TimeZone utcZone = TimeZone.getTimeZone("UTC");
        Calendar calendar = GregorianCalendar.getInstance(utcZone, Locale.ENGLISH);

        Date date = ProductData.UTC.parse(_jan).getAsDate();
        calendar.clear();
        calendar.setTime(date);
        assertEquals(3, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(1 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2003, calendar.get(Calendar.YEAR));
        assertEquals(1, calendar.get(Calendar.HOUR));
        assertEquals(2, calendar.get(Calendar.MINUTE));
        assertEquals(3, calendar.get(Calendar.SECOND));
        assertEquals(346, calendar.get(Calendar.MILLISECOND));

        date = ProductData.UTC.parse(_feb).getAsDate();
        calendar.clear();
        calendar.setTime(date);
        assertEquals(5, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(2 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2002, calendar.get(Calendar.YEAR));
        assertEquals(2, calendar.get(Calendar.HOUR));
        assertEquals(3, calendar.get(Calendar.MINUTE));
        assertEquals(4, calendar.get(Calendar.SECOND));
        assertEquals(679, calendar.get(Calendar.MILLISECOND));

        date = ProductData.UTC.parse(_mar).getAsDate();
        calendar.clear();
        calendar.setTime(date);
        assertEquals(6, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(3 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2002, calendar.get(Calendar.YEAR));
        assertEquals(2, calendar.get(Calendar.HOUR));
        assertEquals(3, calendar.get(Calendar.MINUTE));
        assertEquals(4, calendar.get(Calendar.SECOND));
        assertEquals(679, calendar.get(Calendar.MILLISECOND));

        date = ProductData.UTC.parse(_apr).getAsDate();
        calendar.clear();
        calendar.setTime(date);
        assertEquals(7, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(4 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2004, calendar.get(Calendar.YEAR));
        assertEquals(4, calendar.get(Calendar.HOUR));
        assertEquals(6, calendar.get(Calendar.MINUTE));
        assertEquals(22, calendar.get(Calendar.SECOND));
        assertEquals(323, calendar.get(Calendar.MILLISECOND));

        date = ProductData.UTC.parse(_mai).getAsDate();
        calendar.clear();
        calendar.setTime(date);
        assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(5 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2005, calendar.get(Calendar.YEAR));
        assertEquals(12, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(33, calendar.get(Calendar.MINUTE));
        assertEquals(57, calendar.get(Calendar.SECOND));
        assertEquals(323, calendar.get(Calendar.MILLISECOND));

        date = ProductData.UTC.parse(_dec).getAsDate();
        calendar.clear();
        calendar.setTime(date);
        assertEquals(23, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(12 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2004, calendar.get(Calendar.YEAR));
        assertEquals(22, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(16, calendar.get(Calendar.MINUTE));
        assertEquals(43, calendar.get(Calendar.SECOND));
        assertEquals(557, calendar.get(Calendar.MILLISECOND));
    }

    public void testGetCalendar() {
        Calendar calendar = ProductData.UTC.createCalendar();
        assertEquals(ProductData.UTC.UTC_TIME_ZONE, calendar.getTimeZone());
        assertEquals(946684800000L, calendar.getTimeInMillis());
    }

    public void testGetAsDate() throws ParseException {
        Date date = ProductData.UTC.parse("23-DEC-2004 22:16:43.556677").getAsDate();
        Calendar calendar = ProductData.UTC.createCalendar();
        calendar.setTime(date);
        assertEquals(23, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(12 - 1, calendar.get(Calendar.MONTH));
        assertEquals(2004, calendar.get(Calendar.YEAR));
        assertEquals(22, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(16, calendar.get(Calendar.MINUTE));
        assertEquals(43, calendar.get(Calendar.SECOND));
        assertEquals(557, calendar.get(Calendar.MILLISECOND));
    }

    public void testParseAndFormat() throws ParseException {
        final String expected = "23-DEC-2004 22:16:43.556677";
        final ProductData.UTC utc = ProductData.UTC.parse(expected);
        assertEquals(expected, utc.format());
    }

    public void testDoubleConstructor() throws ParseException {
        final String expected = "23-DEC-2004 22:16:43.556677";
        final ProductData.UTC utc = ProductData.UTC.parse(expected);
        final double mjd = utc.getMJD();
        final ProductData.UTC utc2 = new ProductData.UTC(mjd);
        assertEquals(expected, utc2.format());
    }
}
