/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.dataio.dem.DEMFactory;
import org.esa.nest.dataio.dem.FileElevationModel;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;

import java.awt.*;
import java.io.File;

/**
 * This operator detects mountain area using DEM and gnerates a terrain mask for given SAR image.
 */

@OperatorMetadata(alias="Terrain-Mask",
        category = "Classification\\Masks",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description="Terrain Mask Generation")
public final class TerrainMaskOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {"ACE", "GETASSE30", "SRTM 3Sec", "ASTER 1sec GDEM"},
               description = "The digital elevation model.",
               defaultValue="SRTM 3Sec", label="Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
                           ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
                           ResamplingFactory.CUBIC_CONVOLUTION_NAME,
                           ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
                           ResamplingFactory.BISINC_INTERPOLATION_NAME},
               defaultValue = ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
               label="DEM Resampling Method")
    private String demResamplingMethod = ResamplingFactory.NEAREST_NEIGHBOUR_NAME;

    @Parameter(label="External DEM")
    private File externalDEMFile = null;

    @Parameter(label="DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(valueSet = {WINDOW_SIZE_5x5, WINDOW_SIZE_7x7, WINDOW_SIZE_9x9, WINDOW_SIZE_11x11, WINDOW_SIZE_13x13,
            WINDOW_SIZE_15x15, WINDOW_SIZE_17x17}, defaultValue = WINDOW_SIZE_15x15, label="Window Size")
    private String windowSizeStr = WINDOW_SIZE_15x15;

    @Parameter(description = "Threshold for detection", interval = "(0, *)", defaultValue = "40.0", label="Threshold (m)")
    private double thresholdInMeter = 40.0;

    private ElevationModel dem = null;
    private int windowSize = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private boolean isElevationModelAvailable = false;
    private float demNoDataValue = 0; // no data value for DEM

    public static String TERRAIN_MASK_NAME = "Terrain_Mask";
    private static final String WINDOW_SIZE_5x5 = "5x5";
    private static final String WINDOW_SIZE_7x7 = "7x7";
    private static final String WINDOW_SIZE_9x9 = "9x9";
    private static final String WINDOW_SIZE_11x11 = "11x11";
    private static final String WINDOW_SIZE_13x13 = "13x13";
    private static final String WINDOW_SIZE_15x15 = "15x15";
    private static final String WINDOW_SIZE_17x17 = "17x17";

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            if(OperatorUtils.isMapProjected(sourceProduct)) {
                throw new OperatorException("Source product already map projected");
            }

            setWindowSize();

            getSourceImageDimension();

            createTargetProduct();

            if(externalDEMFile == null) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            DEMFactory.validateDEM(demName, sourceProduct);

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    @Override
    public synchronized void dispose() {
        if (dem != null) {
            dem.dispose();
            dem = null;
        }
    }

    /**
     * Get source image width and height.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    /**
     * Set Window size.
     */
    private void setWindowSize() {

        if (windowSizeStr.equals(WINDOW_SIZE_5x5)) {
            windowSize = 5;
        } else if (windowSizeStr.equals(WINDOW_SIZE_7x7)) {
            windowSize = 7;
        } else if (windowSizeStr.equals(WINDOW_SIZE_9x9)) {
            windowSize = 9;
        } else if (windowSizeStr.equals(WINDOW_SIZE_11x11)) {
            windowSize = 11;
        } else if (windowSizeStr.equals(WINDOW_SIZE_13x13)) {
            windowSize = 13;
        } else if (windowSizeStr.equals(WINDOW_SIZE_15x15)) {
            windowSize = 15;
        } else if (windowSizeStr.equals(WINDOW_SIZE_17x17)) {
            windowSize = 17;
        } else {
            throw new OperatorException("Unknown window size: " + windowSize);
        }
    }

    /**
     * Get elevation model.
     * @throws Exception The exceptions.
     */
    private synchronized void getElevationModel() throws Exception {

        if(isElevationModelAvailable) return;
        try {
            if(externalDEMFile != null) { // if external DEM file is specified by user
                dem = new FileElevationModel(externalDEMFile, demResamplingMethod, (float)externalDEMNoDataValue);
                demNoDataValue = (float) externalDEMNoDataValue;
                demName = externalDEMFile.getPath();

            } else {
                dem = DEMFactory.createElevationModel(demName, demResamplingMethod);
                demNoDataValue = dem.getDescriptor().getNoDataValue();
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        isElevationModelAvailable = true;
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceImageWidth,
                                    sourceImageHeight);

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);

        if(externalDEMFile != null) { // if external DEM file is specified by user
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, externalDEMFile.getPath());
        } else {
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.DEM, demName);
        }

        absTgt.setAttributeString("DEM resampling method", demResamplingMethod);

        if(externalDEMFile != null) {
            absTgt.setAttributeDouble("external DEM no data value", externalDEMNoDataValue);
        }

        addBitmasks(targetProduct);
    }

    private void addSelectedBands() {

        for(Band band : sourceProduct.getBands()) {
            if(band instanceof VirtualBand) {
                final VirtualBand sourceBand = (VirtualBand) band;
                final VirtualBand targetBand = new VirtualBand(sourceBand.getName(),
                                   sourceBand.getDataType(),
                                   sourceBand.getRasterWidth(),
                                   sourceBand.getRasterHeight(),
                                   sourceBand.getExpression());
                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                targetProduct.addBand(targetBand);
            } else {
                final Band targetBand = ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, false);
                targetBand.setSourceImage(band.getSourceImage());
            }
        }

        final Band targetBand = new Band(TERRAIN_MASK_NAME,
                                   ProductData.TYPE_INT8,
                                   sourceImageWidth,
                                   sourceImageHeight);

        targetBand.setUnit(Unit.AMPLITUDE);
        targetProduct.addBand(targetBand);
    }

    public static void addBitmasks(final Product product) {

        for(Band band : product.getBands()) {
            if(band.getName().contains(TERRAIN_MASK_NAME)) {
                final String expression = band.getName() + " > 0";

                final Mask mask = new Mask(band.getName()+"_detection",
                             product.getSceneRasterWidth(),
                             product.getSceneRasterHeight(),
                             Mask.BandMathsType.INSTANCE);

                mask.setDescription("Terrain Detection");
                mask.getImageConfig().setValue("color", Color.ORANGE);
                mask.getImageConfig().setValue("transparency", 0.7);
                mask.getImageConfig().setValue("expression", expression);
                mask.setNoDataValue(0);
                mask.setNoDataValueUsed(true);
                product.getMaskGroup().add(mask);
            }
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final Rectangle targetTileRectangle = targetTile.getRectangle();
        final int x0 = targetTileRectangle.x;
        final int y0 = targetTileRectangle.y;
        final int w  = targetTileRectangle.width;
        final int h  = targetTileRectangle.height;

        try {
            if (!isElevationModelAvailable) {
                getElevationModel();
            }

            final double[][] localDEM = new double[h+windowSize+2][w+windowSize+2];
            final TileGeoreferencing tileGeoRef = new TileGeoreferencing(targetProduct, x0, y0, w+windowSize, h+windowSize);

            final boolean valid = DEMFactory.getLocalDEM(
                    dem, demNoDataValue, demResamplingMethod, tileGeoRef, x0, y0, w+windowSize, h+windowSize, sourceProduct, true, localDEM);

             if(!valid) {
                return;
             }

            final ProductData targetData = targetTile.getDataBuffer();
            final TileIndex targetIndex = new TileIndex(targetTile);
            final double[] minMaxMean = {demNoDataValue, demNoDataValue, demNoDataValue};

            final int ymax = y0 + h;
            final int xmax = x0 + w;
            for (int y = y0; y < ymax; y += windowSize) {
                for (int x = x0; x < xmax; x += windowSize) {
                    getMinMaxMean(x0, y0, x, y, localDEM, minMaxMean);
                    createTerrainMask(x0, y0, x, y, xmax, ymax, minMaxMean, localDEM, targetIndex, targetData);
                }
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void getMinMaxMean(final int x0, final int y0, final int x, final int y,
                               final double[][] localDEM, final double[] minMaxMean) {

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double sum = 0.0;
        int numSamples = 0;
        final int maxX = x + windowSize;
        final int maxY = y + windowSize;
        for (int yy = y; yy < maxY; yy++) {
            final int yIdx = yy - y0 + 1;
            for (int xx = x; xx < maxX; xx++) {
                final double h = localDEM[yIdx][xx - x0 + 1];
                if(h == demNoDataValue)
                    continue;

                if (min > h) {
                    min = h;
                }

                if (max < h) {
                    max = h;
                }

                sum += h;
                numSamples++;
            }
        }

        minMaxMean[0] = min;
        minMaxMean[1] = max;
        minMaxMean[2] = sum/numSamples;
    }

    private void createTerrainMask(final int x0, final int y0, final int x, final int y, final int xmax, final int ymax,
                                   final double[] minMaxMean, final double[][] localDEM, final TileIndex targetIndex,
                                   final ProductData targetData)
    {
        final int maxX = Math.min(x + windowSize, xmax);
        final int maxY = Math.min(y + windowSize, ymax);

        final double elevDiff = minMaxMean[1] - minMaxMean[0];

        if (elevDiff >= thresholdInMeter) { // mountain detected

            for (int yy = y; yy < maxY; yy++) {
                targetIndex.calculateStride(yy);
                final int yIdx = yy - y0 + 1;
                for (int xx = x; xx < maxX; xx++) {
                    final double h = localDEM[yIdx][xx - x0 + 1];
                    if (h != demNoDataValue) {
                        targetData.setElemIntAt(targetIndex.getIndex(xx), 1);
                    } else {
                        targetData.setElemIntAt(targetIndex.getIndex(xx), 0);
                    }
                }
            }

        } else { // no mountain detected

            for (int yy = y; yy < maxY; yy++) {
                targetIndex.calculateStride(yy);
                for (int xx = x; xx < maxX; xx++) {
                    targetData.setElemIntAt(targetIndex.getIndex(xx), 0);
                }
            }
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(TerrainMaskOp.class);
        }
    }
}
