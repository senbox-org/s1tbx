package org.esa.beam.binning.operator;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.binning.support.SeadasGrid;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class that writes SeaDAS-Level-3-alike NetCDF files containing binned Level-3 data.
 *
 * @author Norman Fomferra
 */
public class BinWriter {

    final static int BUFFER_SIZE = 4096;
    final Logger logger;
    private final BinningContext binningContext;
    private final Geometry region;
    private final ProductData.UTC startTime;
    private final ProductData.UTC stopTime;
    private SeadasGrid seadasGrid;
    private final PlanetaryGrid planetaryGrid;

    public BinWriter(BinningContext binningContext, Logger logger, Geometry region, ProductData.UTC startTime, ProductData.UTC stopTime) {
        this.logger = logger;
        this.binningContext = binningContext;
        this.region = region;
        this.startTime = startTime;
        this.stopTime = stopTime;
        planetaryGrid = binningContext.getPlanetaryGrid();
        this.seadasGrid = new SeadasGrid(planetaryGrid);
    }

    public void write(File filePath,
                      List<TemporalBin> temporalBins) throws IOException, InvalidRangeException {

        final NetcdfFileWriteable netcdfFile = NetcdfFileWriteable.createNew(filePath.getPath());

        netcdfFile.addGlobalAttribute("title", "Level-3 Binned Data");
        netcdfFile.addGlobalAttribute("super_sampling", binningContext.getSuperSampling());
        if (region != null) {
            netcdfFile.addGlobalAttribute("region", region.toText());
        }
        DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd");
        netcdfFile.addGlobalAttribute("start_time", startTime != null ? dateFormat.format(startTime.getAsDate()) : "");
        netcdfFile.addGlobalAttribute("stop_time", stopTime != null ? dateFormat.format(stopTime.getAsDate()) : "");

        netcdfFile.addGlobalAttribute("SEAGrid_bins", 2 * planetaryGrid.getNumRows());
        netcdfFile.addGlobalAttribute("SEAGrid_radius", SEAGrid.RE);
        netcdfFile.addGlobalAttribute("SEAGrid_max_north", +90.0);
        netcdfFile.addGlobalAttribute("SEAGrid_max_south", -90.0);
        netcdfFile.addGlobalAttribute("SEAGrid_seam_lon", -180.0);

        final Dimension binIndexDim = netcdfFile.addDimension("bin_index", planetaryGrid.getNumRows());
        final Dimension binListDim = netcdfFile.addDimension("bin_list", temporalBins.size());

        final Variable rowNumVar = netcdfFile.addVariable("bi_row_num", DataType.INT, new Dimension[]{binIndexDim});
        final Variable vsizeVar = netcdfFile.addVariable("bi_vsize", DataType.DOUBLE, new Dimension[]{binIndexDim});
        final Variable hsizeVar = netcdfFile.addVariable("bi_hsize", DataType.DOUBLE, new Dimension[]{binIndexDim});
        final Variable startNumVar = netcdfFile.addVariable("bi_start_num", DataType.INT, new Dimension[]{binIndexDim});
        final Variable maxVar = netcdfFile.addVariable("bi_max", DataType.INT, new Dimension[]{binIndexDim});

        final Variable binNumVar = netcdfFile.addVariable("bl_bin_num", DataType.INT, new Dimension[]{binListDim});
        final Variable numObsVar = netcdfFile.addVariable("bl_nobs", DataType.INT, new Dimension[]{binListDim});
        final Variable numScenesVar = netcdfFile.addVariable("bl_nscenes", DataType.INT, new Dimension[]{binListDim});
        final int aggregatorCount = binningContext.getBinManager().getAggregatorCount();
        final ArrayList<Variable> featureVars = new ArrayList<Variable>(3 * aggregatorCount);
        for (int i = 0; i < aggregatorCount; i++) {
            final Aggregator aggregator = binningContext.getBinManager().getAggregator(i);
            final String[] featureNames = aggregator.getTemporalFeatureNames();
            for (String featureName : featureNames) {
                final Variable featureVar = netcdfFile.addVariable("bl_" + featureName, DataType.FLOAT, new Dimension[]{binListDim});
                featureVar.addAttribute(new Attribute("_FillValue", aggregator.getOutputFillValue()));
                featureVars.add(featureVar);
            }
        }

        netcdfFile.create();
        writeBinIndexVariables(netcdfFile, rowNumVar, vsizeVar, hsizeVar, startNumVar, maxVar);
        writeBinListVariables(netcdfFile, binNumVar, numObsVar, numScenesVar, featureVars, temporalBins);
        netcdfFile.close();
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
                array.setInt(rowIndex, grid.getFirstBinIndex(rowIndex));
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
                array.setDouble(rowIndex, 360.0 / grid.getNumCols(rowIndex));
            }
        });
        writeBinIndexVariable(netcdfFile, maxVar, new BinIndexElementSetter() {
            @Override
            public void setArray(Array array, int rowIndex, SeadasGrid grid) {
                array.setInt(rowIndex, grid.getNumCols(rowIndex));
            }
        });
    }

    private void writeBinListVariables(final NetcdfFileWriteable netcdfFile,
                                       final Variable binNumVar,
                                       final Variable numObsVar,
                                       final Variable numScenesVar,
                                       final List<Variable> featureVars,
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


        final long[] startRowBinIds = new long[planetaryGrid.getNumRows()];
        final long[] endRowBinIds = new long[planetaryGrid.getNumRows()];
        writeBinListVariable0(netcdfFile, temporalBins, binListVars, startRowBinIds, endRowBinIds);

        for (int i = 0; i < planetaryGrid.getNumRows(); i++) {
            long startRowBinId = startRowBinIds[i];
            long endRowBinId = endRowBinIds[i];
            System.out.printf("row(%d) = %d...%d%n", i, startRowBinId, endRowBinId);
        }
    }

    private void writeBinIndexVariable(NetcdfFileWriteable netcdfFile,
                                       Variable variable,
                                       BinIndexElementSetter setter) throws IOException, InvalidRangeException {
        logger.info("Writing bin index variable " + variable.getName());
        final int numRows = seadasGrid.getNumRows();
        final Array array = Array.factory(variable.getDataType(), new int[]{numRows});
        for (int row = 0; row < numRows; row++) {
            setter.setArray(array, row, seadasGrid);
        }
        netcdfFile.write(variable.getName(), array);
    }

    private void writeBinListVariable0(NetcdfFileWriteable netcdfFile,
                                       List<TemporalBin> temporalBins,
                                       List<BinListVar> vars,
                                       long[] startRowBinIds,
                                       long[] endRowBinIds) throws IOException, InvalidRangeException {
        logger.info("Writing bin list variables");

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
                    bufferIndex = writeRowBins(netcdfFile, rowBins, vars, origin, bufferIndex, rowIndex, startRowBinIds, endRowBinIds);
                    rowBins.clear();
                }
                rowBins.add(temporalBin);
                lastRowIndex = rowIndex;
            }
        }
        if (!rowBins.isEmpty()) {
            bufferIndex = writeRowBins(netcdfFile, rowBins, vars, origin, bufferIndex, rowIndex, startRowBinIds, endRowBinIds);
        }
        if (bufferIndex > 0) {
            writeBinListVars(netcdfFile, vars, origin, bufferIndex);
        }
    }

    private int writeRowBins(NetcdfFileWriteable netcdfFile, ArrayList<TemporalBin> rowBins, List<BinListVar> vars, int[] origin, int bufferIndex, int rowIndex, long[] startRowBinIds, long[] endRowBinIds) throws IOException, InvalidRangeException {
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
        startRowBinIds[rowIndex] = rowBins.get(0).getIndex();
        endRowBinIds[rowIndex] = rowBins.get(rowBins.size() - 1).getIndex();
        return bufferIndex;
    }

    private void setBinListVarsArrayElement(List<BinListVar> vars, TemporalBin temporalBin, int bufferIndex) {
        for (BinListVar var : vars) {
            var.setter.setArray(var.buffer, bufferIndex, temporalBin);
        }
    }

    private void writeBinListVars(NetcdfFileWriteable netcdfFile, List<BinListVar> vars, int[] origin) throws IOException, InvalidRangeException {
        for (BinListVar var : vars) {
            netcdfFile.write(var.variable.getName(),
                             origin,
                             var.buffer);
        }
    }

    private void writeBinListVars(NetcdfFileWriteable netcdfFile, List<BinListVar> vars, int[] origin, int bufferIndex) throws IOException, InvalidRangeException {
        final int[] origin0 = {0};
        final int[] shape = {bufferIndex};
        for (BinListVar var : vars) {
            netcdfFile.write(var.variable.getName(),
                             origin,
                             var.buffer.section(origin0, shape));
        }
    }

    private interface BinIndexElementSetter {
        void setArray(Array array, int rowIndex, SeadasGrid grid);
    }

    private interface BinListElementSetter {
        void setArray(Array array, int binIndex, TemporalBin bin);
    }


    private static class BinListVar {
        private final Variable variable;
        private final BinListElementSetter setter;
        private final Array buffer;

        BinListVar(Variable variable, BinListElementSetter setter) {
            this.variable = variable;
            this.setter = setter;
            this.buffer = Array.factory(variable.getDataType(), new int[]{BUFFER_SIZE});
        }
    }
}
