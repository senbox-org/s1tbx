package org.esa.beam.framework.datamodel;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.util.jai.SingleBandedSampleModel;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.SourcelessOpImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PixelPosEstimatorTest {

    @BeforeClass
    public static void init() throws Exception {
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(134217728);
    }

    @Test
    @Ignore
    public void testGetPixelPosForSimulatedSwath() {
        final int nx = 512;
        final int ny = 36000;

        final PlanarImage[] images = generateSwathCoordinates(nx, ny, 0.009, 0.009, new Rotator(0.0, -5.0, 269.0));
        final PlanarImage lonImage = images[0];
        final PlanarImage latImage = images[1];
        final PlanarImage maskImage = images[2];
        final PixelPosEstimator estimator = new PixelPosEstimator(lonImage, latImage, maskImage, 0.5);

        final GeoPos g = new GeoPos();
        final PixelPos p = new PixelPos();

        for (int y = 0; y < ny; y++) {
            final Rectangle region = new Rectangle(0, y, nx, 1);
            final Raster lonData = lonImage.getData(region);
            final Raster latData = latImage.getData(region);
            for (int x = 0; x < nx; x++) {
                final float lon = lonData.getSampleFloat(x, y, 0);
                final float lat = latData.getSampleFloat(x, y, 0);
                g.setLocation(lat, lon);
                estimator.getPixelPos(g, p);

                assertTrue(p.isValid());
                assertEquals(x + 0.5, p.getX(), 0.5);
                assertEquals(y + 0.5, p.getY(), 0.5);
            }
        }
    }

    @Test
    public void testSingleApproximation() {
        final int nx = 512;
        final int ny = 512;
        final PlanarImage[] images = generateSwathCoordinates(nx, ny, 0.009, 0.009, new Rotator(0.0, 0.0, 265.0));

        final PlanarImage lonImage = images[0];
        final PlanarImage latImage = images[1];
        final PlanarImage maskImage = images[2];
        final Raster lonData = lonImage.getData();
        final Raster latData = latImage.getData();

        final Rectangle rectangle = new Rectangle(0, 0, 512, 512);
        final Stepping stepping = new DefaultSteppingFactory().createStepping(
                rectangle, 1000);

        final double[][] data = GeoApproximation.extractWarpPoints(
                new PixelPosEstimator.PlanarImageSampleSource(lonImage),
                new PixelPosEstimator.PlanarImageSampleSource(latImage),
                new PixelPosEstimator.PlanarImageSampleSource(maskImage),
                stepping);
        final GeoApproximation a = GeoApproximation.create(data, 0.5, rectangle);
        final RationalFunctionModel fx = a.getFX();
        final RationalFunctionModel fy = a.getFY();

        final double fxRmse = fx.getRmse();
        final double fyRmse = fy.getRmse();
        assertTrue(0.05 > fxRmse);
        assertTrue(0.05 > fyRmse);

        final Rotator rotator = a.getRotator();
        final Point2D p = new Point2D.Double();
        for (int y = 0; y < ny; y++) {
            for (int x = 0; x < nx; x++) {
                double lon = lonData.getSampleDouble(x, y, 0);
                double lat = latData.getSampleDouble(x, y, 0);

                p.setLocation(lon, lat);
                rotator.transform(p);

                lon = p.getX();
                lat = p.getY();

                assertEquals(x + 0.5, fx.getValue(lat, lon), 0.5);
                assertEquals(y + 0.5, fy.getValue(lat, lon), 0.5);
            }
        }
    }

    @Test
    public void testStepping() {
        final Rectangle rectangle = new Rectangle(0, 0, 512, 512);
        final Stepping stepping = new DefaultSteppingFactory().createStepping(
                rectangle, 1000);

        assertEquals(0, stepping.getMinX());
        assertEquals(0, stepping.getMinY());
        assertEquals(511, stepping.getMaxX());
        assertEquals(511, stepping.getMaxY());
        assertEquals(32, stepping.getPointCountX());
        assertEquals(32, stepping.getPointCountY());
        assertEquals(17, stepping.getStepX());
        assertEquals(17, stepping.getStepY());
        assertEquals(510, stepping.getMinX() + stepping.getStepX() * (stepping.getPointCountX() - 2));
        assertEquals(527, stepping.getMinX() + stepping.getStepX() * (stepping.getPointCountX() - 1));
        assertEquals(510, stepping.getMinY() + stepping.getStepX() * (stepping.getPointCountY() - 2));
        assertEquals(527, stepping.getMinY() + stepping.getStepY() * (stepping.getPointCountY() - 1));
    }

    @Test
    public void testExtractWarpPoints() {
        final int nx = 512;
        final int ny = 512;
        final PlanarImage[] images = generateSwathCoordinates(nx, ny, 0.009, 0.009, new Rotator(0.0, 0.0, 265.0));

        final PlanarImage lonImage = images[0];
        final PlanarImage latImage = images[1];
        final PlanarImage maskImage = images[2];
        final Raster lonData = lonImage.getData();
        final Raster latData = latImage.getData();

        assertEquals(0.0, lonData.getSampleDouble(nx / 2, ny / 2, 0), 0.0);
        assertEquals(0.0, latData.getSampleDouble(nx / 2, ny / 2, 0), 0.0);

        final Rectangle rectangle = new Rectangle(0, 0, 512, 512);
        final Stepping stepping = new DefaultSteppingFactory().createStepping(
                rectangle, 1000);

        final double[][] data = GeoApproximation.extractWarpPoints(
                new PixelPosEstimator.PlanarImageSampleSource(lonImage),
                new PixelPosEstimator.PlanarImageSampleSource(latImage),
                new PixelPosEstimator.PlanarImageSampleSource(maskImage),
                stepping);
        assertEquals(stepping.getPointCount(), data.length);

        final double[] upperLeft = data[0];
        assertEquals(0.5, upperLeft[2], 0.0);
        assertEquals(0.5, upperLeft[3], 0.0);

        final double[] eastOfUpperRight = data[stepping.getPointCountX() - 2];
        assertEquals(510.5, eastOfUpperRight[2], 0.0);
        assertEquals(0.5, eastOfUpperRight[3], 0.0);

        final double[] upperRight = data[stepping.getPointCountX() - 1];
        assertEquals(511.5, upperRight[2], 0.0);
        assertEquals(0.5, upperRight[3], 0.0);

        final double[] northOfLowerRight = data[data.length - 1 - stepping.getPointCountY()];
        assertEquals(511.5, northOfLowerRight[2], 0.0);
        assertEquals(510.5, northOfLowerRight[3], 0.0);

        final double[] lowerRight = data[data.length - 1];
        assertEquals(511.5, lowerRight[2], 0.0);
        assertEquals(511.5, lowerRight[3], 0.0);
    }

    private static PlanarImage[] generateSwathCoordinates(int nx, int ny,
                                                          final double lonResolution,
                                                          final double latResolution,
                                                          Rotator rotator) {
        final RenderingHints configuration = new RenderingHints(JAI.KEY_TILE_CACHE,
                                                                JAI.getDefaultInstance().getTileCache());
        final OpImage lonImage = new CoordinateOpImage(nx, ny, lonResolution, latResolution, rotator, configuration) {

            @Override
            protected final double getCoordinate(Point2D p) {
                return p.getX();
            }
        };

        final OpImage latImage = new CoordinateOpImage(nx, ny, lonResolution, latResolution, rotator, configuration) {

            @Override
            protected final double getCoordinate(Point2D p) {
                return p.getY();
            }
        };

        final RenderedOp maskImage = ConstantDescriptor.create((float) lonImage.getWidth(),
                                                               (float) lonImage.getHeight(),
                                                               new Byte[]{1}, configuration);

        return new PlanarImage[]{lonImage, latImage, maskImage};
    }

    private static abstract class CoordinateOpImage extends SourcelessOpImage {

        private final double minLon;
        private final double latResolution;
        private final double minLat;
        private final double lonResolution;
        private final Rotator rotator;

        public CoordinateOpImage(int nx, int ny, double lonResolution, double latResolution, Rotator rotator,
                                 RenderingHints configuration) {
            super(new ImageLayout(0, 0, nx, ny), configuration,
                  new SingleBandedSampleModel(DataBuffer.TYPE_DOUBLE, nx, ny),
                  0, 0, nx, ny);
            this.latResolution = latResolution;
            this.lonResolution = lonResolution;
            this.minLon = -ny * latResolution * 0.5;
            this.minLat = -nx * lonResolution * 0.5;
            this.rotator = rotator;
        }

        @Override
        protected final void computeRect(PlanarImage[] sources, WritableRaster target, Rectangle targetRectangle) {
            final Point2D p = new Point2D.Double();
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    final double lon = minLon + y * latResolution;
                    final double lat = minLat + x * lonResolution;
                    p.setLocation(lon, lat);
                    rotator.transform(p);
                    target.setSample(x, y, 0, getCoordinate(p));
                }
            }
        }

        protected abstract double getCoordinate(Point2D p);
    }

}
