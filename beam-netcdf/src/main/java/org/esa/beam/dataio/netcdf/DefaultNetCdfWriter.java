/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Default NetCDF writer configured by an implementation of {@link AbstractNetCdfWriterPlugIn}.
 */
class DefaultNetCdfWriter extends AbstractProductWriter {

    private HashMap<String, Variable> variableMap;
    private NetcdfFileWriteable writeable;
    private boolean isYFlipped;

    DefaultNetCdfWriter(AbstractNetCdfWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
        variableMap = new HashMap<String, Variable>();
    }

    @Override
    public AbstractNetCdfWriterPlugIn getWriterPlugIn() {
        return (AbstractNetCdfWriterPlugIn) super.getWriterPlugIn();
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {
        writeable = NetcdfFileWriteable.createNew(getOutputString());
        writeable.setLargeFile(true);
        NetCdfWriteProfile profile = new NetCdfWriteProfile();
        AbstractNetCdfWriterPlugIn plugIn = getWriterPlugIn();
        configureProfile(profile, plugIn);
        ProfileWriteContext context = new ProfileWriteContextImpl(writeable);
        profile.writeProduct(context, getSourceProduct());
        final Object object = context.getProperty(Constants.Y_FLIPPED_PROPERTY_NAME);
        if (object instanceof Boolean) {
            isYFlipped = (Boolean) object;
        }

    }

    public void configureProfile(NetCdfWriteProfile profile, AbstractNetCdfWriterPlugIn plugIn) throws IOException {
        profile.setInitialisationPartWriter(plugIn.createInitialisationPartWriter());
        profile.addProfilePartWriter(plugIn.createMetadataPartWriter());
        profile.addProfilePartWriter(plugIn.createBandPartWriter());
        profile.addProfilePartWriter(plugIn.createTiePointGridPartWriter());
        profile.addProfilePartWriter(plugIn.createFlagCodingPartWriter());
        profile.addProfilePartWriter(plugIn.createGeoCodingPartWriter());
        profile.addProfilePartWriter(plugIn.createImageInfoPartWriter());
        profile.addProfilePartWriter(plugIn.createIndexCodingPartWriter());
        profile.addProfilePartWriter(plugIn.createMaskPartWriter());
        profile.addProfilePartWriter(plugIn.createStxPartWriter());
        profile.addProfilePartWriter(plugIn.createTimePartWriter());
        profile.addProfilePartWriter(plugIn.createDescriptionPartWriter());
    }

    @Override
    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                    int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm) throws IOException {
        final int yIndex = 0;
        final int xIndex = 1;
        final String variableName = ReaderUtils.getVariableName(sourceBand);
        final DataType dataType = getDataType(variableName);
        final int sceneHeight = sourceBand.getProduct().getSceneRasterHeight();
        final int[] writeOrigin = new int[2];
        writeOrigin[xIndex] = sourceOffsetX;

        final int[] sourceShape = new int[]{sourceHeight, sourceWidth};
        final Array sourceArray = Array.factory(dataType, sourceShape, sourceBuffer.getElems());

        final int[] sourceOrigin = new int[2];
        sourceOrigin[xIndex] = 0;
        final int[] writeShape = new int[]{1, sourceWidth};
        for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
            writeOrigin[yIndex] = isYFlipped ? (sceneHeight - 1) - y : y;
            sourceOrigin[yIndex] = y - sourceOffsetY;
            Array dataArrayLine;
            try {
                dataArrayLine = sourceArray.sectionNoReduce(sourceOrigin, writeShape, null);
                writeable.write(variableName, writeOrigin, dataArrayLine);
            } catch (InvalidRangeException e) {
                e.printStackTrace();
                throw new IOException("Unable to encode netCDF data.", e);
            }
        }
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
        if (writeable != null) {
            writeable.close();
            writeable = null;
        }
        if (variableMap != null) {
            variableMap.clear();
            variableMap = null;
        }
    }

    @Override
    public void deleteOutput() throws IOException {
        close();
        //noinspection ResultOfMethodCallIgnored
        getOutputFile().delete();
    }

    private DataType getDataType(String variableName) throws ProductIOException {
        if (!variableMap.containsKey(variableName)) {
            final Variable variable = writeable.getRootGroup().findVariable(variableName);
            if (variable == null) {
                throw new ProductIOException("Nc raster data variable '" + variableName + "' not found");
            }
            variableMap.put(variableName, variable);
        }
        return variableMap.get(variableName).getDataType();
    }

    private File getOutputFile() {
        return new File(getOutputString());
    }

    private String getOutputString() {
        return String.valueOf(getOutput());
    }

}
