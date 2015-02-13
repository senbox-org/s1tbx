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

package org.esa.beam.binning.operator;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.binning.support.SeadasGrid;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility class that writes SeaDAS-Level-3-alike NetCDF files containing binned Level-3 data.
 *
 * @author Norman Fomferra
 */
public abstract class AbstractBinWriter implements BinWriter {

    private static final DateFormat dateFormat = ProductData.UTC.createDateFormat(BinningOp.DATETIME_OUTPUT_PATTERN);
    private final static int BUFFER_SIZE = 4096;

    private final Geometry region;
    private final ProductData.UTC startTime;
    private final ProductData.UTC stopTime;
    private File targetFilePath;
    private Logger logger;

    public AbstractBinWriter(Geometry region, ProductData.UTC startTime, ProductData.UTC stopTime) {
        this.region = region;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.logger = BeamLogManager.getSystemLogger();
    }

    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    Logger getLogger() {
        return logger;
    }

    @Override
    public void setTargetFileTemplatePath(String targetFileTemplatePath) {
        targetFilePath = FileUtils.exchangeExtension(new File(targetFileTemplatePath), "-bins.nc");
    }

    @Override
    public String getTargetFilePath() {
        return targetFilePath.getAbsolutePath();
    }

    Geometry getRegion() {
        return region;
    }

    protected void writeGlobalSEAGridMetadata(NetcdfFileWriteable netcdfFile, int numRows) {
        netcdfFile.addGlobalAttribute("SEAGrid_bins", 2 * numRows);
        netcdfFile.addGlobalAttribute("SEAGrid_radius", SEAGrid.RE);
        netcdfFile.addGlobalAttribute("SEAGrid_max_north", +90.0);
        netcdfFile.addGlobalAttribute("SEAGrid_max_south", -90.0);
        netcdfFile.addGlobalAttribute("SEAGrid_seam_lon", -180.0);
    }

    protected void writeGlobalCommonMetadata(Map<String, String> metadataProperties, NetcdfFileWriteable netcdfFile) {
        for (String name : metadataProperties.keySet()) {
            final String value = metadataProperties.get(name);
            try {
                netcdfFile.addGlobalAttribute(name, value);
            } catch (Exception e) {
                logger.warning(String.format("Failed to write metadata property to '%s': %s = %s", targetFilePath.getAbsolutePath(), name, value));
            }
        }
    }

    static String toDateString(ProductData.UTC utc) {
        return utc != null ? dateFormat.format(utc.getAsDate()) : "";
    }

    protected void writeGlobalTimeCoverageMetadata(NetcdfFileWriteable netcdfFile) {
        final String startTimeString = toDateString(startTime);
        final String stopTimeString = toDateString(stopTime);
        netcdfFile.addGlobalAttribute("start_time", startTimeString);
        netcdfFile.addGlobalAttribute("stop_time", stopTimeString);
        netcdfFile.addGlobalAttribute("time_coverage_start", startTimeString);
        netcdfFile.addGlobalAttribute("time_coverage_end", stopTimeString);
    }

    protected static void setBinListVarsArrayElement(List<BinListVar> vars, TemporalBin temporalBin, int bufferIndex) {
        for (BinListVar var : vars) {
            var.setter.setArray(var.buffer, bufferIndex, temporalBin);
        }
    }

    protected static void writeBinListVars(NetcdfFileWriteable netcdfFile, List<BinListVar> vars, int[] origin) throws IOException, InvalidRangeException {
        for (BinListVar var : vars) {
            netcdfFile.write(var.variable.getFullName(),
                             origin,
                             var.buffer);
        }
    }

    protected static void writeBinListVars(NetcdfFileWriteable netcdfFile, List<BinListVar> vars, int[] origin, int bufferIndex) throws IOException,
            InvalidRangeException {
        final int[] origin0 = {0};
        final int[] shape = {bufferIndex};
        for (BinListVar var : vars) {
            String fullName = var.variable.getFullName();
            Array valuesToWrite = var.buffer.sectionNoReduce(origin0, shape, null);
            netcdfFile.write(fullName, origin, valuesToWrite);
        }
    }

    protected interface BinIndexElementSetter {

        void setArray(Array array, int rowIndex, SeadasGrid grid);
    }

    protected interface BinListElementSetter {

        void setArray(Array array, int binIndex, TemporalBin bin);
    }

    protected static class BinListVar {

        private final Variable variable;
        private final BinListElementSetter setter;
        private final Array buffer;

        protected BinListVar(Variable variable, BinListElementSetter setter) {
            this.variable = variable;
            this.setter = setter;
            this.buffer = Array.factory(variable.getDataType(), new int[]{BUFFER_SIZE});
        }
    }
}
