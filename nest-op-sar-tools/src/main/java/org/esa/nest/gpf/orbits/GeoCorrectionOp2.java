/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf.orbits;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * This operator first fills holes in the latitude and longitude bands of the source product, then creates
 * a pixel geocoding using the latitude and longitude bands for the target product.
 */

@OperatorMetadata(alias = "Geo-Correction-2",
        category = "Geometric\\Geo Correction",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Geo Correction 2")
public final class GeoCorrectionOp2 extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private Band srcLatBand = null;
    private Band srcLonBand = null;

    public static String LATITUDE_BAND_NAME = "lat_band";
    public static String LONGITUDE_BAND_NAME = "lon_band";

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product}
     * annotated with the {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            getSourceImageDimension();

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
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
     * Create target product.
     *
     * @throws Exception The exceptions.
     */
    private void createTargetProduct() throws Exception {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();
    }

    private void addSelectedBands() throws Exception {

        final Band[] sourceBands = sourceProduct.getBands();
        boolean hasLatBand = false;
        boolean hasLonBand = false;
        for (Band band : sourceBands) {
            if (band.getName().equals(GeoCorrectionOp1.LATITUDE_BAND_NAME)) {
                srcLatBand = band;
                hasLatBand = true;
                continue;
            }

            if (band.getName().equals(GeoCorrectionOp1.LONGITUDE_BAND_NAME)) {
                srcLonBand = band;
                hasLonBand = true;
                continue;
            }

            final Band targetBand = ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, false);
            targetBand.setSourceImage(band.getSourceImage());
        }

        if (!hasLatBand || !hasLonBand) {
            throw new OperatorException("Run GeoCorrectionOp1 first.");
        }

        Band latBand = new Band(LATITUDE_BAND_NAME, ProductData.TYPE_FLOAT32, sourceImageWidth, sourceImageHeight);
        Band lonBand = new Band(LONGITUDE_BAND_NAME, ProductData.TYPE_FLOAT32, sourceImageWidth, sourceImageHeight);
        targetProduct.addBand(latBand);
        targetProduct.addBand(lonBand);

        targetProduct.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 6));
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int ymax = y0 + h;
        final int xmax = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final Tile tgtLatTile = targetTiles.get(targetProduct.getBand(LATITUDE_BAND_NAME));
        final Tile tgtLonTile = targetTiles.get(targetProduct.getBand(LONGITUDE_BAND_NAME));
        final ProductData tgtLatData = tgtLatTile.getDataBuffer();
        final ProductData tgtLonData = tgtLonTile.getDataBuffer();
        final TileIndex trgIndex = new TileIndex(tgtLatTile);

        final Tile srcLatTile = getSourceTile(srcLatBand, targetRectangle);
        final Tile srcLonTile = getSourceTile(srcLonBand, targetRectangle);
        final ProductData srcLatData = srcLatTile.getDataBuffer();
        final ProductData srcLonData = srcLonTile.getDataBuffer();

        try {
            for (int y = y0; y < ymax; y++) {
                trgIndex.calculateStride(y);

                for (int x = x0; x < xmax; x++) {
                    final int index = trgIndex.getIndex(x);

                    final double srcLat = srcLatData.getElemDoubleAt(index);
                    final double srcLon = srcLonData.getElemDoubleAt(index);

                    if (srcLat == 0.0) {
                        tgtLatData.setElemFloatAt(index, (float) fillHole(x, y, srcLatBand));
                    } else {
                        tgtLatData.setElemFloatAt(index, (float) srcLat);
                    }

                    if (srcLon == 0.0) {
                        tgtLonData.setElemFloatAt(index, (float) fillHole(x, y, srcLonBand));
                    } else {
                        tgtLonData.setElemFloatAt(index, (float) srcLon);
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private double fillHole(final int x, final int y, final Band sourceBand) throws Exception {

        try {
            final int radius = Math.max(sourceImageWidth, sourceImageHeight);
            for (int i = 1; i <= radius; i++) {
                final int xSt = Math.max(x - i, 0);
                final int xEd = Math.min(x + i, sourceImageWidth - 1);
                final int ySt = Math.max(y - i, 0);
                final int yEd = Math.min(y + i, sourceImageHeight - 1);
                final int w = xEd - xSt + 1;
                final int h = yEd - ySt + 1;
                final int num = w * h;
                final double[] data = new double[num];

                sourceBand.getSourceImage().getData(new Rectangle(xSt, ySt, w, h)).getPixels(xSt, ySt, w, h, data);

                double v = 0.0;
                int k = 0;
                for (double d : data) {
                    if (d != 0) {
                        v += d;
                        k++;
                    }
                }

                if (k != 0) {
                    return v / k;
                }
            }
        } catch (Exception e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }

        return 0.0;
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(GeoCorrectionOp2.class);
        }
    }
}
