/*
 * $Id$
 *
 * Copyright (C) 2010 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.netcdf4;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf4.convention.ProfileImpl;
import org.esa.beam.dataio.netcdf4.convention.beam.DefaultProfileSpi;
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

public class Nc4Writer extends AbstractProductWriter {

    private NetcdfFileWriteable writeable;
    private HashMap<String, Variable> variableMap;
    private ProfileImpl profile;

    public Nc4Writer(ProductWriterPlugIn nc4BeamWriterPlugIn) {
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
        ProfileImpl profile1 = new ProfileImpl();
        new DefaultProfileSpi().configureProfile(null, profile1);
        profile = profile1;
        profile.writeProduct(writeable, getSourceProduct());
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
     * <code>sourceWidth * sourceHeight</code>. The pixel values to be writte are considered to be stored in
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
        final String variableName = sourceBand.getName();
        final DataType dataType = getDataType(variableName);
        final int sceneHeight = sourceBand.getProduct().getSceneRasterHeight();
        final int[] origin = new int[2];
        origin[xIndex] = sourceOffsetX;
        origin[yIndex] = profile.isYFlipped() ? (sceneHeight - 1) - sourceOffsetY : sourceOffsetY;
        final int[] shape = new int[]{sourceHeight, sourceWidth};
        final Array dataArray = Array.factory(dataType, shape, sourceBuffer.getElems());
        try {
            writeable.write(variableName, origin, dataArray);
        } catch (InvalidRangeException ignored) {
            //nothing to do
        }
    }

    private DataType getDataType(String variableName) throws ProductIOException {
        if (!variableMap.containsKey(variableName)) {
            final Variable variable = writeable.findVariable(variableName);
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
