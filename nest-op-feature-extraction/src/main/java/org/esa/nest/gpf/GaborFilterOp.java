/*
 * Copyright (C) 2011 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.TileIndex;

import java.awt.*;
import java.util.HashMap;

/**
 */
@OperatorMetadata(alias="GaborFilter",
                  category = "Classification\\Primitive Features",
                  authors = "Jun Lu, Luis Veci",
                  copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
                  description="Extract Texture Features")
public class GaborFilterOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private String[] sourceBandNames;

    @Parameter
    private double theta = 0.6;

    private final HashMap<String, String[]> targetBandNameToSourceBandName = new HashMap<String, String[]>();

    double[][] filter;

    /**
	     * Default constructor. The graph processing framework
	     * requires that an operator has a default constructor.
	 */
    public GaborFilterOp() {
    }

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

            getSourceMetadata();

            // create target product
            targetProduct = new Product(sourceProduct.getName(),
                                        sourceProduct.getProductType(),
                                        sourceProduct.getSceneRasterWidth(),
                                        sourceProduct.getSceneRasterHeight());

            OperatorUtils.addSelectedBands(
                    sourceProduct, sourceBandNames, targetProduct, targetBandNameToSourceBandName, false, true);

            OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

            // update the metadata with the affect of the processing
            updateTargetProductMetadata();

            filter = GaborFilter.createGarborFilter(4.0, theta, 1.0, 2.0, 0.3);
    }

    /**
     * Compute source metadata
     */
    private void getSourceMetadata() {

    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

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

        final int x0 = targetTile.getRectangle().x;
        final int y0 = targetTile.getRectangle().y;
        final int w = targetTile.getRectangle().width;
        final int h = targetTile.getRectangle().height;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final int xmax=(int)Math.floor(filter.length/2.0);
        final int ymax=(int)Math.floor(filter[0].length/2.0);
        final String[] srcBandName = targetBandNameToSourceBandName.get(targetBand.getName());
        final Band srcBand = sourceProduct.getBand(srcBandName[0]);
        final int minBoundx = Math.max(0, x0-xmax);
        final int minBoundy = Math.max(0, y0-ymax);
        final int boundW = Math.min(w+minBoundx+xmax, srcBand.getSceneRasterWidth()-x0);
        final int boundH = Math.min(h+minBoundy+ymax, srcBand.getSceneRasterHeight()-y0);
        final Rectangle srcRect = new Rectangle(minBoundx, minBoundy, boundW, boundH);

        final Tile sourceTile = getSourceTile(srcBand, srcRect);
        final ProductData trgData = targetTile.getDataBuffer();
        final ProductData srcData = sourceTile.getDataBuffer();
        final TileIndex trgIndex = new TileIndex(targetTile);
        final TileIndex srcIndex = new TileIndex(sourceTile);
        int maxIndex = srcData.getNumElems();

        for (int y = y0; y < y0 + h; y++) {
            trgIndex.calculateStride(y);
            for (int x = x0; x < x0 + w; x++) {

                double sum=0;
                for (int yf=-ymax;yf<=ymax;yf++){
                    final int yy = y-yf;
                    if(yy>=minBoundy && yy < y0 + boundH) {
                        srcIndex.calculateStride(yy);
                        for (int xf=-xmax;xf<=xmax;xf++){
                            final int xx = x-xf;
                            if (xx>=minBoundx && xx < x0 + boundW) {
                                final int idx = srcIndex.getIndex(xx);
                                if(idx < maxIndex)   //todo something not right here
                                    sum+=filter[xf+xmax][yf+ymax] * srcData.getElemDoubleAt(idx);
                            }
                        }
                    }
                }     
                trgData.setElemDoubleAt(trgIndex.getIndex(x), sum);
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
            super(GaborFilterOp.class);
        }
    }
}
