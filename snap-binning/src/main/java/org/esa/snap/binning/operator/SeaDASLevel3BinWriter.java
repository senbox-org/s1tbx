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
import org.esa.snap.binning.support.SeadasGrid;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility class that writes SeaDAS-Level-3-alike NetCDF files containing binned Level-3 data.
 *
 * @author Norman Fomferra
 */
class SeaDASLevel3BinWriter extends AbstractBinWriter {

    private final static int BUFFER_SIZE = 4096;

    private BinningContext binningContext;
    private SeadasGrid seadasGrid;
    private PlanetaryGrid planetaryGrid;

    public SeaDASLevel3BinWriter(Geometry region, ProductData.UTC startTime, ProductData.UTC stopTime) {
        super(region, startTime, stopTime);
    }

    @Override
    public void setBinningContext(BinningContext binningContext) {
        this.binningContext = binningContext;
        planetaryGrid = binningContext.getPlanetaryGrid();
        this.seadasGrid = new SeadasGrid(planetaryGrid);
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
        final Dimension binListDim = netcdfFile.addDimension("bin_list", temporalBins.size());

        final Variable rowNumVar = netcdfFile.addVariable("bi_row_num", DataType.INT, new Dimension[]{binIndexDim});
        rowNumVar.addAttribute(new Attribute("comment", "zero-based index of row corresponding to each 'bin_index' record."));

        final Variable vsizeVar = netcdfFile.addVariable("bi_vsize", DataType.DOUBLE, new Dimension[]{binIndexDim});
        vsizeVar.addAttribute(new Attribute("comment", "north-south extent (degrees latitude) of bins for each row."));

        final Variable hsizeVar = netcdfFile.addVariable("bi_hsize", DataType.DOUBLE, new Dimension[]{binIndexDim});
        hsizeVar.addAttribute(new Attribute("comment", "east-west extent (degrees longitude) of bins for each row;\n" +
                "ranges from 360/SEAGrid_bins for the two equatorial rows to 120 for the two polar rows."));

        final Variable startNumVar = netcdfFile.addVariable("bi_start_num", DataType.INT, new Dimension[]{binIndexDim});
        startNumVar.addAttribute(new Attribute("comment", "1-based bin number of first bin in the grid for each row (see bi_begin);\n" +
                "always the same set of values for the set of rows."));
        startNumVar.addAttribute(new Attribute("missing_value", 0));
        startNumVar.addAttribute(new Attribute("_FillValue", 0));

        final Variable beginOffsetVar = netcdfFile.addVariable("bi_begin_offset", DataType.INT, new Dimension[]{binIndexDim});
        beginOffsetVar.addAttribute(new Attribute("comment", "0-based offset of the first data-containing bin in for each row."));
        beginOffsetVar.addAttribute(new Attribute("missing_value", -1));
        beginOffsetVar.addAttribute(new Attribute("_FillValue", -1));

        final Variable beginVar = netcdfFile.addVariable("bi_begin", DataType.INT, new Dimension[]{binIndexDim});
        beginVar.addAttribute(new Attribute("comment", "1-based bin number of first data-containing bin for each row (see bi_start_num)."));
        beginVar.addAttribute(new Attribute("missing_value", 0));
        beginVar.addAttribute(new Attribute("_FillValue", 0));

        final Variable extendVar = netcdfFile.addVariable("bi_extent", DataType.INT, new Dimension[]{binIndexDim});
        extendVar.addAttribute(new Attribute("comment", "number of bins actually stored (i.e. containing data for each row)."));

        final Variable maxVar = netcdfFile.addVariable("bi_max", DataType.INT, new Dimension[]{binIndexDim});
        maxVar.addAttribute(new Attribute("comment", "the maximum number of bin for each row; " +
                "ranges from 3 for the two polar rows to SEAGrid_bins for the two equatorial bins."));

        final Variable binNumVar = netcdfFile.addVariable("bl_bin_num", DataType.INT, new Dimension[]{binListDim});
        final Variable numObsVar = netcdfFile.addVariable("bl_nobs", DataType.INT, new Dimension[]{binListDim});
        final Variable numScenesVar = netcdfFile.addVariable("bl_nscenes", DataType.INT, new Dimension[]{binListDim});


        final BinManager binManager = binningContext.getBinManager();
        final String[] resultFeatureNames = binManager.getResultFeatureNames();
        final ArrayList<Variable> featureVars = new ArrayList<Variable>(resultFeatureNames.length);
        for (String featureName : resultFeatureNames) {
            final Variable featureVar = netcdfFile.addVariable("bl_" + featureName, DataType.FLOAT, new Dimension[]{binListDim});
            featureVar.addAttribute(new Attribute("_FillValue", Float.NaN));
            featureVars.add(featureVar);
        }

        netcdfFile.create();
        try {
            writeBinIndexVariables(netcdfFile, rowNumVar, vsizeVar, hsizeVar, startNumVar, maxVar);
            writeBinListVariables(netcdfFile, binNumVar, numObsVar, numScenesVar, featureVars, beginOffsetVar, beginVar, extendVar, temporalBins);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            netcdfFile.close();
        }
    }

