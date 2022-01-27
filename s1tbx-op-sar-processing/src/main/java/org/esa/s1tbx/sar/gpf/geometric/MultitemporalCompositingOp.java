/*
 * Copyright (C) 2020 by SkyWatch Space Applications Inc.
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
package org.esa.s1tbx.sar.gpf.geometric;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This operator generates a composite SAR image from a stack of terrain-flattened images together with their
 * corresponding simulated local illuminated areas. The operation requires input images in map geometry. Therefore
 * the processing chain should be:
 * 1. Run Terrain Flattening operator with Output Simulate Image option selected;
 * 2. Run Range Doppler Geocoding operator with the terrain-flattened images as input;
 * 3. Run Create Stack with Maximum Extent option selected;
 * 4. Run this operator with the stack created above as input.
 * Images with both ascending and descending passes are preferred.
 */

@OperatorMetadata(alias = "Multitemporal-Compositing",
        category = "Radar/Radiometric",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2020 by SkyWatch Space Applications Inc.",
        description = "Compute composite image from multi-temporal RTCs", internal = false)
public class MultitemporalCompositingOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;


    private static final String PRODUCT_SUFFIX = "_MC";
    private static final String MASTER_TAG = "_mst";
    private static final String SLAVE_TAG = "_slv";
    private static final String SIMULATED_IMAGE = "simulatedImage";

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();
            validator.checkIfCollocatedStack();
            validator.checkIfCalibrated(true);
            validator.checkIfMapProjected(true);

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        final String[] sourceBandNames = sourceProduct.getBandNames();
        for (String srcBandName : sourceBandNames) {
            if (srcBandName.contains(SLAVE_TAG) || srcBandName.contains(SIMULATED_IMAGE)) {
                continue;
            }
            final String tgtBandName = srcBandName.substring(0, srcBandName.indexOf(MASTER_TAG));
            targetProduct.addBand(tgtBandName, ProductData.TYPE_FLOAT32);
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int x0 = targetTileRectangle.x;
            final int y0 = targetTileRectangle.y;
            final int w = targetTileRectangle.width;
            final int h = targetTileRectangle.height;
            final int xMax = x0 + w;
            final int yMax = y0 + h;

            final String targetBandName = targetBand.getName();
            final List<Band> sourceBandList = new ArrayList<>();
            final List<Band> simImgBandList = new ArrayList<>();
            for (Band srcBand : sourceProduct.getBands()) {
                final String srcBandName = srcBand.getName();
                if(srcBandName.contains(targetBandName)) {
                    sourceBandList.add(srcBand);
                    final Band simImgBand = getSimulatedImageBand(srcBandName);
                    if (simImgBand == null) {
                        throw new OperatorException("Simulated image band not found for source band " + srcBandName);
                    }
                    if (!simImgBandList.contains(simImgBand)) {
                        simImgBandList.add(simImgBand);
                    }
                }
            }
            final Band[] sourceBands = sourceBandList.toArray(new Band[0]);
            final Band[] simImgBands = simImgBandList.toArray(new Band[0]);
            final int numSourceBands = sourceBands.length;
            final double srcNoDataValue = sourceBands[0].getNoDataValue();
            final double simNoDataValue = simImgBands[0].getNoDataValue();

            final Tile[] sourceTile = new Tile[numSourceBands];
            final Tile[] simImgTile = new Tile[numSourceBands];
            final ProductData[] sourceData = new ProductData[numSourceBands];
            final ProductData[] simImgData = new ProductData[numSourceBands];
            for (int i = 0; i < numSourceBands; ++i) {
                sourceTile[i] = getSourceTile(sourceBands[i], targetTileRectangle);
                sourceData[i] = sourceTile[i].getDataBuffer();

                simImgTile[i] = getSourceTile(simImgBands[i], targetTileRectangle);
                simImgData[i] = simImgTile[i].getDataBuffer();
            }
            final TileIndex sourceIndex = new TileIndex(sourceTile[0]);
            final TileIndex simImgIndex = new TileIndex(simImgTile[0]);

            final ProductData targetData = targetTile.getDataBuffer();
            final TileIndex targetIndex = new TileIndex(targetTile);

            for (int y = y0; y < yMax; ++y) {
                targetIndex.calculateStride(y);
                sourceIndex.calculateStride(y);
                simImgIndex.calculateStride(y);

                for (int x = x0; x < xMax; ++x) {
                    final int tgtIdx = targetIndex.getIndex(x);
                    final int srcIdx = sourceIndex.getIndex(x);
                    final int simIdx = simImgIndex.getIndex(x);

                    final double[] area = new double[numSourceBands];
                    final double[] gamma0 = new double[numSourceBands];
                    double totalWeight = 0.0;
                    for (int i = 0; i < numSourceBands; ++i) {
                        area[i] = simImgData[i].getElemDoubleAt(simIdx);
                        gamma0[i] = sourceData[i].getElemDoubleAt(srcIdx);
                        if (area[i] != simNoDataValue && area[i] > 0.0 && gamma0[i] != srcNoDataValue) {
                            totalWeight += 1.0 / area[i];
                        }
                    }

                    if (totalWeight > 0.0) {
                        double sum = 0.0;
                        for (int i = 0; i < numSourceBands; ++i) {
                            if (area[i] != simNoDataValue && area[i] > 0.0) {
                                sum += gamma0[i] / (area[i] * totalWeight);
                            }
                        }
                        targetData.setElemDoubleAt(tgtIdx, sum);
                    } else {
                        targetData.setElemDoubleAt(tgtIdx, srcNoDataValue);
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private Band getSimulatedImageBand(final String srcBandName) {

        final String date = srcBandName.substring(srcBandName.lastIndexOf("_"));
        for (Band band:sourceProduct.getBands()) {
            if (band.getName().contains(SIMULATED_IMAGE) && band.getName().contains(date)) {
                return band;
            }
        }
        return null;
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(Map, Map)
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MultitemporalCompositingOp.class);
        }
    }

}
