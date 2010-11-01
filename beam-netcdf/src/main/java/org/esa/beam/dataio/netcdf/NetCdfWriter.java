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
import org.esa.beam.dataio.netcdf.metadata.ProfileImpl;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContextImpl;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamProfileSpi;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
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

public class NetCdfWriter extends AbstractProductWriter {

    private NetcdfFileWriteable writeable;
    private HashMap<String, Variable> variableMap;
    private boolean isYFlipped;

    public NetCdfWriter(ProductWriterPlugIn nc4BeamWriterPlugIn) {
        super(nc4BeamWriterPlugIn);
        variableMap = new HashMap<String, Variable>();
    }

    /**
     * Writes the in-memory representation of a data product. This method was called by <code>writeProductNodes(product,
     * output)</code> of the AbstractProductWriter.
     *
     * @throws IllegalArgumentException if <code>output</code> type is not one of the supported output sources.
     * @throws java.io.IOException      if an I/O error occurs
     */
    @Override
    protected void writeProductNodesImpl() throws IOException {
        writeable = NetcdfFileWriteable.createNew(getOutputString());
        writeable.setLargeFile(true);
        ProfileImpl profile = new ProfileImpl();
        new BeamProfileSpi().configureProfile(null, profile);
        ProfileWriteContext context = new ProfileWriteContextImpl(writeable);
        profile.writeProduct(context, getSourceProduct());
        final Object object = context.getProperty(Constants.Y_FLIPPED_PROPERTY_NAME);
        if (object instanceof Boolean) {
            isYFlipped = (Boolean) object;
        }
    }

    /**
     * Writes raster data from the given in-memory source buffer into the data sink specified by the given source band
     * and region.
     * <p/>
     * <h3>Source band</h3> The source band is used to identify the data sink in which this method transfers the sample
     * values given in the source buffer. The method does not modify the pixel data of the given source band at all.
     * <p/>
     * <h3>Source buffer</h3> The first element of the source buffer corresponds to the given <code>sourceOffsetX</code>
     * and <code>sourceOffsetY</code> of the source region. These parameters are an offset within the band's raster data
     * and <b>not</b> an offset within the source buffer.<br> The number of elements in the buffer must be exactly be
     * <code>sourceWidth * sourceHeight</code>. The pixel values to be written are considered to be stored in
     * line-by-line order, so the raster X co-ordinate varies faster than the Y.
     * <p/>
     * <h3>Source region</h3> The given destination region specified by the <code>sourceOffsetX</code>,
     * <code>sourceOffsetY</code>, <code>sourceWidth</code> and <code>sourceHeight</code> parameters is given in the
     * source band's raster co-ordinates. These co-ordinates are identical with the destination raster co-ordinates
     * since product writers do not support spectral or spatial subsets.
     *
     * @param sourceBand    the source band which identifies the data sink to which to define the sample values
     * @param sourceOffsetX the X-offset in the band's raster co-ordinates
     * @param sourceOffsetY the Y-offset in the band's raster co-ordinates
     * @param sourceWidth   the width of region to be written given in the band's raster co-ordinates
     * @param sourceHeight  the height of region to be written given in the band's raster co-ordinates
     * @param sourceBuffer  the source buffer which provides the sample values to be written
     * @param pm            a monitor to inform the user about progress
     *
     * @throws java.io.IOException      if an I/O error occurs
     * @throws IllegalArgumentException if the number of elements source buffer not equals <code>sourceWidth *
     *                                  sourceHeight</code> or the source region is out of the band's raster
     * @see org.esa.beam.framework.datamodel.Band#getRasterWidth()
     * @see org.esa.beam.framework.datamodel.Band#getRasterHeight()
     */
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
                throw new IOException("Unable to write netCDF data.", e);
            }
        }
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

    /**
     * Writes all data in memory to the data sink(s) associated with this writer.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
    }

    /**
     * Closes all output streams currently open. A concrete implementation should call <code>flush</code> before
     * performing the actual close-operation.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
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

    /**
     * Complete deletes the physical representation of the given product from the file system.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    public void deleteOutput() throws IOException {
        close();
        //noinspection ResultOfMethodCallIgnored
        getOutputFile().delete();
    }

    private File getOutputFile() {
        final Object output = getOutput();
        if (output instanceof String) {
            return new File((String) output);
        } else if (output instanceof File) {
            return (File) output;
        } else {
            throw new IllegalArgumentException("unsupported input source: " + output);
        }
    }

    private String getOutputString() {
        final Object output = getOutput();
        if (output instanceof String) {
            return (String) output;
        } else if (output instanceof File) {
            return output.toString();
        } else {
            throw new IllegalArgumentException("unsupported input source: " + output);
        }
    }
}
