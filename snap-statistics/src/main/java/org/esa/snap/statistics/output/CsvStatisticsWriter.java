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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Writes the statistics to an instance of {@link PrintStream}.
 * The format written is a tab-separated CSV ASCII file, containing the actual statistics.
 *
 * @author Thomas Storm
 */
public class CsvStatisticsWriter implements StatisticsOutputter {

    private final PrintStream csvOutput;
    private String[] algorithmNames;
    final Statistics statisticsContainer;

    /**
     * Creates a new instance.
     *
     * @param csvOutput The target print stream where the statistics are written to.
     */
    public CsvStatisticsWriter(PrintStream csvOutput) {
        this.csvOutput = csvOutput;
        statisticsContainer = new Statistics();
    }

    /**
     * {@inheritDoc}
     *
     * @param statisticsOutputContext A context providing meta-information about the statistics.
     */
    @Override
    public void initialiseOutput(StatisticsOutputContext statisticsOutputContext) {
        this.algorithmNames = statisticsOutputContext.algorithmNames;
        Arrays.sort(algorithmNames);
    }

    /**
     * {@inheritDoc}
     *
     * @param bandName   The name of the band the statistics have been computed for.
     * @param regionId   The id of the region the statistics have been computed for.
     * @param statistics The actual statistics as map. Keys are the algorithm names, values are the actual statistical values.
     */
    @Override
    public void addToOutput(String bandName, String regionId, Map<String, Number> statistics) {
        if (!statisticsContainer.containsBand(bandName)) {
            statisticsContainer.put(bandName, new BandStatistics());
        }
        final BandStatistics dataForBandName = statisticsContainer.getDataForBandName(bandName);
        if (!dataForBandName.containsRegion(regionId)) {
            dataForBandName.put(regionId, new RegionStatistics());
        }
        final RegionStatistics dataForRegionName = dataForBandName.getDataForRegionName(regionId);
        for (Map.Entry<String, Number> entry : statistics.entrySet()) {
            dataForRegionName.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException Never.
     */
    @Override
    public void finaliseOutput() throws IOException {
        if (algorithmNames == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " not initialised.");
        }

        writeHeader();

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
                        final Number numberValue = dataForRegionName.getDataForAlgorithmName(algorithmName);
                        csvOutput.append(getValueAsString(numberValue));
                    }
                }
                csvOutput.append("\n");
            }
        }
    }

    private void writeHeader() {
        csvOutput.append("# Region")
                .append("\t")
                .append("Band");

        for (String algorithmName : algorithmNames) {
            csvOutput.append("\t")
                    .append(algorithmName);
        }
        csvOutput.append("\n");
    }

    static String getValueAsString(Number numberValue) {
        if (numberValue instanceof Float || numberValue instanceof Double) {
            return String.format(Locale.ENGLISH, "%.4f", numberValue.doubleValue());
        }
        return numberValue.toString();
    }

    static class Statistics {

        Map<String, BandStatistics> statistics = new HashMap<>();

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

        Map<String, RegionStatistics> bandStatistics = new HashMap<>();

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

        Map<String, Number> regionStatistics = new HashMap<>();

        Number getDataForAlgorithmName(String algorithmName) {
            return regionStatistics.get(algorithmName);
        }

        boolean containsAlgorithm(String algorithmName) {
            return regionStatistics.containsKey(algorithmName);
        }

        void put(String algorithmName, Number value) {
            regionStatistics.put(algorithmName, value);
        }
    }

}
