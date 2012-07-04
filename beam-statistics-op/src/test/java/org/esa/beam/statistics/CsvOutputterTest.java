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

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class CsvOutputterTest {

    private CsvOutputter csvOutputter;
    private StringBuilder metadataOutput;
    private StringBuilder csvOutput;
    private PrintStream metadataStream;
    private PrintStream csvStream;

    @Before
    public void setUp() throws Exception {
        metadataOutput = new StringBuilder();
        csvOutput = new StringBuilder();
        metadataStream = new PrintStream(new StringOutputStream(metadataOutput));
        csvStream = new PrintStream(new StringOutputStream(csvOutput));
        csvOutputter = new CsvOutputter(metadataStream, csvStream);
    }

    @Test
    public void testInitialiseOutput() throws Exception {
        final Product[] sourceProducts = {new Product("MER_RR__2PBCMsomething", "type", 10, 10)};
        final ProductData.UTC startDate = ProductData.UTC.parse("2010-01-01", "yyyy-MM-dd");
        final ProductData.UTC endDate = ProductData.UTC.parse("2011-01-01", "yyyy-MM-dd");
        final String[] regionIds = {"bullerbue", "bielefeld"};
        final String[] algorithmNames = new String[]{"p90", "p95", "min", "max"};
        final String[] bandNames = new String[0];

        csvOutputter.initialiseOutput(sourceProducts, bandNames, algorithmNames, startDate, endDate, regionIds);
        metadataStream.close();

        assertEquals("# BEAM Statistics export\n" +
                     "## Products:\n" +
                     "#              MER_RR__2PBCMsomething\n" +
                     "# Start Date: 01-JAN-2010 00:00:00.000000\n" +
                     "# End Date: 01-JAN-2011 00:00:00.000000\n" +
                     "# Regions:\n" +
                     "#              bullerbue\n" +
                     "#              bielefeld\n" +
                     "#\n" +
                     "#\n" +
                     "#\n" +
                     "# Region\tBand\tmax\tmin\tp90\tp95"
                , metadataOutput.toString());

    }

    @Ignore // todo - make run and comment in
    @Test
    public void testAddToOutput() throws Exception {
        addOutput();

        assertEquals(csvOutputter.statisticsContainer.getDataForBandName("normalised_cow_density_index_(ncdi)")
                             .getDataForRegionName("werdohl")
                             .getDataForAlgorithmName("p95"), 3.0, 1E-6);
        assertEquals(csvOutputter.statisticsContainer.getDataForBandName("normalised_cow_density_index_(ncdi)")
                             .getDataForRegionName("werdohl")
                             .getDataForAlgorithmName("p90"), 2.0, 1E-6);

        assertEquals(csvOutputter.statisticsContainer.getDataForBandName("normalised_pig_density_index_(npdi)")
                             .getDataForRegionName("bielefeld")
                             .getDataForAlgorithmName("p90"), 1.0, 1E-6);
        assertEquals(csvOutputter.statisticsContainer.getDataForBandName("normalised_pig_density_index_(npdi)")
                             .getDataForRegionName("bielefeld")
                             .getDataForAlgorithmName("p95"), 2.0, 1E-6);
        assertEquals(csvOutputter.statisticsContainer.getDataForBandName("normalised_pig_density_index_(npdi)")
                             .getDataForRegionName("bielefeld")
                             .getDataForAlgorithmName("max"), 3.0, 1E-6);
        assertEquals(csvOutputter.statisticsContainer.getDataForBandName("normalised_pig_density_index_(npdi)")
                             .getDataForRegionName("bielefeld")
                             .getDataForAlgorithmName("min"), 0.5, 1E-6);

        assertEquals(csvOutputter.statisticsContainer.getDataForBandName("normalised_cow_density_index_(ncdi)")
                             .getDataForRegionName("bielefeld")
                             .getDataForAlgorithmName("p90"), 1.0, 1E-6);
    }

    @Test
    public void testFinaliseOutput() throws Exception {
        csvOutputter.initialiseOutput(new Product[0], new String[0], new String[]{
                "p90",
                "p95",
                "max",
                "min"
        }, null, null, new String[]{"werdohl", "bielefeld"});
        addOutput();
        csvOutputter.finaliseOutput();
        csvStream.close();
        assertEquals("werdohl\tnormalised_cow_density_index_(ncdi)\t\t\t2.0\t3.0\n" +
                     "bielefeld\tnormalised_cow_density_index_(ncdi)\t\t\t1.0\t3.0\n" +
                     "bielefeld\tnormalised_pig_density_index_(npdi)\t3.0\t0.5\t1.0\t2.0\n"
                , csvOutput.toString());
    }

    private void addOutput() {
        final HashMap<String, Double> statistics = new HashMap<String, Double>();
        statistics.put("p90", 2.0);
        statistics.put("p95", 3.0);

        csvOutputter.addToOutput("normalised_cow_density_index_(ncdi)", "werdohl", statistics);

        statistics.put("p90", 1.0);

        csvOutputter.addToOutput("normalised_cow_density_index_(ncdi)", "bielefeld", statistics);

        statistics.put("p90", 1.0);
        statistics.put("p95", 2.0);
        statistics.put("max", 3.0);
        statistics.put("min", 0.5);

        csvOutputter.addToOutput("normalised_pig_density_index_(npdi)", "bielefeld", statistics);
    }


    private static class StringOutputStream extends OutputStream {

        StringBuilder builder;

        private StringOutputStream(StringBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void write(int b) throws IOException {
            byte b1 = (byte) b;
            builder.append(new String(new byte[]{b1}));
        }
    }
}
