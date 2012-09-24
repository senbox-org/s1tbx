package org.esa.beam.binning.reader;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.Reprojector;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.binning.support.SeadasGrid;
import org.esa.beam.dataio.netcdf.util.MetadataUtils;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
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

    private NetcdfFile netcdfFile;
    private Product product;
    private PlanetaryGrid planetaryGrid;
    private int sceneRasterWidth;
    private int sceneRasterHeight;
    private Map<Band, VariableMetadata> bandMap;
    /**
     * Key: BinIndex in PlanetaryGrid
     * Value: BinIndex in bin_list
     */
    private Map<Integer, Integer> indexMap;
    private Map<Variable, float[]> storages;

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
        netcdfFile = NetcdfFile.open(path);
        bandMap = new HashMap<Band, VariableMetadata>(10);
        try {
            //create grid
            int numRows = 0;
            for (Dimension dimension : netcdfFile.getDimensions()) {
                if (dimension.getName().equals("bin_index")) {
                    numRows = dimension.getLength();
                }
            }
            planetaryGrid = createPlanetaryGrid(numRows);

            //determine sceneRasterWidth and -Height
            Geometry roiGeometry = null;
            for (Attribute attribute : netcdfFile.getGlobalAttributes()) {
                if (attribute.getName().equals("region")) {
                    WKTReader wktReader = new WKTReader();
                    try {
                        roiGeometry = wktReader.read(attribute.getStringValue());
                    } catch (ParseException e) {
                        //todo error handling
                    }
                    break;
                }
            }
            final Rectangle sceneRasterRectangle = Reprojector.computeRasterSubRegion(planetaryGrid, roiGeometry);
            sceneRasterHeight = sceneRasterRectangle.height;
            sceneRasterWidth = sceneRasterRectangle.width;

            File productFile = new File(path);
            product = new Product(FileUtils.getFilenameWithoutExtension(productFile),
                                  "Binned SeaDas data",
                                  sceneRasterWidth,
                                  sceneRasterHeight,
                                  this);

            MetadataUtils.readNetcdfMetadata(netcdfFile, product.getMetadataRoot());

            //read geophysical band values
            for (Variable variable : netcdfFile.getVariables()) {
                final String bandName = variable.getName();
                if (!bandName.startsWith("bi_") && !bandName.equals("bl_bin_num") &&
                        !bandName.equals("bl_nobs") && !bandName.equals("bl_nscenes") &&
                        !bandName.equals("bl_weights")) {
                    addBand(bandName);
                }
            }
            product.setQuicklookBandName("water_class1");
            product.setFileLocation(productFile);
            if (product.getNumBands() == 0) {
                throw new IOException("No bands found.");
            }

            //create indexMap
            final Variable bl_bin_num = netcdfFile.findVariable(NetcdfFile.escapeName("bl_bin_num"));
            synchronized (netcdfFile) {
                final Object storage = bl_bin_num.read().getStorage();
                int[] binIndexes = (int[]) storage;
                indexMap = new HashMap<Integer, Integer>(binIndexes.length);
                for (int i = 0; i < binIndexes.length; i++) {
                    indexMap.put(binIndexes[i], i);
                }
            }
            storages = new HashMap<Variable, float[]>();

            initGeoCoding();

        } catch (IOException e) {
            dispose();
            throw e;
        }
        return product;
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
        float[] rasterData = (float[]) destBuffer.getElems();
        VariableMetadata variableMetadata = bandMap.get(destBand);
        //todo check whether grid is y-flipped / see CfGeocodingPart:createConventionBasedMapGeoCoding
        final SeadasGrid seadasGrid = new SeadasGrid(planetaryGrid);
        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceHeight);
        try {
            final Variable binVariable = variableMetadata.variable;
            updateStorages(binVariable);
            final Number fillValueN = getAttributeNumericValue(binVariable, "_FillValue");
            float fillValue = fillValueN != null ? fillValueN.floatValue() : 0;
            Arrays.fill(rasterData, fillValue);
            for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
                for (int x = sourceOffsetX; x < sourceOffsetX + sourceWidth; x++) {
                    final GeoPos geoPos = product.getGeoCoding().getGeoPos(new PixelPos(x + 0.5f, y + 0.5f), null);
                    final long seaGridBinIndex = planetaryGrid.getBinIndex(geoPos.lat, geoPos.lon);
                    final long binIndex = seadasGrid.convertBinIndex(seaGridBinIndex);
                    if (indexMap.containsKey((int) binIndex)) {
                        final int otherBinIndex = indexMap.get((int) binIndex);
                        if (otherBinIndex > 0) {
                            final int rasterIndex = sourceWidth * (y - sourceOffsetY) + (x - sourceOffsetX);
                            rasterData[rasterIndex] = storages.get(binVariable)[otherBinIndex];
                        }
                    }
                }
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    private void updateStorages(Variable binVariable) throws IOException {
        if (!storages.containsKey(binVariable)) {
            try {
                storages.put(binVariable, (float[]) binVariable.read().getStorage());
            } catch (IOException e) {
                throw new IOException("Format problem.");
            }
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

        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
        bandMap.clear();
        indexMap.clear();
        storages.clear();
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
        float pixelSizeX = 360.0f / sceneRasterWidth;
        float pixelSizeY = 180.0f / sceneRasterHeight;
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
            int dataType = ProductData.TYPE_FLOAT32;
            Band band = new Band(variableMetadata.name, dataType, sceneRasterWidth, sceneRasterHeight);
            band.setDescription(variableMetadata.description);
            band.setNoDataValue(variableMetadata.fillValue);
            band.setNoDataValueUsed(variableMetadata.fillValue != Double.NaN);
            product.addBand(band);
            bandMap.put(band, variableMetadata);
        }
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

}
