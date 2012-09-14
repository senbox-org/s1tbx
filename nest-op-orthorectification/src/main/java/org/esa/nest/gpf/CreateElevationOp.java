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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.resamp.Resampling;
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
import org.esa.nest.dataio.dem.FileElevationModel;
import org.esa.nest.datamodel.Unit;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
    CreateElevationBandOp adds an elevation band to a product
 */

@OperatorMetadata(alias="CreateElevation",
        category = "Geometry\\DEM Tools",
        description="Creates a DEM band")
public final class CreateElevationOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {"ACE", "GETASSE30", "SRTM 3Sec", "ASTER 1sec GDEM"},
            description = "The digital elevation model.", defaultValue="SRTM 3Sec", label="Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(description = "The elevation band name.", defaultValue="elevation", label="Elevation Band Name")
    private String elevationBandName = "elevation";

    @Parameter(description = "The external DEM file.", defaultValue=" ", label="External DEM")
    private String externalDEM = " ";

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME, ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
            ResamplingFactory.CUBIC_CONVOLUTION_NAME, ResamplingFactory.BISINC_INTERPOLATION_NAME,
            ResamplingFactory.BICUBIC_INTERPOLATION_NAME}, defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
                label="Resampling Method")
    private String resamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

    private FileElevationModel fileElevationModel = null;
    private ElevationModel dem = null;
    private Band elevationBand = null;
    private float noDataValue = 0;

    private final Map<Band, Band> sourceRasterMap = new HashMap<Band, Band>(10);

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

            final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
            final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);
            if (demDescriptor == null)
                throw new OperatorException("The DEM '" + demName + "' is not supported.");
            if (demDescriptor.isInstallingDem())
                throw new OperatorException("The DEM '" + demName + "' is currently being installed.");

            if(externalDEM != null && !externalDEM.trim().isEmpty()) {

                fileElevationModel = new FileElevationModel(new File(externalDEM),
                        ResamplingFactory.createResampling(resamplingMethod));
                noDataValue = fileElevationModel.getNoDataValue();
            } else {

                dem = DEMFactory.createElevationModel(demName, resamplingMethod);
                noDataValue = dem.getDescriptor().getNoDataValue();
            }
            
            createTargetProduct(demDescriptor);

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     * @param demDescriptor dem
     */
    void createTargetProduct(final ElevationModelDescriptor demDescriptor) {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());
        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        for(Band band : sourceProduct.getBands()) {
            if(band.getName().equalsIgnoreCase(elevationBandName))
                throw new OperatorException("Band "+elevationBandName+" already exists. Try another name.");
            if(band instanceof VirtualBand) {
                final VirtualBand sourceBand = (VirtualBand) band;
                final VirtualBand targetBand = new VirtualBand(sourceBand.getName(),
                                   sourceBand.getDataType(),
                                   sourceBand.getRasterWidth(),
                                   sourceBand.getRasterHeight(),
                                   sourceBand.getExpression());
                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                targetProduct.addBand(targetBand);
                sourceRasterMap.put(targetBand, band);
            } else {
                final Band targetBand = ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct);
                targetBand.setSourceImage(band.getSourceImage());
                sourceRasterMap.put(targetBand, band);
            }
        }

        elevationBand = targetProduct.addBand(elevationBandName, ProductData.TYPE_FLOAT32);
        elevationBand.setNoDataValue(noDataValue);
        elevationBand.setUnit(Unit.METERS);
        elevationBand.setDescription(demDescriptor.getName());
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

        try {
            if(targetBand == elevationBand) {
                final Rectangle targetRectangle = targetTile.getRectangle();
                final int x0 = targetRectangle.x;
                final int y0 = targetRectangle.y;
                final int w = targetRectangle.width;
                final int h = targetRectangle.height;
                final ProductData trgData = targetTile.getDataBuffer();

                final TileGeoreferencing tileGeoRef = new TileGeoreferencing(targetProduct, x0, y0, w, h);

                final float demNoDataValue = dem.getDescriptor().getNoDataValue();
                final float[][] localDEM = new float[h+2][w+2];
                DEMFactory.getLocalDEM(dem, demNoDataValue, tileGeoRef, x0, y0, w, h, localDEM);

                final TileIndex trgIndex = new TileIndex(targetTile);

                final int maxX = x0 + w;
                final int maxY = y0 + h;
                for (int y = y0; y < maxY; ++y) {
                    final int yy = y-y0+1;
                    trgIndex.calculateStride(y);
                    for (int x = x0; x < maxX; ++x) {

                        trgData.setElemDoubleAt(trgIndex.getIndex(x), localDEM[yy][x-x0+1]);
                    }
                }
            }
        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
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
            super(CreateElevationOp.class);
            setOperatorUI(CreateElevationOpUI.class);
        }
    }
}