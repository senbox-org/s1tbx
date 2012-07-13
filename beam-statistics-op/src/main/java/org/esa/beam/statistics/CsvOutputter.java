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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Writes the output to two files: the first file contains metadata, the second file contains the actual statistics.
 *
 * @author Thomas Storm
 */
class CsvOutputter implements StatisticsOp.Outputter {

    private final PrintStream metadataOutput;
    private final PrintStream csvOutput;

    final Statistics statisticsContainer;

    private String[] algorithmNames;

    CsvOutputter(PrintStream metadataOutput, PrintStream csvOutput) throws FileNotFoundException {
        this.metadataOutput = metadataOutput;
        this.csvOutput = csvOutput;
        statisticsContainer = new Statistics();
    }

    @Override
    public void initialiseOutput(Product[] sourceProducts, String[] bandNames, String[] algorithmNames, ProductData.UTC startDate, ProductData.UTC endDate,
                                 String[] regionIds) {
        Arrays.sort(algorithmNames);
        this.algorithmNames = algorithmNames;
        metadataOutput.append("# BEAM Statistics export\n")
                .append("#\n")
                .append("# Products:\n");
        for (Product sourceProduct : sourceProducts) {
            metadataOutput.append("#              ")
                    .append(sourceProduct.getName())
                    .append("\n");
        }
        if (startDate != null) {
            metadataOutput
                    .append("#\n")
                    .append("# Start Date: ")
                    .append(startDate.format())
                    .append("\n");
        }
        if (endDate != null) {
            metadataOutput
                    .append("#\n")
                    .append("# End Date: ")
                    .append(endDate.format())
                    .append("\n");
        }
        metadataOutput.append("#\n");
        metadataOutput.append("# Regions:\n");
        for (String regionId : regionIds) {
            metadataOutput.append("#              ")
                    .append(regionId)
                    .append("\n");
        }
    }

    @Override
    public void addToOutput(String bandName, String regionId, Map<String, Double> statistics) {
        if (!statisticsContainer.containsBand(bandName)) {
            statisticsContainer.put(bandName, new BandStatistics());
        }
        final BandStatistics dataForBandName = statisticsContainer.getDataForBandName(bandName);
        if (!dataForBandName.containsRegion(regionId)) {
            dataForBandName.put(regionId, new RegionStatistics());
        }
        for (Map.Entry<String, Double> entry : statistics.entrySet()) {
            final RegionStatistics dataForRegionName = dataForBandName.getDataForRegionName(regionId);
            dataForRegionName.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void finaliseOutput() throws IOException {
        if (algorithmNames == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " not initialised.");
        }

        csvOutput.append("# Region")
                .append("\t")
                .append("Band");

        for (String algorithmName : algorithmNames) {
            csvOutput.append("\t")
                    .append(algorithmName);
        }
        csvOutput.append("\n");

        for (String bandName : statisticsContainer.getBandNames()) {
            final BandStatistics bandStatistics = statisticsContainer.getDataForBandName(bandName);
            for (String regionName : bandStatistics.getRegionNames()) {
                csvOutput.append(regionName)
                        .append("\t")
                        .append(bandName);
                for (String algorithmName : algorithmNames) {
                    csvOutput.append("\t");
                    final RegionStatistics dataForRegionName = bandStatistics.getDataForRegionName(regionName);
                    if (dataForRegionName.containsAlgorithm(algorithmName)) {
                        final double doubleValue = dataForRegionName.getDataForAlgorithmName(algorithmName);
                        final String stringValue = String.valueOf(doubleValue);
                        csvOutput.append(stringValue.substring(0, Math.min(
                                stringValue.lastIndexOf('.') + 6, stringValue.length())));
                    }
                }
                csvOutput.append("\n");
            }
        }
    }

    static class Statistics {

        Map<String, BandStatistics> statistics = new HashMap<String, BandStatistics>();

        BandStatistics getDataForBandName(String bandName) {
            return statistics.get(bandName);
        }

        boolean containsBand(String bandName) {
            return statistics.containsKey(bandName);
        }

        String[] getBandNames() {
            final Set<String> bandNames = statistics.keySet();
            return bandNames.toArray(new String[bandNames.size()]);
        }

        void put(String bandName, BandStatistics bandStatistics) {
            statistics.put(bandName, bandStatistics);
        }
    }

    static class BandStatistics {

        Map<String, RegionStatistics> bandStatistics = new HashMap<String, RegionStatistics>();

        RegionStatistics getDataForRegionName(String regionName) {
            return bandStatistics.get(regionName);
        }

        boolean containsRegion(String regionName) {
            return bandStatistics.containsKey(regionName);
        }

        String[] getRegionNames() {
            final Set<String> regionNames = bandStatistics.keySet();
            return regionNames.toArray(new String[regionNames.size()]);
        }

        void put(String regionName, RegionStatistics regionStatistics) {
            bandStatistics.put(regionName, regionStatistics);
        }
    }

    static class RegionStatistics {

        Map<String, Double> regionStatistics = new HashMap<String, Double>();

        double getDataForAlgorithmName(String algorithmName) {
            return regionStatistics.get(algorithmName);
        }

        boolean containsAlgorithm(String algorithmName) {
            return regionStatistics.containsKey(algorithmName);
        }

        void put(String algorithmName, double value) {
            regionStatistics.put(algorithmName, value);
        }
    }

}
