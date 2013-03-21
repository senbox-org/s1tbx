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

package org.esa.beam.timeseries.core.insitu;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.timeseries.core.insitu.csv.CsvRecordSource;
import org.esa.beam.timeseries.core.insitu.csv.InsituRecord;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Storm
 * @author Sabine Embacher
 */
public class InsituSourceTest {

    private DateFormat dateFormat;
    private InsituSource insituSource;

    @Before
    public void setUp() throws Exception {
        dateFormat = ProductData.UTC.createDateFormat("dd.MM.yyyy");
        final StringReader csvReader = new StringReader("# Test CSV\n"
                                                        + "LAT\tLON\tTIME\tStation\tCHL\tys\n"
                                                        + "10\t30\t08.04.2003\tName 1\t0.9\t20\n"
                                                        + "20\t40\t02.04.2003\tName 2\t0.5\t30\n"
                                                        + "20\t40\t05.04.2003\tName 2\t0.6\t40\n"
                                                        + "20\t50\t11.04.2003\tName 3\t0.4\t50\n");
        final CsvRecordSource csvRecordSource = new CsvRecordSource(csvReader, dateFormat);
        insituSource = new InsituSource(csvRecordSource);
    }

    @Test
    public void testGetValuesForCHL_TimeOrdered() throws Exception {
        // execution
        InsituRecord[] chlRecords = insituSource.getValuesFor("CHL", null);

        // verification
        assertEquals(4, chlRecords.length);

        InsituRecord expectedRecord;
        expectedRecord = new InsituRecord(new GeoPos(20, 40), getDate("02.04.2003"), "Name 2", 0.5);
        assertEquals(expectedRecord, chlRecords[0]);

        expectedRecord = new InsituRecord(new GeoPos(20, 40), getDate("05.04.2003"), "Name 2", 0.6);
        assertEquals(expectedRecord, chlRecords[1]);

        expectedRecord = new InsituRecord(new GeoPos(10, 30), getDate("08.04.2003"), "Name 1", 0.9);
        assertEquals(expectedRecord, chlRecords[2]);

        expectedRecord = new InsituRecord(new GeoPos(20, 50), getDate("11.04.2003"), "Name 3", 0.4);
        assertEquals(expectedRecord, chlRecords[3]);
    }

    @Test
    public void testGetValuesForYS_TimeOrdered() throws Exception {
        // execution
        InsituRecord[] ysRecords = insituSource.getValuesFor("ys", null);

        // verification
        assertEquals(4, ysRecords.length);

        InsituRecord expectedRecord;
        expectedRecord = new InsituRecord(new GeoPos(20, 40), getDate("02.04.2003"), "Name 2", 30);
        assertEquals(expectedRecord, ysRecords[0]);

        expectedRecord = new InsituRecord(new GeoPos(20, 40), getDate("05.04.2003"), "Name 2", 40);
        assertEquals(expectedRecord, ysRecords[1]);

        expectedRecord = new InsituRecord(new GeoPos(10, 30), getDate("08.04.2003"), "Name 1", 20);
        assertEquals(expectedRecord, ysRecords[2]);

        expectedRecord = new InsituRecord(new GeoPos(20, 50), getDate("11.04.2003"), "Name 3", 50);
        assertEquals(expectedRecord, ysRecords[3]);
    }

    @Test
    public void testGetValuesForGeoPos() throws Exception {
        // execution
        InsituRecord[] chlRecordsPos1 = insituSource.getValuesFor("ys", new GeoPos(20, 40));
        InsituRecord[] chlRecordsPos2 = insituSource.getValuesFor("ys", new GeoPos(10, 30));

        // verification
        assertEquals(2, chlRecordsPos1.length);
        assertEquals(1, chlRecordsPos2.length);

        InsituRecord expectedRecord;
        expectedRecord = new InsituRecord(new GeoPos(20, 40), getDate("02.04.2003"), "Name 2", 30);
        assertEquals(expectedRecord, chlRecordsPos1[0]);

        expectedRecord = new InsituRecord(new GeoPos(20, 40), getDate("05.04.2003"), "Name 2", 40);
        assertEquals(expectedRecord, chlRecordsPos1[1]);

        expectedRecord = new InsituRecord(new GeoPos(10, 30), getDate("08.04.2003"), "Name 1", 20);
        assertEquals(expectedRecord, chlRecordsPos2[0]);
    }

    @Test
    public void testGetParameterNames() throws Exception {
        // execution
        final String[] parameterNames = insituSource.getParameterNames();

        // verification
        assertArrayEquals(new String[]{"CHL", "ys"}, parameterNames);
    }

    private Date getDate(String dateString) throws ParseException {
        return dateFormat.parse(dateString);
    }
}
