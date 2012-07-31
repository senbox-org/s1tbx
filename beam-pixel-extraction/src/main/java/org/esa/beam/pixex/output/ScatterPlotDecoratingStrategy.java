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

package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.measurement.Measurement;
import org.esa.beam.measurement.writer.FormatStrategy;
import org.esa.beam.util.logging.BeamLogManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Tonio Fincke
 * @author Thomas Storm
 */
public class ScatterPlotDecoratingStrategy implements FormatStrategy {

    private final Measurement[] originalMeasurements;
    private final FormatStrategy decoratedStrategy;
    private final Set<String[]> scatterPlotVariableCombinations;

    final Map<Long, Map<String, Integer>> rasterNamesIndices = new HashMap<Long, Map<String, Integer>>();
    final List<JFreeChart> plots = new ArrayList<JFreeChart>();
    private final RasterNamesFactory rasterNamesFactory;
    private final ProductRegistry productRegistry;
    private final File parent;
    private final String filePrefix;

    public ScatterPlotDecoratingStrategy(Measurement[] originalMeasurements, FormatStrategy decoratedStrategy,
                                         Set<String[]> scatterPlotVariableCombinations, RasterNamesFactory rasterNamesFactory,
                                         ProductRegistry productRegistry, File parentDirectory, String filePrefix) {
        this.originalMeasurements = originalMeasurements;
        this.decoratedStrategy = decoratedStrategy;
        this.scatterPlotVariableCombinations = scatterPlotVariableCombinations;
        this.rasterNamesFactory = rasterNamesFactory;
        this.productRegistry = productRegistry;
        this.parent = parentDirectory;
        this.filePrefix = filePrefix;
    }

    @Override
    public void writeHeader(PrintWriter writer, Product product) {
        decoratedStrategy.writeHeader(writer, product);
        fillRasterNamesIndicesMap(product);
    }

    void fillRasterNamesIndicesMap(Product product) {
        final long productId = getProductId(product);
        if (!rasterNamesIndices.containsKey(productId)) {
            rasterNamesIndices.put(productId, new HashMap<String, Integer>());
        }
        String[] rasterNames = rasterNamesFactory.getRasterNames(product);
        for (int i = 0; i < rasterNames.length; i++) {
            String rasterName = rasterNames[i];
            rasterNamesIndices.get(productId).put(rasterName, i);
        }
    }

    private long getProductId(Product product) {
        long productId;
        try {
            productId = productRegistry.getProductId(product);
        } catch (IOException e) {
            throw new IllegalStateException("Should never come here", e);
        }
        return productId;
    }

    @Override
    public void writeMeasurements(PrintWriter writer, Measurement[] measurements) {
        decoratedStrategy.writeMeasurements(writer, measurements);

        for (String[] variableCombination : scatterPlotVariableCombinations) {
            String scatterPlotName = String.format("Scatter plot of '%s' and '%s'", variableCombination[0], variableCombination[1]);
            XYDataset dataset = createDataset(variableCombination, measurements);
            JFreeChart scatterPlot = ChartFactory.createScatterPlot(scatterPlotName, variableCombination[0], variableCombination[1],
                                                                    dataset, PlotOrientation.VERTICAL, false, false, false);
            plots.add(scatterPlot);
            try {
                File targetFile = new File(parent,
                                           String.format("%s_scatter_plot_%s_%s.png",
                                                         filePrefix, variableCombination[0], variableCombination[1]));
                ChartUtilities.saveChartAsPNG(targetFile, scatterPlot, 600, 400);
            } catch (IOException e) {
                BeamLogManager.getSystemLogger().warning(e.getMessage());
            }
        }
    }

    private XYDataset createDataset(String[] variableCombination, Measurement[] measurements) {
        final XYSeriesCollection dataSet = new XYSeriesCollection();
        XYSeries data = new XYSeries("data");
        String originalVariableName = variableCombination[0];
        String productVariableName = variableCombination[1];
        for (Measurement measurement : measurements) {
            Measurement originalMeasurement = MatchupFormatStrategy.findMatchingMeasurement(measurement, originalMeasurements);
            final String[] originalAttributeNames = originalMeasurement.getOriginalAttributeNames();
            Object originalValue = null;
            for (int i = 0; i < originalAttributeNames.length; i++) {
                final String attributeName = originalAttributeNames[i];
                if (attributeName.equals(originalVariableName)) {
                    originalValue = originalMeasurement.getValues()[i];
                    break;
                }
            }
            Object value = getValue(productVariableName, measurement);
            data.add((Number) originalValue, (Number) value);
        }
        dataSet.addSeries(data);
        return dataSet;
    }

    private Object getValue(String productVariableName, Measurement measurement) {
        long productId = measurement.getProductId();
        int columnIndex = rasterNamesIndices.get(productId).get(productVariableName);
        return measurement.getValues()[columnIndex];
    }

}
