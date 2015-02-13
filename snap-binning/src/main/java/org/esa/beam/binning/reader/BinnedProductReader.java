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

package org.esa.beam.binning.reader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BinnedProductReader extends AbstractProductReader {

    private NetcdfFile netcdfFile;
    private AbstractGridAccessor gridAccessor;
    private Product product;
    private SEAGrid planetaryGrid;
    private int sceneRasterWidth;
    private int sceneRasterHeight;
    private Map<Band, VariableReader> bandMap;
    private double pixelSizeX;

    /**
     * Constructs a new Binned Level-3 product reader.
     *
     * @param readerPlugIn the plug-in which created this reader instance
     */
    public BinnedProductReader(BinnedProductReaderPlugin readerPlugIn) {
        super(readerPlugIn);
    }

    /**
     * Reads a data product and returns an in-memory representation of it. This method is called by
     * <code>readProductNodes(input, subsetInfo)</code> of the abstract superclass.
     *
     * @throws IllegalArgumentException
     *                             if <code>input</code> type is not one of the supported input sources.
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final String path = getInput().toString();
        netcdfFile = NetcdfFileOpener.open(path);
        if (netcdfFile == null) {
            throw new IOException("Could not open NetCDF file " +  path);
        }

        if (isSparseGridded(netcdfFile)) {
            gridAccessor = new SparseGridAccessor(netcdfFile);
        } else {
            gridAccessor = new FullGridAccessor(netcdfFile);
        }

        bandMap = new HashMap<Band, VariableReader>();
        try {
            initProductWidthAndHeight();
            initProduct();
            initGeoCoding();
            readMetadata();
            initBands();
            initPlanetaryGrid();

            gridAccessor.setPlanetaryGrid(planetaryGrid);
            gridAccessor.setPixelSizeX(pixelSizeX);
        } catch (IOException e) {
            dispose();
            throw e;
        }
        return product;
    }

    private static boolean isSparseGridded(NetcdfFile netcdfFile) {
        final Variable bl_bin_num = netcdfFile.findVariable("bl_bin_num");
        final Variable bi_begin = netcdfFile.findVariable("bi_begin");
        final Variable bi_extent = netcdfFile.findVariable("bi_extent");

        return bl_bin_num != null && bi_begin != null && bi_extent != null;
    }

    private void initBands() throws IOException {
        int largestDimensionSize = getLargestDimensionSize();
        //read geophysical band values
        for (Variable variable : netcdfFile.getVariables()) {
            final List<Dimension> variableDimensions = variable.getDimensions();
            int numDimensions = variableDimensions.size();
            if (numDimensions == 0) {
                continue;
            } else if (numDimensions == 1) {
                if (variableDimensions.get(0).getLength() == largestDimensionSize) {
                    addBand(variable.getFullName());
                }
            } else {
                // can handle:
                // - dimension length = 1: 0 to many
                // - dimension length = largestDimensionSize: exact 1
                // - auxiliary dimensions: 1 (currently)
                int binDimIndex = -1;
                int auxDimIndex = -1;
                for (int i = 0; i < numDimensions; i++) {
                    if (variableDimensions.get(i).getLength() == largestDimensionSize) {
                        if (binDimIndex != -1) {
                            throw new IllegalArgumentException("2 Dimensions have num bins. Unsupported.");
                        }
                        binDimIndex = i;
                    } else if (variableDimensions.get(i).getLength() > 1) {
                        if (auxDimIndex != -1) {
                            throw new IllegalArgumentException("2 Auxiliary dimension. Unsupported.");
                        }
                        auxDimIndex = i;
                    }
                }
                if (binDimIndex != -1) {
                    int[] origin = new int[numDimensions];
                    Arrays.fill(origin, 0);
                    int[] shape = new int[numDimensions];
                    Arrays.fill(shape, 1);
                    shape[binDimIndex] = largestDimensionSize;
                    if (auxDimIndex != -1) {
                        for (int i = 0; i < variableDimensions.get(auxDimIndex).getLength(); i++) {
                            String suffix = "_" + i;
                            int[] auxOrigin = origin.clone();
                            auxOrigin[auxDimIndex] = i;
                            int[] auxShape = shape.clone();
                            auxShape[auxDimIndex] = 1;
                            addBand(variable.getFullName(), suffix, auxOrigin, auxShape, binDimIndex);
                        }
                    } else {
                        addBand(variable.getFullName(), "", origin, shape, binDimIndex);
                    }
                }
            }
        }
        if (product.getNumBands() == 0) {
            throw new IOException("No bands found.");
        }
    }

    private void readMetadata() {
        MetadataUtils.readNetcdfMetadata(netcdfFile, product.getMetadataRoot(), sceneRasterHeight);
    }

    private void initProduct() {
        final File productFile = new File(getInput().toString());
        final String productName = FileUtils.getFilenameWithoutExtension(productFile);

        final String productType = netcdfFile.findGlobalAttribute("title").getStringValue();
        product = new Product(productName, productType, sceneRasterWidth, sceneRasterHeight, this);
        product.setFileLocation(productFile);
        product.setAutoGrouping("adg:aph:atot:bbp:bl_Rrs:chlor_a:Rrs:water");
        product.setStartTime(extractStartTime(netcdfFile));
        product.setEndTime(extractEndTime(netcdfFile));
        product.setPreferredTileSize(sceneRasterWidth, 64);
    }

    private void initPlanetaryGrid() {
        planetaryGrid = new SEAGrid(sceneRasterHeight);
    }

    private int getLargestDimensionSize() {
        int largestDimensionSize = 0;
        for (Dimension dimension : netcdfFile.getDimensions()) {
            if (dimension.getLength() > largestDimensionSize) {
                largestDimensionSize = dimension.getLength();
            }
        }
        return largestDimensionSize;
    }

    private void initProductWidthAndHeight() {
        sceneRasterHeight = 0;

        for (Variable variable : netcdfFile.getVariables()) {
            Attribute gridMappingAttr = variable.findAttribute("grid_mapping_name");
            if (gridMappingAttr != null) {
                if ("1D binned sinusoidal".equalsIgnoreCase(gridMappingAttr.getStringValue())) {
                    Attribute numberOfLatitudeRows = variable.findAttribute("number_of_latitude_rows");
                    sceneRasterHeight = numberOfLatitudeRows.getNumericValue().intValue();
                    break;
                }
            }
        }
        if (sceneRasterHeight == 0)  {
            final Dimension bin_index = netcdfFile.findDimension("bin_index");
            if (bin_index != null) {
                sceneRasterHeight = bin_index.getLength();
            }
        }
        if (sceneRasterHeight == 0)  {
            sceneRasterHeight = 2160;
        }

        sceneRasterWidth = 2 * sceneRasterHeight;
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
                                          int sourceStepX, int sourceStepY, final Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        if (isSubSampled(sourceStepX, sourceStepY)) {
            throw new IOException("Sub-sampling is not supported by this product reader.");
        }
        if (sourceWidth != destWidth || sourceHeight != destHeight) {
            throw new IllegalStateException("sourceWidth != destWidth || sourceHeight != destHeight");
        }

        final VariableReader variableReader = bandMap.get(destBand);

        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceHeight);
        try {
            float fillValue = getFillValue(variableReader.getBinVariable());

            if (destBuffer.getType() == ProductData.TYPE_FLOAT32) {
                float[] destRasterData = (float[]) destBuffer.getElems();
                Arrays.fill(destRasterData, fillValue);
            } else if (destBuffer.getType() == ProductData.TYPE_INT32) {
                int[] destRasterData = (int[]) destBuffer.getElems();
                Arrays.fill(destRasterData, (int) fillValue);
            } else {
                throw new IOException("Format problem. Band datatype should be float32 or int32.");
            }

            for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
                int lineIndex = sceneRasterHeight - y - 1;

                final int startBinIndex = gridAccessor.getStartBinIndex(sourceOffsetX, lineIndex);
                final int endBinIndex = gridAccessor.getEndBinIndex(sourceOffsetX, sourceWidth, lineIndex);

                final Array lineValues = gridAccessor.getLineValues(destBand, variableReader, lineIndex);

                for (int i = startBinIndex; i < endBinIndex; i++) {
                    final float value = lineValues.getFloat(i);
                    if (value != fillValue) {
                        int binIndexInGrid = gridAccessor.getBinIndexInGrid(i, lineIndex);

                        final int[] xValuesForBin = getXValuesForBin(binIndexInGrid, lineIndex);
                        final int destStart = Math.max(xValuesForBin[0], sourceOffsetX);
                        final int destEnd = Math.min(xValuesForBin[1], sourceOffsetX + sourceWidth);

                        for (int x = destStart; x < destEnd; x++) {
                            final int destRasterIndex = sourceWidth * (y - sourceOffsetY) + (x - sourceOffsetX);
                            destBuffer.setElemFloatAt(destRasterIndex, value);
                        }
                    }
                }
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    private float getFillValue(Variable binVariable) {
        final Number fillValueN = getAttributeNumericValue(binVariable, "_FillValue");
        return fillValueN != null ? fillValueN.floatValue() : 0;
    }

    // package access for testing only tb 2013-08-12
    static boolean isSubSampled(int sourceStepX, int sourceStepY) {
        return sourceStepX != 1 || sourceStepY != 1;
    }

    private int[] getXValuesForBin(int binIndexInGrid, int row) {
        int numberOfBinsInRow = planetaryGrid.getNumCols(row);
        int firstBinIndex = (int) planetaryGrid.getFirstBinIndex(row);
        if (firstBinIndex > binIndexInGrid) {  // TODO check for sparse grid: >= or > ???
            numberOfBinsInRow = planetaryGrid.getNumCols(row - 1);
            firstBinIndex = (int) planetaryGrid.getFirstBinIndex(row - 1) + 1;
        }
        double binIndexInRow = binIndexInGrid - firstBinIndex;
        final double longitudeExtent = 360.0 / numberOfBinsInRow;
        final double smallestLongitude = binIndexInRow * longitudeExtent;
        final double largestLongitude = smallestLongitude + longitudeExtent;
        final int startX = (int) Math.floor(smallestLongitude / pixelSizeX);
        final int endX = (int) Math.ceil(largestLongitude / pixelSizeX);
        return new int[]{startX, endX};
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
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    public void close() throws
            IOException {
        super.close();

        if (gridAccessor != null) {
            gridAccessor.dispose();
            gridAccessor = null;
        }

        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
        bandMap.clear();
        product = null;
        planetaryGrid = null;
    }

    /////////////////////////////////////////////////////////////////////////
    // private helpers
    /////////////////////////////////////////////////////////////////////////

    private void initGeoCoding() throws IOException {
        float pixelX = 0.0f;
        float pixelY = 0.0f;
        float easting = -180f;
        float northing = +90f;
        pixelSizeX = 360.0 / sceneRasterWidth;
        double pixelSizeY = 180.0 / sceneRasterHeight;
        try {
            product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                    sceneRasterWidth, sceneRasterHeight,
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
        } catch (IOException e) {
            // OK
        }
    }

    private void addBand(String varName) {
        VariableMetadata variableMetadata = getVariableMetadata(varName);
        if (variableMetadata != null) {
            Band band = new Band(variableMetadata.name, variableMetadata.dataType, sceneRasterWidth, sceneRasterHeight);
            band.setDescription(variableMetadata.description);
            band.setUnit(variableMetadata.variable.getUnitsString());
            band.setNoDataValue(variableMetadata.fillValue);
            band.setNoDataValueUsed(variableMetadata.fillValue != Double.NaN);
            band.setSpectralWavelength(getWavelengthFromBandName(varName));
            product.addBand(band);
            final VariableReader variableReader = new VariableReader(variableMetadata.variable);
            bandMap.put(band, variableReader);
        }
    }

    private void addBand(String varName, String suffix, int[] origin, int[] shape, int binDimIndex) {
        final VariableMetadata variableMetadata = getVariableMetadata(varName);

        if (variableMetadata != null) {
            final String bandName = variableMetadata.name + suffix;

            final Band band = new Band(bandName, variableMetadata.dataType, sceneRasterWidth, sceneRasterHeight);
            band.setDescription(variableMetadata.description);
            band.setUnit(variableMetadata.variable.getUnitsString());
            band.setNoDataValue(variableMetadata.fillValue);
            band.setNoDataValueUsed(variableMetadata.fillValue != Double.NaN);
            band.setSpectralWavelength(getWavelengthFromBandName(varName));

            product.addBand(band);
            final VariableReader variableReader = new VariableReader(variableMetadata.variable, origin, shape, binDimIndex);
            bandMap.put(band, variableReader);
        }
    }

    // package access for testing only tb 2013-08-12
    static int getWavelengthFromBandName(String bandName) {
        final String[] bandNameParts = bandName.split("_");
        for (String bandNamePart : bandNameParts) {
            if (StringUtils.isNumeric(bandNamePart, Integer.class)) {
                return Integer.parseInt(bandNamePart);
            }
        }
        return 0;
    }

    private VariableMetadata getVariableMetadata(String varName) {
        final Variable variable = netcdfFile.getRootGroup().findVariable(varName);
        if (variable == null) {
            return null;
        }

        Number numericValue;
        String description = variable.getDescription();
        if (description == null) {
            description = getAttributeStringValue(variable, "comment");
        }

        numericValue = getAttributeNumericValue(variable, "_FillValue");
        final double fillValue = numericValue != null ? numericValue.doubleValue() : Double.NaN;

        final DataType dataType = variable.getDataType();
        int productDataType = ProductData.TYPE_INT32;
        final DataType dType = DataType.getType(Double.class);
        final DataType fType = DataType.getType(Float.class);
        if (fType.equals(dataType)) {
            productDataType = ProductData.TYPE_FLOAT32;
        } else if (dType.equals(dataType)) {
            productDataType = ProductData.TYPE_FLOAT64;
        }

        return new VariableMetadata(variable, varName, description, fillValue, productDataType);
    }

    private static Number getAttributeNumericValue(Variable variable, String attributeName) {
        final Attribute att = variable.findAttribute(attributeName);
        return att != null ? att.getNumericValue() : null;
    }

    private static String getAttributeStringValue(Variable variable, String attributeName) {
        final Attribute att = variable.findAttribute(attributeName);
        return att != null ? att.getStringValue() : null;
    }

    static ProductData.UTC extractStartTime(NetcdfFile netcdfFile) {
        return extractTime(netcdfFile, "time_coverage_start");
    }

    static ProductData.UTC extractEndTime(NetcdfFile netcdfFile) {
        return extractTime(netcdfFile, "time_coverage_end");
    }

    private static ProductData.UTC extractTime(NetcdfFile netcdfFile, String attributeName) {
        final Attribute timeAttribute = netcdfFile.findGlobalAttribute(attributeName);
        if (timeAttribute == null) {
            return null;
        }
        String timeAsString = timeAttribute.getStringValue();
        timeAsString = timeAsString.substring(0, timeAsString.length() - 1);
        ProductData.UTC parsedDate = null;
        try {
            parsedDate = ProductData.UTC.parse(timeAsString, "yyyyMMddHHmm");
        } catch (ParseException ignored) {
        }
        return parsedDate;
    }

    private static class VariableMetadata {

        final Variable variable;
        final String name;
        final String description;
        final double fillValue;
        final int dataType;

        public VariableMetadata(Variable variable, String name, String description, double fillValue, int dataType) {
            this.variable = variable;
            this.name = name;
            this.description = description;
            this.fillValue = fillValue;
            this.dataType = dataType;
        }
    }
}
