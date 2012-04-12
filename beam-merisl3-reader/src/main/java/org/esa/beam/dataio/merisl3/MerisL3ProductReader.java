/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.merisl3;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The <code>MerisL3ProductReader</code> class is an implementation of the <code>ProductReader</code> interface
 * exclusively for data products having the standard MERIS Binned Level-3 format.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.beam.dataio.merisl3.MerisL3ProductReaderPlugIn
 */
public class MerisL3ProductReader extends AbstractProductReader {

    public static final String COL_INDEX_BAND_NAME = "col_index";

    private NetcdfFile _netcdfFile;
    private Product _product;
    private ISINGrid _grid;
    private int _sceneRasterWidth;
    private int _sceneRasterHeight;
    private RowInfo[] _rowInfos;
    private Map<Band, VariableMetadata> bandMap;

    /**
     * Constructs a new MERIS Binned Level-3 product reader.
     *
     * @param readerPlugIn the plug-in which created this reader instance
     */
    public MerisL3ProductReader(MerisL3ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    /**
     * Reads a data product and returns an in-memory representation of it. This method is called by
     * <code>readProductNodes(input, subsetInfo)</code> of the abstract superclass.
     *
     * @throws java.lang.IllegalArgumentException
     *                             if <code>input</code> type is not one of the supported input sources.
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        String path = getInput().toString();
        _netcdfFile = NetcdfFile.open(path);
        bandMap = new HashMap<Band, VariableMetadata>(10);
        try {
            _grid = new ISINGrid(ISINGrid.detectRowCount(path));
            _sceneRasterWidth = _grid.getRowCount() * 2;
            _sceneRasterHeight = _grid.getRowCount();
            File productFile = new File(path);
            _product = new Product(FileUtils.getFilenameWithoutExtension(productFile),
                                   "L3_ENV_MER",
                                   _sceneRasterWidth,
                                   _sceneRasterHeight,
                                   this);

            MetadataUtils.readNetcdfMetadata(_netcdfFile, _product.getMetadataRoot());

            // todo - traverse all variables and check if they can be converted into bands
            addBand("mean");
            addBand("stdev");
            addBand("min");
            addBand("max");
            addBand("count");
            _product.addBand(createColumnIndexBand());
            _product.setQuicklookBandName("mean");
            _product.setFileLocation(productFile);

            if (_product.getNumBands() == 0) {
                throw new IOException("No bands found.");
            }

            initGeoCoding();

        } catch (IOException e) {
            dispose();
            throw e;
        }

        return _product;
    }

    /**
     * The template method which is called by the {@link org.esa.beam.framework.dataio.AbstractProductReader#readBandRasterDataImpl(int, int, int, int, int, int, org.esa.beam.framework.datamodel.Band, int, int, int, int, org.esa.beam.framework.datamodel.ProductData, com.bc.ceres.core.ProgressMonitor)} }
     * method after an optional spatial subset has been applied to the input parameters.
     * <p/>
     * <p>The destination band, buffer and region parameters are exactly the ones passed to the original {@link
     * org.esa.beam.framework.dataio.AbstractProductReader#readBandRasterDataImpl} call. Since the
     * <code>destOffsetX</code> and <code>destOffsetY</code> parameters are already taken into acount in the
     * <code>sourceOffsetX</code> and <code>sourceOffsetY</code> parameters, an implementor of this method is free to
     * ignore them.
     *
     * @param sourceOffsetX the absolute X-offset in source raster co-ordinates
     * @param sourceOffsetY the absolute Y-offset in source raster co-ordinates
     * @param sourceWidth   the width of region providing samples to be decode given in source raster co-ordinates
     * @param sourceHeight  the height of region providing samples to be decode given in source raster co-ordinates
     * @param sourceStepX   the sub-sampling in X direction within the region providing samples to be decode
     * @param sourceStepY   the sub-sampling in Y direction within the region providing samples to be decode
     * @param destBand      the destination band which identifies the data source from which to decode the sample values
     * @param destBuffer    the destination buffer which receives the sample values to be decode
     * @param destOffsetX   the X-offset in the band's raster co-ordinates
     * @param destOffsetY   the Y-offset in the band's raster co-ordinates
     * @param destWidth     the width of region to be decode given in the band's raster co-ordinates
     * @param destHeight    the height of region to be decode given in the band's raster co-ordinates
     * @param pm            a monitor to inform the user about progress
     * @throws java.io.IOException if  an I/O error occurs
     * @see #getSubsetDef
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        if (sourceStepX != 1 || sourceStepY != 1) {
            throw new IOException("Sub-sampling is not supported by this product reader.");
        }

        if (sourceWidth != destWidth || sourceHeight != destHeight) {
            throw new IllegalStateException("sourceWidth != destWidth || sourceHeight != destHeight");
        }

        final Variable idxVariable = _netcdfFile.getRootGroup().findVariable("idx");
        short[] rasterData = (short[]) destBuffer.getElems();

        if (_rowInfos == null) {
            _rowInfos = createRowInfos();
        }
        VariableMetadata variableMetadata = bandMap.get(destBand);
        boolean readColIndex = variableMetadata == null; // this band has no variables associated

        final int height = _sceneRasterHeight;
        final int width = _sceneRasterWidth;
        final ISINGrid grid = _grid;

        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceHeight);
        try {

            if (readColIndex) {
                for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final int rowIndex = (height - 1) - y;
                    for (int x = sourceOffsetX; x < sourceOffsetX + sourceWidth; x++) {
                        final double lon = x * 360.0 / width;
                        final int colIndex = grid.getColIndex(rowIndex, lon);
                        final int rasterIndex = sourceWidth * (y - sourceOffsetY) + (x - sourceOffsetX);
                        rasterData[rasterIndex] = (short) colIndex;
                    }
                    pm.worked(1);
                }
            } else {
                final Variable binVariable = variableMetadata.variable;
                final Number fillValueN = getAttributeNumericValue(binVariable, "_FillValue");
                final short fillValue = fillValueN != null ? fillValueN.shortValue() : 0;

                int[] lineOffsets = new int[1];
                int[] lineLengths = new int[1];

                Arrays.fill(rasterData, fillValue);

                for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final int rowIndex = (height - 1) - y;
                    final RowInfo rowInfo = _rowInfos[rowIndex];
                    if (rowInfo != null) {

                        final int lineOffset = rowInfo.offset;
                        final int lineLength = rowInfo.length;

                        lineOffsets[0] = lineOffset;
                        lineLengths[0] = lineLength;
                        final int[] idxValues;
                        final short[] binValues;
                        try {
                            synchronized (_netcdfFile) {
                                idxValues = (int[]) idxVariable.read(lineOffsets, lineLengths).getStorage();
                                binValues = (short[]) binVariable.read(lineOffsets, lineLengths).getStorage();
                            }
                        } catch (InvalidRangeException e) {
                            // ?!?!?!?
                            throw new IOException("Format problem.");
                        }

                        int lineIndex0 = 0;
                        for (int x = sourceOffsetX; x < sourceOffsetX + sourceWidth; x++) {
                            final double lon = x * 360.0 / width;
                            final int binIndex = grid.getBinIndex(rowIndex, lon);
                            int lineIndex = -1;
                            for (int i = lineIndex0; i < lineLength; i++) {
                                if (idxValues[i] >= binIndex) {
                                    if (idxValues[i] == binIndex) {
                                        lineIndex = i;
                                    }
                                    lineIndex0 = i;
                                    break;
                                }
                            }
                            if (lineIndex >= 0) {
                                final int rasterIndex = sourceWidth * (y - sourceOffsetY) + (x - sourceOffsetX);
                                rasterData[rasterIndex] = binValues[lineIndex];
                            }
                        }
                        pm.worked(1);
                    }
                }
            }
        } finally {
            pm.done();
        }

    }

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws
            IOException {
        super.close();

        if (_netcdfFile != null) {
            _netcdfFile.close();
            _netcdfFile = null;
        }
        bandMap.clear();
        _product = null;
        _grid = null;
        _rowInfos = null;
    }

    /////////////////////////////////////////////////////////////////////////
    // private helpers
    /////////////////////////////////////////////////////////////////////////

    private Band createColumnIndexBand() {
        Band colIndexBand = new Band(COL_INDEX_BAND_NAME, ProductData.TYPE_UINT16, _sceneRasterWidth,
                                     _sceneRasterHeight);
        colIndexBand.setDescription("Zero-based column index in the global ISIN grid");
        return colIndexBand;
    }

    private void initGeoCoding() throws IOException {
        float pixelX = 0.0f;
        float pixelY = 0.0f;
        float easting = -180f;
        float northing = +90f;
        float pixelSizeX = 360.0f / _sceneRasterWidth;
        float pixelSizeY = 180.0f / _sceneRasterHeight;
        try {
            _product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                                   _sceneRasterWidth, _sceneRasterHeight,
                                                   easting, northing,
                                                   pixelSizeX, pixelSizeY,
                                                   pixelX, pixelY));
        } catch (FactoryException e) {
            throw new IOException(e);
        } catch (TransformException e) {
            throw new IOException(e);
        }
    }

    private void dispose() {
        try {
            close();
        }
        catch (IOException e) {
            // OK
        }
    }

    private RowInfo[] createRowInfos() throws
            IOException {
        final ISINGrid grid = _grid;
        final RowInfo[] binLines = new RowInfo[_sceneRasterHeight];
        final Variable idxVariable = _netcdfFile.getRootGroup().findVariable("idx");
        final int[] idxValues;
        synchronized (_netcdfFile) {
            idxValues = (int[]) idxVariable.read().getStorage();
        }
        final Point gridPoint = new Point();
        int lastBinIndex = -1;
        int lastRowIndex = -1;
        int lineOffset = 0;
        int lineLength = 0;
        for (int i = 0; i < idxValues.length; i++) {

            final int binIndex = idxValues[i];
            if (binIndex < lastBinIndex) {
                throw new IOException(
                        "Unrecognized level-3 format. Bins numbers expected to appear in ascending order.");
            }
            lastBinIndex = binIndex;

            grid.getGridPoint(binIndex, gridPoint);
            final int rowIndex = gridPoint.y;

            if (rowIndex != lastRowIndex) {
                if (lineLength > 0) {
                    binLines[lastRowIndex] = new RowInfo(lineOffset, lineLength);
                }
                lineOffset = i;
                lineLength = 0;
            }

            lineLength++;
            lastRowIndex = rowIndex;
        }

        if (lineLength > 0) {
            binLines[lastRowIndex] = new RowInfo(lineOffset, lineLength);
        }

        return binLines;
    }

    private void addBand(String varName) {
        VariableMetadata variableMetadata = getVariableMetadata(varName);
        if (variableMetadata != null) {
            Band band = new Band(variableMetadata.name, ProductData.TYPE_INT16, _sceneRasterWidth, _sceneRasterHeight);
            band.setDescription(variableMetadata.description);
            band.setScalingOffset(variableMetadata.scalingOffset);
            band.setScalingFactor(variableMetadata.scalingFactor);
            band.setLog10Scaled(variableMetadata.log10Scaled);
            band.setNoDataValue(variableMetadata.fillValue);
            band.setNoDataValueUsed(variableMetadata.fillValue != Double.NaN);
            _product.addBand(band);
            bandMap.put(band, variableMetadata);
        }
    }

    private VariableMetadata getVariableMetadata(String varName) {
        final Variable variable = _netcdfFile.getRootGroup().findVariable(varName);
        if (variable == null) {
            return null;
        }

        Number numericValue;
        String stringValue;

        stringValue = getAttributeStringValue(variable, "long_name");
        final String description = stringValue;

        numericValue = getAttributeNumericValue(variable, "_FillValue");
        final double fillValue = numericValue != null ? numericValue.doubleValue() : Double.NaN;

        numericValue = getAttributeNumericValue(variable, "scale_factor");
        final double scale = numericValue != null ? numericValue.doubleValue() : 1.0;

        numericValue = getAttributeNumericValue(variable, "add_offset");
        final double offset = numericValue != null ? numericValue.doubleValue() : 0.0;

        stringValue = getAttributeStringValue(variable, "scaling_equation");
        final boolean logScaled = (stringValue != null) && stringValue.equals("value=10**(offset+code*gain)");

        return new VariableMetadata(variable, varName, description, fillValue, scale, offset, logScaled);
    }

    private static Number getAttributeNumericValue(Variable variable, String attributeName) {
        final Attribute att = variable.findAttribute(attributeName);
        return att != null ? att.getNumericValue() : null;
    }

    private static String getAttributeStringValue(Variable variable, String attributeName) {
        final Attribute att = variable.findAttribute(attributeName);
        return att != null ? att.getStringValue() : null;
    }

    private static class VariableMetadata {
        final Variable variable;
        final String name;
        final String description;
        final double fillValue;
        final double scalingFactor;
        final double scalingOffset;
        final boolean log10Scaled;

        public VariableMetadata(Variable variable, String name, String description, double fillValue, double scale, double offset,
                                boolean logScaled) {
            this.variable = variable;
            this.name = name;
            this.description = description;
            this.fillValue = fillValue;
            this.scalingFactor = scale;
            this.scalingOffset = offset;
            this.log10Scaled = logScaled;
        }
    }

    private static final class RowInfo {

        // offset of row within file
        final int offset;
        // number of bins per row
        final int length;

        public RowInfo(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }
    }
}
