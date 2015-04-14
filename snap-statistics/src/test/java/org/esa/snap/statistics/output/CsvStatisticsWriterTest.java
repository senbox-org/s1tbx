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

package org.esa.snap.statistics.output;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class CsvStatisticsWriterTest {

    private CsvStatisticsWriter csvStatisticsWriter;
    private StringBuilder csvOutput;
    private PrintStream csvStream;

    @Before
    public void setUp() throws Exception {
        csvOutput = new StringBuilder();
        csvStream = new PrintStream(new StringOutputStream(csvOutput));
        csvStatisticsWriter = new CsvStatisticsWriter(csvStream);
    }

    @Test
    public void testAddToOutput() throws Exception {
        addOutput();

        assertEquals(csvStatisticsWriter.statisticsContainer.getDataForBandName("normalised_cow_density_index_(ncdi)")
                             .getDataForRegionName("werdohl")
                             .getDataForAlgorithmName("p95").doubleValue(), 3.0, 1E-6);
        assertEquals(csvStatisticsWriter.statisticsContainer.getDataForBandName("normalised_cow_density_index_(ncdi)")
                             .getDataForRegionName("werdohl")
                             .getDataForAlgorithmName("p90").doubleValue(), 2.0, 1E-6);

        assertEquals(csvStatisticsWriter.statisticsContainer.getDataForBandName("normalised_pig_density_index_(npdi)")
                             .getDataForRegionName("bielefeld")
                             .getDataForAlgorithmName("p90").doubleValue(), 1.0, 1E-6);
        assertEquals(csvStatisticsWriter.statisticsContainer.getDataForBandName("normalised_pig_density_index_(npdi)")
                             .getDataForRegionName("bielefeld")
                             .getDataForAlgorithmName("p95").doubleValue(), 2.0, 1E-6);
        assertEquals(csvStatisticsWriter.statisticsContainer.getDataForBandName("normalised_pig_density_index_(npdi)")
                             .getDataForRegionName("bielefeld")
                             .getDataForAlgorithmName("max").doubleValue(), 3.0, 1E-6);
        assertEquals(csvStatisticsWriter.statisticsContainer.getDataForBandName("normalised_pig_density_index_(npdi)")
                             .getDataForRegionName("bielefeld")
                             .getDataForAlgorithmName("min").doubleValue(), 0.5, 1E-6);

        assertEquals(csvStatisticsWriter.statisticsContainer.getDataForBandName("normalised_cow_density_index_(ncdi)")
                             .getDataForRegionName("bielefeld")
                             .getDataForAlgorithmName("p90").doubleValue(), 1.0, 1E-6);
    }

    @Test
    public void testFinaliseOutput() throws Exception {
        csvStatisticsWriter.initialiseOutput(StatisticsOutputContext.create(null, new String[]{
                "p90",
                "p95",
                "max",
                "min"
        }));
        addOutput();
        csvStatisticsWriter.finaliseOutput();
        csvStream.close();
        String actualOutput = csvOutput.toString();
        assertTrue(actualOutput.startsWith("# Region\tBand\tmax\tmin\tp90\tp95\n"));
        assertTrue(actualOutput.contains("werdohl\tnormalised_cow_density_index_(ncdi)\t\t\t2.0000\t3.0000\n"));
        assertTrue(actualOutput.contains("bielefeld\tnormalised_cow_density_index_(ncdi)\t\t\t1.0000\t3.0000\n"));
        assertTrue(actualOutput.contains("bielefeld\tnormalised_pig_density_index_(npdi)\t3.0000\t0.5000\t1.0000\t2.0000\n"));
    }

    @Test
    public void testGetValueAsString() throws Exception {
        assertEquals("0.0016", CsvStatisticsWriter.getValueAsString(1.6E-3));
        assertEquals("10.0000", CsvStatisticsWriter.getValueAsString(10.0));
        assertEquals("10", CsvStatisticsWriter.getValueAsString(10));
    }

    private void addOutput() {
        final HashMap<String, Number> statistics = new HashMap<String, Number>();
        statistics.put("p90", 2.0);
        statistics.put("p95", 3.0);

        csvStatisticsWriter.addToOutput("normalised_cow_density_index_(ncdi)", "werdohl", statistics);

        statistics.put("p90", 1.0);

        csvStatisticsWriter.addToOutput("normalised_cow_density_index_(ncdi)", "bielefeld", statistics);

        statistics.put("p90", 1.0);
        statistics.put("p95", 2.0);
        statistics.put("max", 3.0);
        statistics.put("min", 0.5);

        csvStatisticsWriter.addToOutput("normalised_pig_density_index_(npdi)", "bielefeld", statistics);
    }


    static class StringOutputStream extends OutputStream {

        StringBuilder builder;

        StringOutputStream(StringBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void write(int b) throws IOException {
            byte b1 = (byte) b;
            builder.append(new String(new byte[]{b1}));
        }
    }
}