    private void writeBinIndexVariables(final NetcdfFileWriteable netcdfFile,
                                        final Variable rowNumVar,
                                        final Variable vsizeVar,
                                        final Variable hsizeVar,
                                        final Variable startNumVar,
                                        final Variable maxVar) throws IOException, InvalidRangeException {
        writeBinIndexVariable(netcdfFile, rowNumVar, new BinIndexElementSetter() {
            @Override
            public void setArray(Array array, int rowIndex, SeadasGrid grid) {
                array.setInt(rowIndex, rowIndex);
            }
        });
        writeBinIndexVariable(netcdfFile, startNumVar, new BinIndexElementSetter() {
            @Override
            public void setArray(Array array, int rowIndex, SeadasGrid grid) {
                array.setInt(rowIndex, grid.getFirstBinIndex(seadasGrid.convertRowIndex(rowIndex)));
            }
        });
        writeBinIndexVariable(netcdfFile, vsizeVar, new BinIndexElementSetter() {
            @Override
            public void setArray(Array array, int rowIndex, SeadasGrid grid) {
                array.setDouble(rowIndex, 180.0 / grid.getNumRows());
            }
        });
        writeBinIndexVariable(netcdfFile, hsizeVar, new BinIndexElementSetter() {
            @Override
            public void setArray(Array array, int rowIndex, SeadasGrid grid) {
                array.setDouble(rowIndex, 360.0 / grid.getNumCols(seadasGrid.convertRowIndex(rowIndex)));
            }
        });
        writeBinIndexVariable(netcdfFile, maxVar, new BinIndexElementSetter() {
            @Override
            public void setArray(Array array, int rowIndex, SeadasGrid grid) {
                array.setInt(rowIndex, grid.getNumCols(seadasGrid.convertRowIndex(rowIndex)));
            }
        });
    }

    private void writeBinListVariables(final NetcdfFileWriteable netcdfFile,
                                       final Variable binNumVar,
                                       final Variable numObsVar,
                                       final Variable numScenesVar,
                                       final List<Variable> featureVars,
                                       final Variable beginOffsetVar,
                                       final Variable beginVar,
                                       final Variable extendVar,
                                       final List<TemporalBin> temporalBins) throws IOException, InvalidRangeException {

        ArrayList<BinListVar> binListVars = new ArrayList<BinListVar>();

        binListVars.add(new BinListVar(binNumVar, new BinListElementSetter() {
            @Override
            public void setArray(Array array, int binIndex, TemporalBin bin) {
                array.setInt(binIndex, seadasGrid.convertBinIndex(bin.getIndex()));
            }
        }));

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

        for (int featureIndex = 0; featureIndex < featureVars.size(); featureIndex++) {
            final int k = featureIndex;
            binListVars.add(new BinListVar(featureVars.get(k), new BinListElementSetter() {
                @Override
                public void setArray(Array array, int binIndex, TemporalBin bin) {
                    array.setFloat(binIndex, bin.getFeatureValues()[k]);
                }
            }));
        }

        final int[] binRowBeginOffsets = new int[planetaryGrid.getNumRows()];
        final long[] binRowBegins = new long[planetaryGrid.getNumRows()];
        final int[] binRowExtends = new int[planetaryGrid.getNumRows()];
        Arrays.fill(binRowBeginOffsets, -1);
        Arrays.fill(binRowBegins, -1);
        Arrays.fill(binRowExtends, 0);
        writeBinListVariable0(netcdfFile, temporalBins, binListVars, binRowBeginOffsets, binRowBegins, binRowExtends);

        writeBinIndexVariable(netcdfFile, beginOffsetVar, new BinIndexElementSetter() {
            @Override
            public void setArray(Array array, int rowIndex, SeadasGrid grid) {
                array.setInt(rowIndex, binRowBeginOffsets[seadasGrid.convertRowIndex(rowIndex)]);
            }
        });

        writeBinIndexVariable(netcdfFile, beginVar, new BinIndexElementSetter() {
            @Override
            public void setArray(Array array, int rowIndex, SeadasGrid grid) {
                final long id = binRowBegins[seadasGrid.convertRowIndex(rowIndex)];
                if (id == -1L) {
                    array.setInt(rowIndex, 0);
                } else {
                    array.setInt(rowIndex, seadasGrid.convertBinIndex(id));
                }
            }
        });

        writeBinIndexVariable(netcdfFile, extendVar, new BinIndexElementSetter() {
            @Override
            public void setArray(Array array, int rowIndex, SeadasGrid grid) {
                array.setInt(rowIndex, binRowExtends[seadasGrid.convertRowIndex(rowIndex)]);
            }
        });

    }

