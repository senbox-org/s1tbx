package org.esa.beam.binning.reader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
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
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class BinnedProductReader extends AbstractProductReader {

    private NetcdfFile netcdfFile;
    private Product product;
    private SEAGrid planetaryGrid;
    private int sceneRasterWidth;
    private int sceneRasterHeight;
    private Map<Band, Variable> bandMap;
    /**
     * Key: BinIndex in PlanetaryGrid
     * Value: BinIndex in bin_list
     */
    private Map<Integer, Integer> indexMap;
    private int[] binOffsets;
    private int[] binExtents;
    private int[] binIndexes;
    private double pixelSizeX;

    private final Map<Band, Array> bandDataCacheMap;

    /**
     * Constructs a new MERIS Binned Level-3 product reader.
     *
     * @param readerPlugIn the plug-in which created this reader instance
     */
    public BinnedProductReader(BinnedProductReaderPlugin readerPlugIn) {
        super(readerPlugIn);
        bandDataCacheMap = new Hashtable<Band, Array>();
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
        netcdfFile = NetcdfFile.open(path);
        bandMap = new HashMap<Band, Variable>();
        try {
            initProductWidthAndHeight();
            initProduct();
            initGeoCoding();
            readMetadata();
            initBands();
            initPlanetaryGrid();

            //create indexMap
            final Variable bl_bin_num = netcdfFile.findVariable(NetcdfFile.escapeName("bl_bin_num"));
            if (bl_bin_num != null) {
                synchronized (netcdfFile) {
                    final Object storage = bl_bin_num.read().getStorage();
                    binIndexes = (int[]) storage;
                    indexMap = new HashMap<Integer, Integer>(binIndexes.length);
                    for (int i = 0; i < binIndexes.length; i++) {
                        indexMap.put(binIndexes[i], i);
                    }
                }
            }

            final Variable bi_begin = netcdfFile.findVariable(NetcdfFile.escapeName("bi_begin"));
            if (bi_begin != null) {
                synchronized (netcdfFile) {
                    final Object storage = bi_begin.read().getStorage();
                    binOffsets = (int[]) storage;
                }
            } else {
                binOffsets = new int[sceneRasterHeight];
                Arrays.fill(binOffsets, 1);
            }


            final Variable bi_extent = netcdfFile.findVariable(NetcdfFile.escapeName("bi_extent"));
            if (bi_extent != null) {
                synchronized (netcdfFile) {
                    final Object storage = bi_extent.read().getStorage();
                    binExtents = (int[]) storage;
                }
            }
        } catch (IOException e) {
            dispose();
            throw e;
        }
        return product;
    }

    private void initBands() throws IOException {
        int largestDimensionSize = getLargestDimensionSize();
        //read geophysical band values
        for (Variable variable : netcdfFile.getVariables()) {
            final String bandName = variable.getName();
            if (variable.getDimensions().get(0).getLength() == largestDimensionSize) {
                addBand(bandName);
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
        File productFile = new File(getInput().toString());
        final String productName = FileUtils.getFilenameWithoutExtension(productFile);
        final String productType = netcdfFile.findGlobalAttribute("title").getStringValue();
        product = new Product(productName, productType, sceneRasterWidth, sceneRasterHeight, this);
        product.setFileLocation(productFile);
        product.setAutoGrouping("adg:aph:atot:bl_Rrs:chlor_a:Rrs:water");
        product.setStartTime(extractStartTime(netcdfFile));
        product.setEndTime(extractEndTime(netcdfFile));
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
        final Dimension bin_index = netcdfFile.findDimension("bin_index");
        if (bin_index != null) {
            sceneRasterHeight = bin_index.getLength();
        } else {
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
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        if (sourceStepX != 1 || sourceStepY != 1) {
            throw new IOException("Sub-sampling is not supported by this product reader.");
        }
        if (sourceWidth != destWidth || sourceHeight != destHeight) {
            throw new IllegalStateException("sourceWidth != destWidth || sourceHeight != destHeight");
        }
        Variable binVariable = bandMap.get(destBand);
        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceHeight);
        try {
            final Number fillValueN = getAttributeNumericValue(binVariable, "_FillValue");
            float fillValue = fillValueN != null ? fillValueN.floatValue() : 0;
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

                final int binOffset = binOffsets[lineIndex];
                if (binOffset > 0) {
                    int[] origin = {0};
                    int[] shape = {1};
                    final int startBinIndex;
                    final int endBinIndex;
                    if (indexMap != null) {
                        final Integer binIndexInBinList = indexMap.get(binOffset);
                        origin[0] = binIndexInBinList;
                        final int binExtent = binExtents[lineIndex];
                        shape[0] = binExtent;
                        startBinIndex = 0;
                        endBinIndex = binExtent;
                    } else {
                        startBinIndex = getBinIndexInGrid(sourceOffsetX, lineIndex);
                        endBinIndex = getBinIndexInGrid(sourceOffsetX + sourceWidth - 1, lineIndex) + 1;
                        final int extent = endBinIndex - startBinIndex;
                        origin[0] = 0;
                        shape[0] = extent;
                    }
                    final Array lineValues;
                    try {
                        if (indexMap != null) {
                            synchronized (netcdfFile) {
                                lineValues = binVariable.read(origin, shape);
                            }
                        } else {
                            final Array cachedBandData = getCachedBandData(destBand);
                            if (cachedBandData != null) {
                                lineValues = cachedBandData;
                            } else {
                                synchronized (netcdfFile) {
                                    lineValues = binVariable.read();
                                }
                            }
                        }
                    } catch (InvalidRangeException e) {
                        throw new IOException("Format problem.");
                    }

                    if (indexMap == null) {
                        appendBandDataToCache(destBand, lineValues);
                    }

                    for (int i = startBinIndex; i < endBinIndex; i++) {
                        final float value = lineValues.getFloat(i);
                        if (value != fillValue) {
                            int binIndexInGrid;
                            if (indexMap != null) {
                                binIndexInGrid = binIndexes[indexMap.get(binOffset) + i];
                            } else {
                                binIndexInGrid = origin[0] + i;
                            }

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
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    private Array getCachedBandData(Band band) {
        return bandDataCacheMap.get(band);
    }

    private void appendBandDataToCache(final Band band, Array bandData) {
        if (bandDataCacheMap.containsKey(band)) {
            return;
        }
        bandDataCacheMap.put(band, bandData);
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                bandDataCacheMap.remove(band);
            }
        }, 2000);
    }

    private int[] getXValuesForBin(int binIndexInGrid, int row) {
        int numberOfBinsInRow = planetaryGrid.getNumCols(row);
        int firstBinIndex = (int) planetaryGrid.getFirstBinIndex(row);
        if (firstBinIndex >= binIndexInGrid) {
            numberOfBinsInRow = planetaryGrid.getNumCols(row - 1);
            firstBinIndex = (int) planetaryGrid.getFirstBinIndex(row -1) + 1;
        }
        double binIndexInRow = binIndexInGrid - firstBinIndex;
        final double longitudeExtent = 360.0 / numberOfBinsInRow;
        final double smallestLongitude = binIndexInRow * longitudeExtent;
        final double largestLongitude = smallestLongitude + longitudeExtent;
        final int startX = (int) Math.floor(smallestLongitude / pixelSizeX);
        final int endX = (int) Math.ceil(largestLongitude / pixelSizeX);
        return new int[]{startX, endX};
    }

    private int getBinIndexInGrid(int x, int y) {
        final int numberOfBinsInRow = planetaryGrid.getNumCols(y);
        final double longitudeExtentPerBin = 360.0 / numberOfBinsInRow;
        final double pixelCenterLongitude = x * pixelSizeX + pixelSizeX / 2;
        final int firstBinIndex = (int) planetaryGrid.getFirstBinIndex(y);
        return ((int) (pixelCenterLongitude / longitudeExtentPerBin)) + firstBinIndex;
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

        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
        bandMap.clear();
        if (indexMap != null) {
            indexMap.clear();
        }
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
            band.setNoDataValue(variableMetadata.fillValue);
            band.setNoDataValueUsed(variableMetadata.fillValue != Double.NaN);
            band.setSpectralWavelength(getWavelengthFromBandName(varName));
            product.addBand(band);
            bandMap.put(band, variableMetadata.variable);
        }
    }

    private int getWavelengthFromBandName(String bandName) {
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
        String stringValue;

        stringValue = getAttributeStringValue(variable, "comment");
        final String description = stringValue;

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
