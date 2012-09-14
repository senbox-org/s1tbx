/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.ReaderUtils;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The product reader for NetCDF products.
 *
 */
public class NetCDFReader extends AbstractProductReader {

    private NetcdfFile netcdfFile = null;
    private Product product = null;
    private NcVariableMap variableMap = null;
    private boolean yFlipped = false;
    private final ProductReaderPlugIn readerPlugIn;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public NetCDFReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        this.readerPlugIn = readerPlugIn;
    }

    private void initReader() {
        product = null;
        netcdfFile = null;
        variableMap = null;
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = ReaderUtils.getFileFromInput(getInput());
        initReader();

        netcdfFile = NetcdfFile.open(inputFile.getPath());
        if (netcdfFile == null) {
            close();
            throw new IllegalFileFormatException(inputFile.getName() +
                    " Could not be interpretted by the reader.");
        }

        final Map<NcRasterDim, List<Variable>> variableListMap = NetCDFUtils.getVariableListMap(netcdfFile.getRootGroup());
        if (variableListMap.isEmpty()) {
            close();
            throw new IllegalFileFormatException("No netCDF variables found which could\n" +
                    "be interpreted as remote sensing bands.");  /*I18N*/
        }
        final NcRasterDim rasterDim = NetCDFUtils.getBestRasterDim(variableListMap);
        final Variable[] rasterVariables = NetCDFUtils.getRasterVariables(variableListMap, rasterDim);
        final Variable[] tiePointGridVariables = NetCDFUtils.getTiePointGridVariables(variableListMap, rasterVariables);

        variableMap = new NcVariableMap(rasterVariables);
        yFlipped = false;

        final NcAttributeMap globalAttributes = NcAttributeMap.create(netcdfFile);

        product = new Product(inputFile.getName(),
                               NetCDFUtils.getProductType(globalAttributes, readerPlugIn.getFormatNames()[0]),
                               rasterDim.getDimX().getLength(),
                               rasterDim.getDimY().getLength(),
                               this);
        product.setFileLocation(inputFile);
        product.setDescription(NetCDFUtils.getProductDescription(globalAttributes));
        product.setStartTime(NetCDFUtils.getSceneRasterStartTime(globalAttributes));
        product.setEndTime(NetCDFUtils.getSceneRasterStopTime(globalAttributes));

        addMetadataToProduct();
        addBandsToProduct(rasterVariables);
        addTiePointGridsToProduct(tiePointGridVariables);
        addGeoCodingToProduct(rasterDim);

        product.getGcpGroup();
        product.setModified(false);

        // update product type
        if(product.getProductType().equalsIgnoreCase(readerPlugIn.getFormatNames()[0])) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
            final String type = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
            if(!type.isEmpty())
                product.setProductType(type);
        }

        return product;
    }

    @Override
    public void close() throws IOException {
        if (product != null) {
            product = null;
            variableMap.clear();
            variableMap = null;
            netcdfFile.close();
            netcdfFile = null;
        }
        super.close();
    }

    private void addMetadataToProduct() {

        final Group rootGroup = netcdfFile.getRootGroup();
        NetCDFUtils.addGroups(product.getMetadataRoot(), rootGroup);

        AbstractMetadata.getAbstractedMetadata(product);
    }

    private void addBandsToProduct(final Variable[] variables) {
        for (Variable variable : variables) {
            final int rank = variable.getRank();
            final int width = variable.getDimension(rank - 1).getLength();
            final int height = variable.getDimension(rank - 2).getLength();
            final Band band = NetCDFUtils.createBand(variable, width, height);

            product.addBand(band);
        }
    }

    private void addTiePointGridsToProduct(final Variable[] variables) throws IOException {
        for (Variable variable : variables) {
            final int rank = variable.getRank();
            final int gridWidth = variable.getDimension(rank - 1).getLength();
            int gridHeight = variable.getDimension(rank - 2).getLength();
            if(rank >= 3 && gridHeight <= 1)
                gridHeight = variable.getDimension(rank - 3).getLength();
            final TiePointGrid tpg = NetCDFUtils.createTiePointGrid(variable, gridWidth, gridHeight,
                        product.getSceneRasterWidth(), product.getSceneRasterHeight());

            product.addTiePointGrid(tpg);
        }
    }

    private void addGeoCodingToProduct(final NcRasterDim rasterDim) throws IOException {
        setTiePointGeoCoding(product);
        if (product.getGeoCoding() == null) {
            setPixelGeoCoding(product);
        }
        if (product.getGeoCoding() == null) {
            yFlipped = setMapGeoCoding(rasterDim, product, netcdfFile, yFlipped);
        }
    }

    public static boolean setMapGeoCoding(final NcRasterDim rasterDim, final Product product,
                                       NetcdfFile netcdfFile, boolean yFlipped) {
        final NcVariableMap varMap = NcVariableMap.create(netcdfFile);

        Variable lonVar=null, latVar=null;
        for(String lonStr : NetcdfConstants.LON_VAR_NAMES) {
            lonVar = varMap.get(lonStr);
            if(lonVar != null)
                break;
        }
        for(String latStr : NetcdfConstants.LAT_VAR_NAMES) {
            latVar = varMap.get(latStr);
            if(latVar != null)
                break;
        }
        if (lonVar != null && latVar != null && rasterDim.fitsTo(lonVar, latVar)) {
            try {
                final NetCDFUtils.MapInfoX mapInfoX = NetCDFUtils.createMapInfoX(lonVar, latVar,
                                                                                 product.getSceneRasterWidth(),
                                                                                 product.getSceneRasterHeight());
                if (mapInfoX != null) {
                    yFlipped = mapInfoX.isYFlipped();
                    product.setGeoCoding(new MapGeoCoding(mapInfoX.getMapInfo()));
                }
            } catch (IOException e) {
                BeamLogManager.getSystemLogger().warning("Failed to create NetCDF geo-coding");
            }
        }
        return yFlipped;
    }

    public static void setTiePointGeoCoding(final Product product) {
        TiePointGrid lonGrid=null, latGrid=null;
        for(String lonStr : NetcdfConstants.LON_VAR_NAMES) {
            lonGrid = product.getTiePointGrid(lonStr);
            if(lonGrid != null)
                break;
        }
        for(String latStr : NetcdfConstants.LAT_VAR_NAMES) {
            latGrid = product.getTiePointGrid(latStr);
            if(latGrid != null)
                break;
        }
        if (latGrid != null && lonGrid != null) {       
            final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);
            product.setGeoCoding(tpGeoCoding);
        }
    }

    public static void setPixelGeoCoding(final Product product) throws IOException {
        Band lonBand=null, latBand=null;
        for(String lonStr : NetcdfConstants.LON_VAR_NAMES) {
            lonBand = product.getBand(lonStr);
            if(lonBand != null)
                break;
        }
        for(String latStr : NetcdfConstants.LAT_VAR_NAMES) {
            latBand = product.getBand(latStr);
            if(latBand != null)
                break;
        }
        if (latBand != null && lonBand != null) {
            product.setGeoCoding(new PixelGeoCoding(latBand, lonBand,
                                                     latBand.getValidPixelExpression(),
                                                     5, ProgressMonitor.NULL));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        Guardian.assertTrue("sourceStepX == 1 && sourceStepY == 1", sourceStepX == 1 && sourceStepY == 1);
        Guardian.assertTrue("sourceWidth == destWidth", sourceWidth == destWidth);
        Guardian.assertTrue("sourceHeight == destHeight", sourceHeight == destHeight);

        final int sceneHeight = product.getSceneRasterHeight();
        final int y0 = yFlipped ? (sceneHeight - 1) - sourceOffsetY : sourceOffsetY;

        final Variable[] variables = variableMap.getAll();
        Variable variable = null;
        for(Variable var : variables) {
            if(destBand.getName().equalsIgnoreCase(var.getName()) ||
               destBand.getName().equalsIgnoreCase(var.getShortName())) {
                variable = var;
                break;
            }
        }
        if(variable == null) {
            throw new IOException("Band "+destBand.getName()+" not found");
        }
        final int rank = variable.getRank();
        final int[] origin = new int[rank];
        final int[] shape = new int[rank];
        for (int i = 0; i < rank; i++) {
            shape[i] = 1;
            origin[i] = 0;
        }
        shape[rank - 2] = 1;
        shape[rank - 1] = destWidth;
        origin[rank - 1] = sourceOffsetX;

        pm.beginTask("Reading data from band " + destBand.getName(), destHeight);
        try {
            for (int y = 0; y < destHeight; y++) {
                origin[rank - 2] = yFlipped ? y0 - y : y0 + y;
                final Array array;
                synchronized (netcdfFile) {
                    array = variable.read(origin, shape);
                }
                System.arraycopy(array.getStorage(), 0, destBuffer.getElems(), y * destWidth, destWidth);
                pm.worked(1);
                if (pm.isCanceled()) {
                    throw new IOException("Process terminated by user."); /*I18N*/
                }
            }
        } catch (InvalidRangeException e) {
            final IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        } finally {
            pm.done();
        }
    }

}