    private void writeBinIndexVariable(NetcdfFileWriteable netcdfFile,
                                       Variable variable,
                                       BinIndexElementSetter setter) throws IOException, InvalidRangeException {
        getLogger().info("Writing bin index variable " + variable.getFullName());
        final int numRows = seadasGrid.getNumRows();
        final Array array = Array.factory(variable.getDataType(), new int[]{numRows});
        for (int row = 0; row < numRows; row++) {
            setter.setArray(array, row, seadasGrid);
        }
        netcdfFile.write(variable.getFullName(), array);
    }

    private void writeBinListVariable0(NetcdfFileWriteable netcdfFile,
                                       List<TemporalBin> temporalBins,
                                       List<BinListVar> vars,
                                       int[] binRowBeginOffsets,
                                       long[] binRowBegins,
                                       int[] binRowExtends) throws IOException, InvalidRangeException {
        getLogger().info("Writing bin list variables");

        final int[] origin = new int[1];
        int bufferIndex = 0;
        int lastRowIndex = -1;
        int rowIndex = -1;
        ArrayList<TemporalBin> rowBins = new ArrayList<TemporalBin>(2 * planetaryGrid.getNumRows());

        // Reverse-iterate through the bins
        for (int i = temporalBins.size() - 1; i >= 0; i--) {
            final TemporalBin temporalBin = temporalBins.get(i);

            final long id = temporalBin.getIndex();

            rowIndex = planetaryGrid.getRowIndex(id);
            // Row change? If so we can write the bins of the same row.
            if (rowIndex == lastRowIndex) {
                rowBins.add(temporalBin);
            } else {
                if (!rowBins.isEmpty()) {
                    bufferIndex = writeRowBins(netcdfFile, rowBins, vars, origin, bufferIndex, lastRowIndex,
                            binRowBeginOffsets, binRowBegins, binRowExtends);
                    rowBins.clear();
                }
                rowBins.add(temporalBin);
                lastRowIndex = rowIndex;
            }
        }
        if (!rowBins.isEmpty()) {
            bufferIndex = writeRowBins(netcdfFile, rowBins, vars, origin, bufferIndex, rowIndex,
                    binRowBeginOffsets, binRowBegins, binRowExtends);
        }
        if (bufferIndex > 0) {
            writeBinListVars(netcdfFile, vars, origin, bufferIndex);
        }
    }

    private int writeRowBins(NetcdfFileWriteable netcdfFile,
                             ArrayList<TemporalBin> rowBins,
                             List<BinListVar> vars,
                             int[] origin,
                             int bufferIndex,
                             int rowIndex,
                             int[] binRowBeginOffsets,
                             long[] binRowBegins,
                             int[] binRowExtends) throws IOException, InvalidRangeException {
        int offset = origin[0] + bufferIndex;
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
        binRowBeginOffsets[rowIndex] = offset;
        binRowBegins[rowIndex] = rowBins.get(0).getIndex();
        binRowExtends[rowIndex] = rowBins.size();
        return bufferIndex;
    }
}
