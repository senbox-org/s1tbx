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

package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.measurement.Measurement;
import org.esa.snap.measurement.writer.FormatStrategy;
import org.esa.snap.pixex.PixExOp;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tonio Fincke
 * @author Thomas Storm
 */
public class ScatterPlotDecoratingStrategy implements FormatStrategy {

    private final Measurement[] originalMeasurements;
    private final FormatStrategy decoratedStrategy;
    private final PixExOp.VariableCombination[] scatterPlotVariableCombinations;

    final Map<Long, Map<String, Integer>> rasterNamesIndices = new HashMap<Long, Map<String, Integer>>();
    final Map<Long, Map<PixExOp.VariableCombination, JFreeChart>> plotMaps = new HashMap<Long, Map<PixExOp.VariableCombination, JFreeChart>>();
    private final RasterNamesFactory rasterNamesFactory;
    private final ProductRegistry productRegistry;
    private final Map<Long, String> productNames = new HashMap<Long, String>();
    private final File parent;
    private final String filePrefix;

    public ScatterPlotDecoratingStrategy(Measurement[] originalMeasurements, FormatStrategy decoratedStrategy,
                                         PixExOp.VariableCombination[] scatterPlotVariableCombinations,
                                         RasterNamesFactory rasterNamesFactory,
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
    }

    void updateRasterNamesMaps(Product product) {
        final long productId = getProductId(product);
        productNames.put(productId, product.getName());
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
    public void writeMeasurements(Product product, PrintWriter writer, Measurement[] measurements) {
        updateRasterNamesMaps(product);
        decoratedStrategy.writeMeasurements(product, writer, measurements);
        for (PixExOp.VariableCombination variableCombination : scatterPlotVariableCombinations) {
            if (!combinationHasData(variableCombination.productVariableName, measurements)) {
                continue;
            }
            addData(variableCombination, measurements);
        }
    }

    @Override
    public void finish() {
        for (Map.Entry<Long, Map<PixExOp.VariableCombination, JFreeChart>> mapEntry : plotMaps.entrySet()) {
            final Map<PixExOp.VariableCombination, JFreeChart> plots = mapEntry.getValue();
            final Long productId = mapEntry.getKey();
            for (Map.Entry<PixExOp.VariableCombination, JFreeChart> entry : plots.entrySet()) {
                final PixExOp.VariableCombination variableCombination = entry.getKey();
                try {
                    File targetFile = new File(parent,
                                               String.format("%s_scatter_plot_%s_%s_%s.png",
                                                             filePrefix,
                                                             variableCombination.originalVariableName,
                                                             variableCombination.productVariableName,
                                                             productNames.get(productId))
                    );
                    ChartUtilities.saveChartAsPNG(targetFile, entry.getValue(), 600, 400);
                } catch (IOException e) {
                    SystemUtils.LOG.warning(e.getMessage());
                }
            }
        }
    }

    private void addData(PixExOp.VariableCombination variableCombination, Measurement[] measurements) {
        for (Measurement measurement : measurements) {
            addData(variableCombination, measurement);
        }
    }

    private void addData(PixExOp.VariableCombination variableCombination, Measurement measurement) {
        final long productId = measurement.getProductId();
        initPlotMaps(productId);
        XYSeries xySeries = getXYSeries(variableCombination, productId);
        Measurement originalMeasurement = MatchupFormatStrategy.findMatchingMeasurement(measurement,
                                                                                        originalMeasurements);
        if (!combinationHasData(variableCombination.productVariableName, measurement.getProductId())) {
            return;
        }
        final String[] originalAttributeNames = originalMeasurement.getOriginalAttributeNames();
        String originalValue = "";
        for (int i = 0; i < originalAttributeNames.length; i++) {
            final String attributeName = originalAttributeNames[i];
            if (attributeName.equals(variableCombination.originalVariableName)) {
                originalValue = originalMeasurement.getValues()[i].toString();
                break;
            }
        }
        Object value = getValue(variableCombination.productVariableName, measurement);
        xySeries.add(getOriginalMeasurementAsNumber(originalValue), (Number) value);
    }

    private XYSeries getXYSeries(PixExOp.VariableCombination variableCombination, long productId) {
        final XYSeries xySeries;
        if (plotMaps.get(productId).containsKey(variableCombination)) {
            xySeries = ((XYSeriesCollection) plotMaps.get(productId).get(variableCombination).getXYPlot().getDataset()).getSeries(0);
        } else {
            xySeries = new XYSeries("data");
            JFreeChart scatterPlot = createScatterPlot(variableCombination, xySeries, productId);
            plotMaps.get(productId).put(variableCombination, scatterPlot);
        }
        return xySeries;
    }

    private JFreeChart createScatterPlot(PixExOp.VariableCombination variableCombination, XYSeries dataset, long productId) {
        final XYSeriesCollection data = new XYSeriesCollection();
        data.addSeries(dataset);
        String scatterPlotName = String.format("Scatter plot of '%s' and '%s' for product '%s'",
                                               variableCombination.originalVariableName,
                                               variableCombination.productVariableName,
                                               productNames.get(productId));
        return ChartFactory.createScatterPlot(scatterPlotName,
                                              variableCombination.originalVariableName,
                                              variableCombination.productVariableName,
                                              data, PlotOrientation.VERTICAL, false, false,
                                              false);
    }

    private void initPlotMaps(long productId) {
        if (!plotMaps.containsKey(productId)) {
            plotMaps.put(productId, new HashMap<PixExOp.VariableCombination, JFreeChart>());
        }
    }

    private Number getOriginalMeasurementAsNumber(String value) {
        if (StringUtils.isNumeric(value, Integer.class)) {
            return Integer.parseInt(value);
        } else if (StringUtils.isNumeric(value, Double.class)) {
            return Double.parseDouble(value);
        } else if (StringUtils.isNumeric(value, Byte.class)) {
            return Byte.parseByte(value);
        } else if (StringUtils.isNumeric(value, Float.class)) {
            return Float.parseFloat(value);
        } else if (StringUtils.isNumeric(value, Long.class)) {
            return Long.parseLong(value);
        } else if (StringUtils.isNumeric(value, Short.class)) {
            return Short.parseShort(value);
        } else {
            return Double.NaN;
        }
    }

    private Object getValue(String productVariableName, Measurement measurement) {
        long productId = measurement.getProductId();
        int columnIndex = rasterNamesIndices.get(productId).get(productVariableName);
        return measurement.getValues()[columnIndex];
    }

    private boolean combinationHasData(String productVariableName, Measurement[] measurements) {
        for (Measurement measurement : measurements) {
            long productId = measurement.getProductId();
            if (combinationHasData(productVariableName, productId)) {
                return true;
            }
        }
        return false;
    }

    private boolean combinationHasData(String productVariableName, long productId) {
        return (rasterNamesIndices.containsKey(productId) && rasterNamesIndices.get(productId).containsKey(productVariableName));
    }

}
