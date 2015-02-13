/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.support;

import org.esa.beam.binning.Bin;
import org.esa.beam.binning.BinManager;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.SpatialBin;
import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.VariableContext;
import org.esa.beam.binning.Vector;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Traces all activities that happen to a given bin.
 */
public class BinTracer {

    private static final String SYSPROP_TRACE_LAT_LON = "beam.binning.traceLatLon";

    private final long binIndex;
    private final String[] obsNames;
    private final String[] spatialFeatureNames;
    private final String[] temporalFeatureNames;
    private final String[] outputFeatureNames;
    private final String[] postFeatureNames;
    private final String outputFile;
    private String productName;
    private boolean spatialHeaderWritten;
    private boolean temporalHeaderWritten;
    private PrintStream out;

    public static boolean traceThis(BinTracer binTracer, long binIndex) {
        return binTracer != null && binTracer.binIndex == binIndex;
    }

    public static boolean isActive() {
        return System.getProperty(SYSPROP_TRACE_LAT_LON) != null;
    }

    public static BinTracer create(BinManager binManager, PlanetaryGrid planetaryGridInst, String productOutputFile) {
        long binIndex = getBinIndexToTrace(planetaryGridInst);
        if (binIndex == -1) {
            return null;
        }
        VariableContext variableContext = binManager.getVariableContext();
        String[] obsNames = new String[variableContext.getVariableCount()];
        for (int i = 0; i < obsNames.length; i++) {
            obsNames[i] = variableContext.getVariableName(i);
        }
        String filename = "";
        if (productOutputFile != null) {
            filename = productOutputFile.substring(0, productOutputFile.lastIndexOf(".")) + "_";
        }
        filename = filename + "bintrace_" + binIndex + ".csv";
        return new BinTracer(binIndex,
                             obsNames,
                             binManager.getSpatialFeatureNames(),
                             binManager.getTemporalFeatureNames(),
                             binManager.getOutputFeatureNames(),
                             binManager.getPostProcessFeatureNames(),
                             filename);
    }

    private static long getBinIndexToTrace(PlanetaryGrid planetaryGrid) {
        String latLonString = System.getProperty(SYSPROP_TRACE_LAT_LON);
        if (latLonString != null && latLonString.contains(",")) {
            String[] latLon = latLonString.split(",");
            if (latLon.length == 2) {
                double lat = Double.parseDouble(latLon[0]);
                double lon = Double.parseDouble(latLon[1]);
                long binIndex = planetaryGrid.getBinIndex(lat, lon);
                String msg = String.format("Bin tracing enabled for lat=%s lon=%s binIndex=%d", lat, lon, binIndex);
                BeamLogManager.getSystemLogger().info(msg);
                return binIndex;
            }
        }
        return -1;
    }

    private BinTracer(long binIndex, String[] obsNames, String[] spatialFeatureNames, String[] temporalFeatureNames, String[] outputFeatureNames, String[] postFeatureNames, String outputFile) {
        this.binIndex = binIndex;
        this.obsNames = obsNames;
        this.spatialFeatureNames = spatialFeatureNames;
        this.temporalFeatureNames = temporalFeatureNames;
        this.outputFeatureNames = outputFeatureNames;
        this.postFeatureNames = postFeatureNames;
        this.outputFile = outputFile;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    private void ensureOutputOpen() {
        if (out == null) {
            try {
                out = new PrintStream(new FileOutputStream(outputFile), true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                out = System.out;
            }
        }
    }

    private void printHeaderSpatial() {
        out.println();
        out.println("spatial aggregation");
        out.println();

        out.print(",obs");
        printCategory("obs", obsNames.length);
        printCategory("spatial", spatialFeatureNames.length);
        out.println();

        out.print("action,productName");
        printHeads(obsNames);
        printHeads(spatialFeatureNames);
        out.println();
    }

    private void printHeaderTemporal() {
        out.println();
        out.println("temporal aggregation");
        out.println();

        printCategory("spatial", spatialFeatureNames.length);
        printCategory("temporal", temporalFeatureNames.length);
        out.println();

        out.print("action");
        printHeads(spatialFeatureNames);
        printHeads(temporalFeatureNames);
        out.println();
    }


    public void traceSpatial(String action, Observation observation, SpatialBin spatialBin) {
        ensureOutputOpen();
        if (!spatialHeaderWritten) {
            printHeaderSpatial();
            spatialHeaderWritten = true;
        }
        out.print(action + "," + productName);
        if (observation != null) {
            printVector(observation);
        } else {
            printEmtpy(obsNames.length);
        }
        printBin(spatialBin, spatialFeatureNames);
        out.println();
    }

    public void traceTemporal(String action, SpatialBin spatialBin, TemporalBin temporalBin) {
        ensureOutputOpen();
        if (!temporalHeaderWritten) {
            printHeaderTemporal();
            temporalHeaderWritten = true;
        }
        out.print(action);
        printBin(spatialBin, spatialFeatureNames);
        printBin(temporalBin, temporalFeatureNames);
        out.println();
    }

    public void traceOutput(TemporalBin temporalBin, Vector outputVector) {
        ensureOutputOpen();
        out.println();
        out.println("output computation");
        out.println();

        printHeads(temporalFeatureNames);
        out.println();
        out.print("temporal");
        printBin(temporalBin, temporalFeatureNames);
        out.println();

        printHeads(outputFeatureNames);
        out.println();
        out.print("output");
        printVector(outputVector);
        out.println();
    }

    public void tracePost(TemporalBin temporalBin, TemporalBin processBin) {
        ensureOutputOpen();
        out.println();
        out.println("post processing");
        out.println();

        printHeads(outputFeatureNames);
        out.println();
        out.print("output");
        printBin(temporalBin, outputFeatureNames);
        out.println();

        printHeads(postFeatureNames);
        out.println();
        out.print("processed");
        printBin(processBin, postFeatureNames);
        out.println();
    }

    private void printCategory(String cat, int length) {
        for (int i = 0; i < length; i++) {
            out.print("," + cat);
        }
    }

    private void printHeads(String[] names) {
        for (String name : names) {
            out.print("," + name);
        }
    }

    private void printBin(Bin bin, String[] featureNames) {
        if (bin != null) {
            float[] featureValues = bin.getFeatureValues();
            for (float value : featureValues) {
                out.print("," + value);
            }
        } else {
            printEmtpy(featureNames.length);
        }
    }

    private void printEmtpy(int length) {
        for (int i = 0; i < length; i++) {
            out.print(",");
        }
    }

    private void printVector(Vector vector) {
        for (int i = 0; i < vector.size(); i++) {
            out.print("," + vector.get(i));
        }
    }


}
