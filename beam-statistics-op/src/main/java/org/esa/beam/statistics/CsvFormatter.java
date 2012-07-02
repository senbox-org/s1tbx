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
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of the {@link org.esa.beam.statistics.StatisticsOp.OutputStrategy} interface.
 * Writes the output to two files: the first file contains metadata, the second file contains the actual statistics.
 *
 * @author Thomas Storm
 */
class CsvFormatter implements StatisticsOp.OutputStrategy {

    private final PrintStream metadataOutput;
    private final PrintStream csvOutput;

    final Map<String, Map<String, Map<String, Double>>> data;

    CsvFormatter(PrintStream metadataOutput, PrintStream csvOutput) throws FileNotFoundException {
        this.metadataOutput = metadataOutput;
        this.csvOutput = csvOutput;
        data = new HashMap<String, Map<String, Map<String, Double>>>();
    }

    @Override
    public void initialiseOutput(Product[] sourceProducts, String[] algorithmNames, ProductData.UTC startDate, ProductData.UTC endDate,
                                 String[] regionIds) {
        metadataOutput.append("# BEAM Statistics export\n")
                .append("#")
                .append("# Products:\n");
        for (Product sourceProduct : sourceProducts) {
            metadataOutput.append("#              ")
                    .append(sourceProduct.getName())
                    .append("\n");
        }
        if (startDate != null) {
            metadataOutput.append("# Start Date: ")
                    .append(startDate.format())
                    .append("\n");
        }
        if (endDate != null) {
            metadataOutput.append("# End Date: ")
                    .append(endDate.format())
                    .append("\n");
        }
        metadataOutput.append("# Regions:\n");
        for (String regionId : regionIds) {
            metadataOutput.append("#              ")
                    .append(regionId)
                    .append("\n");
        }
        metadataOutput.append("#\n")
                .append("# Region")
                .append("\t")
                .append("Band");
        for (String algorithmName : algorithmNames) {
            metadataOutput.append("\t")
                    .append(algorithmName);
        }
    }

    @Override
    public void addToOutput(StatisticsOp.BandConfiguration bandConfiguration, String regionId, Map<String, Double> statistics) {
        if (!data.containsKey(bandConfiguration.sourceBandName)) {
            final HashMap<String, Map<String, Double>> dataForRegion = new HashMap<String, Map<String, Double>>();
            data.put(bandConfiguration.sourceBandName, dataForRegion);
        }
        if (!data.get(bandConfiguration.sourceBandName).containsKey(regionId)) {
            data.get(bandConfiguration.sourceBandName).put(regionId, new HashMap<String, Double>());
        }
        for (Map.Entry<String, Double> entry : statistics.entrySet()) {
            data.get(bandConfiguration.sourceBandName).get(regionId).put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void finaliseOutput() throws IOException {
        for (Map.Entry<String, Map<String, Map<String, Double>>> dataForBand : data.entrySet()) {
            for (Map.Entry<String, Map<String, Double>> dataForRegion : dataForBand.getValue().entrySet()) {
                csvOutput.append(dataForRegion.getKey())
                        .append("\t")
                        .append(dataForBand.getKey());
                for (Map.Entry<String, Double> dataForAlgorithm : dataForRegion.getValue().entrySet()) {
                    csvOutput.append("\t")
                            .append(String.valueOf(dataForAlgorithm.getValue()));
                }
                csvOutput.append("\n");
            }
        }
    }
}
