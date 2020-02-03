/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.operator;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.binning.BinManager;
import org.esa.snap.binning.BinningContext;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.TemporalBin;
import org.esa.snap.core.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility class that writes SeaDAS-Level-3-alike NetCDF files containing binned Level-3 data.
 *
 * @author Norman Fomferra
 */
class FullyPopulatedBinWriter extends AbstractBinWriter {

    private final static int BUFFER_SIZE = 4096;

    private BinningContext binningContext;
    private PlanetaryGrid planetaryGrid;

    public FullyPopulatedBinWriter(Geometry region, ProductData.UTC startTime, ProductData.UTC stopTime) {
        super(region, startTime, stopTime);
    }

    @Override
    public void setBinningContext(BinningContext binningContext) {
        this.binningContext = binningContext;
        planetaryGrid = binningContext.getPlanetaryGrid();
    }

    @Override
    public void write(Map<String, String> metadataProperties, List<TemporalBin> temporalBins) throws IOException {
        final NetcdfFileWriteable netcdfFile = NetcdfFileWriteable.createNew(getTargetFilePath());
        netcdfFile.setLargeFile(true);

        netcdfFile.addGlobalAttribute("title", "Level-3 Binned Data");
        netcdfFile.addGlobalAttribute("super_sampling", binningContext.getSuperSampling());
        if (getRegion() != null) {
            netcdfFile.addGlobalAttribute("region", getRegion().toText());
        }

        writeGlobalTimeCoverageMetadata(netcdfFile);
        writeGlobalCommonMetadata(metadataProperties, netcdfFile);
        writeGlobalSEAGridMetadata(netcdfFile, planetaryGrid.getNumRows());

        final Dimension binIndexDim = netcdfFile.addDimension("bin_index", planetaryGrid.getNumRows());
        long numBinsLong = planetaryGrid.getNumBins();
        if (numBinsLong > Integer.MAX_VALUE) {
            throw new IOException("Num_bins exceeds MAX_INT. Can not be written as fully populated grid.");
        }
        final Dimension binListDim = netcdfFile.addDimension("bin_list", (int) numBinsLong);


        final Variable numObsVar = netcdfFile.addVariable("bl_nobs", DataType.INT, new Dimension[]{binListDim});
        final Variable numScenesVar = netcdfFile.addVariable("bl_nscenes", DataType.INT, new Dimension[]{binListDim});

        Variable longitude = netcdfFile.addVariable("longitude", DataType.FLOAT, new Dimension[]{binListDim});
        longitude.addAttribute(new Attribute("units", "degrees_east"));
        Variable latitude = netcdfFile.addVariable("latitude", DataType.FLOAT, new Dimension[]{binListDim});
        latitude.addAttribute(new Attribute("units", "degrees_north"));

        final BinManager binManager = binningContext.getBinManager();
        final String[] resultFeatureNames = binManager.getResultFeatureNames();
        final ArrayList<Variable> featureVars = new ArrayList<Variable>(resultFeatureNames.length);
        for (String featureName : resultFeatureNames) {
            final Variable featureVar = netcdfFile.addVariable(featureName, DataType.FLOAT, new Dimension[]{binListDim});
            featureVar.addAttribute(new Attribute("_FillValue", Float.NaN));
            featureVars.add(featureVar);
        }

        netcdfFile.create();
        try {
            writeBinListVariables(netcdfFile, numObsVar, numScenesVar, latitude, longitude, featureVars, temporalBins);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            netcdfFile.close();
        }
    }

