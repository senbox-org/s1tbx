package org.esa.beam.binning.reader;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.Reprojector;
import org.esa.beam.binning.support.SEAGrid;
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
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class BinnedProductReader extends AbstractProductReader {

    public static final String COL_INDEX_BAND_NAME = "col_index";

    private NetcdfFile _netcdfFile;
    private Product _product;
    private PlanetaryGrid planetaryGrid;
    private int _sceneRasterWidth;
    private int _sceneRasterHeight;
    private RowInfo[] _rowInfos;
    private Map<Band, VariableMetadata> bandMap;

    /**
     * Constructs a new MERIS Binned Level-3 product reader.
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
            int numRows = 0;
            for (Dimension dimension : _netcdfFile.getDimensions()) {
                if (dimension.getName().equals("bin_index")) {
                    numRows = dimension.getLength();
                }
            }
            planetaryGrid = createPlanetaryGrid(numRows);
            Geometry roiGeometry = null;
            for (Attribute attribute : _netcdfFile.getGlobalAttributes()) {
                if (attribute.getName().equals("region")) {
                    WKTReader wktReader = new WKTReader();
                    try {
                        roiGeometry = wktReader.read(attribute.getStringValue());
                    } catch (ParseException e) {
                        //todo error handling
                    }
                }
            }
            final Rectangle sceneRasterRectangle = Reprojector.computeRasterSubRegion(planetaryGrid, roiGeometry);
            _sceneRasterHeight = sceneRasterRectangle.height;
            _sceneRasterWidth = sceneRasterRectangle.width;
            File productFile = new File(path);
            _product = new Product(FileUtils.getFilenameWithoutExtension(productFile),
                                   "Binned SeaDas data",
                                   _sceneRasterWidth,
                                   _sceneRasterHeight,
                                   this);

            MetadataUtils.readNetcdfMetadata(_netcdfFile, _product.getMetadataRoot());

            for (Variable variable : _netcdfFile.getVariables()) {
                addBand(variable.getName());
            }
            _product.setQuicklookBandName("bl_nobs");
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

    private PlanetaryGrid createPlanetaryGrid(int numRows) {
        if (numRows > 0) {
            return new SEAGrid(numRows);
        } else {
            return new SEAGrid();
        }
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

//        final Variable idxVariable = _netcdfFile.getRootGroup().findVariable("idx");
//        short[] rasterData = (short[]) destBuffer.getElems();

        Number[] rasterData;
        final String destBandName = destBand.getName();
        if (destBuffer.getType() == ProductData.TYPE_INT32) {
            final int[] elems = (int[]) destBuffer.getElems();
            rasterData = new Integer[elems.length];
            for (int i = 0; i < elems.length; i++) {
                rasterData[i] = elems[i];
            }
        } else if (destBuffer.getType() == ProductData.TYPE_FLOAT32) {
            final float[] elems = (float[]) destBuffer.getElems();
            rasterData = new Float[elems.length];
            for (int i = 0; i < elems.length; i++) {
                rasterData[i] = elems[i];
            }
        } else if (destBuffer.getType() == ProductData.TYPE_FLOAT64) {
            final double[] elems = (double[]) destBuffer.getElems();
            rasterData = new Double[elems.length];
            for (int i = 0; i < elems.length; i++) {
                rasterData[i] = elems[i];
            }
        } else {
            final double[] elems = (double[]) destBuffer.getElems();
            rasterData = new Double[elems.length];
            for (int i = 0; i < elems.length; i++) {
                rasterData[i] = elems[i];
            }
        }
//        if (destBandName.equals("bi_vsize") || destBandName.equals("bi_hsize")) {
//            rasterData = (Double[]) destBuffer.getElems();
//        } else if (destBandName.startsWith("bl") && !destBandName.equals("bl_bin_num") && !destBandName.equals("bl_nobs")
//                && !destBandName.equals("bl_nscenes")) {
//            rasterData = (Float[]) destBuffer.getElems();
//        } else {
//            destBuffer.getType()
//            for (Object o : (Object[]) destBuffer.getElems()) {
//                try {
//                    Integer i = (Integer) o;
//                } catch (ClassCastException e) {
//                    System.out.println("numberValue = " + o.toString());
//                }
//            }
//            rasterData = (Integer[]) destBuffer.getElems();
//        }

//        if (_rowInfos == null) {
//            _rowInfos = createRowInfos();
//        }
        VariableMetadata variableMetadata = bandMap.get(destBand);
        boolean readColIndex = variableMetadata == null; // this band has no variables associated

        final int height = _sceneRasterHeight;
        final int width = _sceneRasterWidth;
        final PlanetaryGrid grid = planetaryGrid;

        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceHeight);
        try {
            if (readColIndex) {
                for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final int rowIndex = (height - 1) - y;
                    final double centerLat = grid.getCenterLat(rowIndex);
                    for (int x = sourceOffsetX; x < sourceOffsetX + sourceWidth; x++) {
                        final double lon = x * 360.0 / width;
                        final int colIndex = (int) grid.getBinIndex(centerLat, lon);
//                        final int colIndex = grid.getColIndex(rowIndex, lon);
                        final int rasterIndex = sourceWidth * (y - sourceOffsetY) + (x - sourceOffsetX);
                        rasterData[rasterIndex] = (short) colIndex;
                    }
                    pm.worked(1);
                }
            } else {
                final Variable binVariable = variableMetadata.variable;

                final Number fillValueN = getAttributeNumericValue(binVariable, "_FillValue");

                Number fillValue = fillValueN != null ? fillValueN.shortValue() : 0;

                if (destBuffer.getType() == ProductData.TYPE_INT32) {
                    fillValue = fillValueN != null ? fillValueN.intValue() : 0;
                } else if (destBuffer.getType() == ProductData.TYPE_FLOAT32) {
                    fillValue = fillValueN != null ? fillValueN.floatValue() : 0;
                } else if (destBuffer.getType() == ProductData.TYPE_FLOAT64) {
                    fillValue = fillValueN != null ? fillValueN.doubleValue() : 0;
                } else {
                    fillValue = fillValueN != null ? fillValueN.intValue() : 0;
                }

                int[] lineOffsets = new int[1];
                int[] lineLengths = new int[1];

                Arrays.fill(rasterData, fillValue);

                for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final int rowIndex = (height - 1) - y;
                    final RowInfo rowInfo = new RowInfo((int) grid.getFirstBinIndex(rowIndex), grid.getNumCols(rowIndex));
//                    final RowInfo rowInfo = _rowInfos[rowIndex];
//                    if (rowInfo != null) {

                    final int lineOffset = rowInfo.offset;
                    final int lineLength = rowInfo.length;

                    lineOffsets[0] = lineOffset;
                    lineLengths[0] = lineLength;

                    lineLengths[0] = grid.getNumRows();
                    lineOffsets[0] = 0;
//                        final int[] idxValues;
                    final int[] binValues;
                    try {
                        synchronized (_netcdfFile) {
//                                idxValues = (int[]) idxVariable.read(lineOffsets, lineLengths).getStorage();
                            binValues = (int[]) binVariable.read(lineOffsets, lineLengths).getStorage();
                        }
                    } catch (InvalidRangeException e) {
                        // ?!?!?!?
                        throw new IOException("Format problem.");
                    }

                    int lineIndex0 = 0;
                    for (int x = sourceOffsetX; x < sourceOffsetX + sourceWidth; x++) {
                        final double lon = x * 360.0 / width;
//                            final int binIndex = grid.getBinIndex(rowIndex, lon);
                        final long binIndex = grid.getBinIndex(grid.getCenterLat(rowIndex), lon);
//                            int lineIndex = -1;
//                            for (int i = lineIndex0; i < lineLength; i++) {
//                                if (idxValues[i] >= binIndex) {
//                                    if (idxValues[i] == binIndex) {
//                                        lineIndex = i;
//                                    }
//                                    lineIndex0 = i;
//                                    break;
//                                }
//                            }
//                            if (lineIndex >= 0) {
                        final int rasterIndex = sourceWidth * (y - sourceOffsetY) + (x - sourceOffsetX);
//                            final Band bl_bin_num_band = _product.getBand("bl_bin_num");
//                            final int rasterDataIndex = bl_bin_num_band.getData().getElemIntAt((int) binIndex);
                        final int rasterDataIndex = grid.getRowIndex(binIndex);
//                            rasterData[rasterIndex] = binValues[rasterDataIndex];
//                            }
                    }
                    pm.worked(1);
//                    }
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
        planetaryGrid = null;
        _rowInfos = null;
    }

    /////////////////////////////////////////////////////////////////////////
    // private helpers
    /////////////////////////////////////////////////////////////////////////

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
        } catch (IOException e) {
            // OK
        }
    }

    private void addBand(String varName) {
        VariableMetadata variableMetadata = getVariableMetadata(varName);
        if (variableMetadata != null) {
            int dataType = ProductData.TYPE_INT32;
            if (varName.equals("bi_vsize") || varName.equals("bi_hsize")) {
                dataType = ProductData.TYPE_FLOAT64;
            }
            if (varName.startsWith("bl") && !varName.equals("bl_bin_num") && !varName.equals("bl_nobs") && !varName.equals("bl_nscenes")) {
                dataType = ProductData.TYPE_FLOAT32;
            }
            Band band = new Band(variableMetadata.name, dataType, _sceneRasterWidth, _sceneRasterHeight);
            band.setDescription(variableMetadata.description);
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

        stringValue = getAttributeStringValue(variable, "comment");
        final String description = stringValue;

        numericValue = getAttributeNumericValue(variable, "_FillValue");
        final double fillValue = numericValue != null ? numericValue.doubleValue() : Double.NaN;

        return new VariableMetadata(variable, varName, description, fillValue);
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

        public VariableMetadata(Variable variable, String name, String description, double fillValue) {
            this.variable = variable;
            this.name = name;
            this.description = description;
            this.fillValue = fillValue;
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
