/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.gpf.operators.meris;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;

import java.awt.Rectangle;

@OperatorMetadata(alias = "L3ToL1", internal = true)
public class L3ToL1Op extends MerisBasisOp {

    private GeoCoding l3GeoCoding;
    private GeoCoding l1GeoCoding;

    @SourceProduct(alias = "l1")
    private Product l1Product;
    @SourceProduct(alias = "l3")
    private Product l3Product;
    @SourceProduct(alias = "mask", optional = true)
    private Product maskProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter
    private String maskBand;
    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME, ResamplingFactory.BILINEAR_INTERPOLATION_NAME, 
            ResamplingFactory.CUBIC_CONVOLUTION_NAME}, defaultValue = ResamplingFactory.NEAREST_NEIGHBOUR_NAME)
    private String resamplingType = ResamplingFactory.NEAREST_NEIGHBOUR_NAME;

    @Override
    public void initialize() throws OperatorException {
        l3GeoCoding = l3Product.getGeoCoding();
        l1GeoCoding = l1Product.getGeoCoding();
        targetProduct = createCompatibleProduct(l1Product, "l3tol1", "L3");

        Band[] l3Bands = l3Product.getBands();
        for (Band sourceBand : l3Bands) {
            Band targetBand = targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
            ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            targetBand.setDescription(sourceBand.getDescription());
            targetBand.setUnit(sourceBand.getUnit());
            targetBand.setScalingFactor(sourceBand.getScalingFactor());
            targetBand.setScalingOffset(sourceBand.getScalingOffset());
            targetBand.setLog10Scaled(sourceBand.isLog10Scaled());
            targetBand.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
            targetBand.setNoDataValue(sourceBand.getNoDataValue());
            if (sourceBand.getFlagCoding() != null) {
                FlagCoding srcFlagCoding = sourceBand.getFlagCoding();
                ProductUtils.copyFlagCoding(srcFlagCoding, targetProduct);
                targetBand.setSampleCoding(targetProduct.getFlagCodingGroup().get(srcFlagCoding.getName()));
            }
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        Rectangle rectangle = targetTile.getRectangle();
        Band srcBand = l3Product.getBand(band.getName());

        PixelPos l1PixelPos = new PixelPos();
        PixelPos l3PixelPos = new PixelPos();
        GeoPos geoPos = new GeoPos();

        Rectangle l3Rect = findL3Rectangle(rectangle, srcBand);
        Tile srcTile = getSourceTile(srcBand, l3Rect);

        Tile maskTile = null;
        boolean useMask = false;
        if (maskProduct != null && StringUtils.isNotNullAndNotEmpty(maskBand)) {
            maskTile = getSourceTile(maskProduct.getBand(maskBand), rectangle);
            useMask = true;
        }
        final Resampling resampling = Resampling.BILINEAR_INTERPOLATION;
        ResamplingFactory.createResampling(resamplingType);
        final Resampling.Index resamplingIndex = resampling.createIndex();
        final TileBasedResamplingRaster resamplingRaster = new TileBasedResamplingRaster(srcTile);
        
        pm.beginTask("compute", rectangle.height);
        try {
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                l1PixelPos.y = y;
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    if (!useMask || maskTile.getSampleBoolean(x, y)) {
                        l1PixelPos.x = x;
                        l1GeoCoding.getGeoPos(l1PixelPos, geoPos);
                        l3GeoCoding.getPixelPos(geoPos, l3PixelPos);
                        
                        resampling.computeIndex(l3PixelPos.x, l3PixelPos.y,
                                l3Product.getSceneRasterWidth(), l3Product.getSceneRasterHeight(),
                                resamplingIndex);
                        double sample = resampling.resample(resamplingRaster, resamplingIndex);
                        targetTile.setSample(x, y, sample);
                    }
                }
                checkForCancellation();
                pm.worked(1);
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        } finally {
            pm.done();
        }
    }

    private Rectangle findL3Rectangle(Rectangle l1Rectangle, Band srcBand) {
        PixelPos bottomLeft = new PixelPos(l1Rectangle.x, l1Rectangle.y);
        PixelPos l3PixelPos = l3GeoCoding.getPixelPos(l1GeoCoding.getGeoPos(bottomLeft, null), null);
        Rectangle l3Rectangle = new Rectangle(Math.round(l3PixelPos.x), Math.round(l3PixelPos.y), 1, 1);

        PixelPos bottomRight = new PixelPos(l1Rectangle.x + l1Rectangle.width, l1Rectangle.y);
        l3PixelPos = l3GeoCoding.getPixelPos(l1GeoCoding.getGeoPos(bottomRight, null), l3PixelPos);
        l3Rectangle.add(l3PixelPos.x, l3PixelPos.y);

        PixelPos topRight = new PixelPos(l1Rectangle.x + l1Rectangle.width, l1Rectangle.y + l1Rectangle.height);
        l3PixelPos = l3GeoCoding.getPixelPos(l1GeoCoding.getGeoPos(topRight, null), l3PixelPos);
        l3Rectangle.add(l3PixelPos.x, l3PixelPos.y);

        PixelPos topLeft = new PixelPos(l1Rectangle.x, l1Rectangle.y + l1Rectangle.height);
        l3PixelPos = l3GeoCoding.getPixelPos(l1GeoCoding.getGeoPos(topLeft, null), l3PixelPos);
        l3Rectangle.add(l3PixelPos.x, l3PixelPos.y);

        l3Rectangle.grow(2, 2);
        Rectangle sceneRectangle = new Rectangle(srcBand.getSceneRasterWidth(), srcBand.getSceneRasterHeight());
        return l3Rectangle.intersection(sceneRectangle);
    }
    
    private static class TileBasedResamplingRaster implements Resampling.Raster {

        private final Tile tile;

        public TileBasedResamplingRaster(Tile tile) {
            this.tile = tile;
        }

        public final int getWidth() {
            return tile.getWidth();
        }

        public final int getHeight() {
            return tile.getHeight();
        }

        public boolean getSamples(int[] x, int[] y, double[][] samples) {
            for (int i = 0; i < y.length; i++) {
                for (int j = 0; j < x.length; j++) {
                    samples[i][j] = tile.getSampleDouble(x[j], y[i]);
                }
            }
            return true;
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(L3ToL1Op.class);
        }
    }
}