    private void writeBinListVariables(final NetcdfFileWriteable netcdfFile,
                                       final Variable numObsVar,
                                       final Variable numScenesVar,
                                       final Variable latitude,
                                       final Variable longitude,
                                       final List<Variable> featureVars,
                                       final List<TemporalBin> temporalBins) throws IOException, InvalidRangeException {

        ArrayList<BinListVar> binListVars = new ArrayList<BinListVar>();


        binListVars.add(new BinListVar(numObsVar, new BinListElementSetter() {
            @Override
            public void setArray(Array array, int binIndex, TemporalBin bin) {
                array.setInt(binIndex, bin.getNumObs());
            }
        }));

        binListVars.add(new BinListVar(numScenesVar, new BinListElementSetter() {
            @Override
            public void setArray(Array array, int binIndex, TemporalBin bin) {
                array.setInt(binIndex, bin.getNumPasses());
            }
        }));

        binListVars.add(new BinListVar(latitude, new BinListElementSetter() {
            @Override
            public void setArray(Array array, int binIndex, TemporalBin bin) {
                float centerLat = (float) planetaryGrid.getCenterLat(planetaryGrid.getRowIndex(bin.getIndex()));
                array.setFloat(binIndex, centerLat);
            }
        }));

        binListVars.add(new BinListVar(longitude, new BinListElementSetter() {
            @Override
            public void setArray(Array array, int binIndex, TemporalBin bin) {
                double[] centerLatLon = planetaryGrid.getCenterLatLon(bin.getIndex());
                array.setFloat(binIndex, (float) centerLatLon[1]);
            }
        }));

        for (int featureIndex = 0; featureIndex < featureVars.size(); featureIndex++) {
            final int k = featureIndex;
            binListVars.add(new BinListVar(featureVars.get(k), new BinListElementSetter() {
                @Override
                public void setArray(Array array, int binIndex, TemporalBin bin) {
                    if (bin.getFeatureValues().length == 0) {
                        array.setFloat(binIndex, Float.NaN);
                    } else {
                        array.setFloat(binIndex, bin.getFeatureValues()[k]);
                    }
                }
            }));
        }

        writeBinListVariable(netcdfFile, temporalBins, binListVars);
    }

    private void writeBinListVariable(NetcdfFileWriteable netcdfFile,
                                       List<TemporalBin> temporalBins,
                                       List<BinListVar> vars) throws IOException, InvalidRangeException {
        getLogger().info("Writing bin list variables");

        final int[] origin = new int[1];
        int bufferIndex = 0;
        int lastRowIndex = -1;
        int rowIndex = -1;
        ArrayList<TemporalBin> rowBins = new ArrayList<TemporalBin>(2 * planetaryGrid.getNumRows());

        // Reverse-iterate through the bins
        int binListIndex = temporalBins.size() - 1;
        TemporalBin temporalBin = null;
        TemporalBin emptyBin = new TemporalBin(0,0);
        int id = -1;
        final int numFeatures = 0;//temporalBins.get(0).getFeatureValues().length;
        for (int globalIndex = (int) (planetaryGrid.getNumBins() - 1); globalIndex >= 0; globalIndex--) {
            if (temporalBin == null && binListIndex >= 0) {
                temporalBin = temporalBins.get(binListIndex--);
                id = (int) temporalBin.getIndex();
            }

//            final long id = temporalBin.getIndex();

            rowIndex = planetaryGrid.getRowIndex(globalIndex);
            // Row change? If so we can write the bins of the same row.
            if (rowIndex == lastRowIndex) {
                if (id == globalIndex) {
                    rowBins.add(temporalBin);
                    temporalBin = null;
                    id = -1;
                } else {
//                    rowBins.add(new TemporalBin(globalIndex, numFeatures));
                    rowBins.add(emptyBin);
                }
            } else {
                bufferIndex = writeRowBins(netcdfFile, rowBins, vars, origin, bufferIndex);
                rowBins.clear();
                if (id == globalIndex) {
                    rowBins.add(temporalBin);
                    temporalBin = null;
                    id = -1;
                } else {
//                    rowBins.add(new TemporalBin(globalIndex, numFeatures));
                    rowBins.add(emptyBin);
                }
                lastRowIndex = rowIndex;
            }
        }
        bufferIndex = writeRowBins(netcdfFile, rowBins, vars, origin, bufferIndex);
        if (bufferIndex > 0) {
            writeBinListVars(netcdfFile, vars, origin, bufferIndex);
        }
    }

    private int writeRowBins(NetcdfFileWriteable netcdfFile,
                             ArrayList<TemporalBin> rowBins,
                             List<BinListVar> vars,
                             int[] origin,
                             int bufferIndex) throws IOException, InvalidRangeException {
        Collections.reverse(rowBins);
        for (TemporalBin rowBin : rowBins) {
            if (bufferIndex == BUFFER_SIZE) {
                writeBinListVars(netcdfFile, vars, origin);
                bufferIndex = 0;
                origin[0] += BUFFER_SIZE;
            }
            setBinListVarsArrayElement(vars, rowBin, bufferIndex);
            bufferIndex++;
        }
        return bufferIndex;
    }
}
