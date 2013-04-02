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
import java.util.*;
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
                      Map<String, String> metadataProperties,
                      List<TemporalBin> temporalBins) throws IOException, InvalidRangeException {

        final NetcdfFileWriteable netcdfFile = NetcdfFileWriteable.createNew(filePath.getPath());

        netcdfFile.addGlobalAttribute("title", "Level-3 Binned Data");
        netcdfFile.addGlobalAttribute("super_sampling", binningContext.getSuperSampling());
        if (region != null) {
            netcdfFile.addGlobalAttribute("region", region.toText());
        }
        DateFormat dateFormat = ProductData.UTC.createDateFormat(BinningOp.DATETIME_PATTERN);
        netcdfFile.addGlobalAttribute("start_time", startTime != null ? dateFormat.format(startTime.getAsDate()) : "");
        netcdfFile.addGlobalAttribute("stop_time", stopTime != null ? dateFormat.format(stopTime.getAsDate()) : "");

        for (String name : metadataProperties.keySet()) {
            final String value = metadataProperties.get(name);
            try {
                netcdfFile.addGlobalAttribute(name, value);
            } catch (Exception e) {
                logger.warning(String.format("Failed to write metadata property to '%s': %s = %s", filePath, name, value));
            }
        }

        netcdfFile.addGlobalAttribute("SEAGrid_bins", 2 * planetaryGrid.getNumRows());
        netcdfFile.addGlobalAttribute("SEAGrid_radius", SEAGrid.RE);
        netcdfFile.addGlobalAttribute("SEAGrid_max_north", +90.0);
        netcdfFile.addGlobalAttribute("SEAGrid_max_south", -90.0);
        netcdfFile.addGlobalAttribute("SEAGrid_seam_lon", -180.0);

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
        writeBinListVariables(netcdfFile, binNumVar, numObsVar, numScenesVar, featureVars, beginOffsetVar, beginVar, extendVar, temporalBins);
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
                                       int[] binRowBeginOffsets,
                                       long[] binRowBegins,
                                       int[] binRowExtends) throws IOException, InvalidRangeException {
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
                    bufferIndex = writeRowBins(netcdfFile, rowBins, vars, origin, bufferIndex, rowIndex,
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
