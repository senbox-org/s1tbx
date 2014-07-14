/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.datamodel;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;

/**
 * A class for estimating the pixel position for a given geo-location. To be used with
 * pixel geo-codings in order to obtain a fast and accurate estimate.
 * <p>
 * This class does not belong to the public API.
 *
 * @author Ralf Quast
 * @since Version 5.0
 */
public class PixelPosEstimator {

    private static final boolean EXTRAPOLATE = true;

    private final GeoApproximation[] approximations;
    private final Rectangle bounds;


    public PixelPosEstimator(GeoApproximation[] approximations, Rectangle bounds) {
        this.approximations = approximations;
        this.bounds = bounds;
    }

    PixelPosEstimator(PlanarImage lonImage, PlanarImage latImage, PlanarImage maskImage, double accuracy) {
        this(lonImage, latImage, maskImage, accuracy, new DefaultSteppingFactory());
    }

    private PixelPosEstimator(PlanarImage lonImage, PlanarImage latImage, PlanarImage maskImage, double accuracy,
                              SteppingFactory steppingFactory) {
        approximations = createApproximations(lonImage, latImage, maskImage, accuracy, steppingFactory);
        bounds = lonImage.getBounds();
    }

    public final boolean canGetPixelPos() {
        return approximations != null;
    }

    /**
     * Returns an approximate pixel position for a given geographic position.
     *
     * @param g the geographic position.
     * @param p the pixel position.
     *
     * @return an approximate pixel position.
     */
    public GeoApproximation getPixelPos(GeoPos g, PixelPos p) {
        GeoApproximation approximation = null;
        if (approximations != null) {
            if (g.isValid()) {
                final double lat = g.getLat();
                final double lon = g.getLon();
                approximation = GeoApproximation.findMostSuitable(approximations, lat, lon);
                if (approximation != null) {
                    p.setLocation(lon, lat);
                    approximation.g2p(p);
                    final double x = p.getX();
                    final double y = p.getY();
                    if (!EXTRAPOLATE && (x < bounds.getMinX() || x > bounds.getMaxX() || y < bounds.getMinY() || y > bounds.getMaxY())) {
                        p.setInvalid();
                    }
                } else {
                    p.setInvalid();
                }
            } else {
                p.setInvalid();
            }
        }
        return approximation;
    }

    GeoApproximation getGeoPos(PixelPos p, GeoPos g) {
        GeoApproximation approximation = null;
        if (approximations != null) {
            if (g == null) {
                g = new GeoPos();
            }
            if (p.isValid()) {
                approximation = GeoApproximation.findSuitable(approximations, p);
                if (approximation != null) {
                    final double x = p.getX();
                    final double y = p.getY();
                    final Point2D q = new Point2D.Double(x, y);
                    approximation.p2g(q);
                    final double lon = q.getX();
                    final double lat = q.getY();
                    if (lon >= -180.0 && lon <= 180.0 && lat >= -90.0 && lat <= 90.0) {
                        g.setLocation((float) lat, (float) lon);
                    } else {
                        g.setInvalid();
                    }
                } else {
                    g.setInvalid();
                }
            } else {
                g.setInvalid();
            }
        }
        return approximation;
    }

    private static GeoApproximation[] createApproximations(PlanarImage lonImage,
                                                           PlanarImage latImage,
                                                           PlanarImage maskImage,
                                                           double accuracy,
                                                           SteppingFactory steppingFactory) {
        final SampleSource lonSamples = new PlanarImageSampleSource(lonImage);
        final SampleSource latSamples = new PlanarImageSampleSource(latImage);
        final SampleSource maskSamples;
        if (maskImage != null) {
            maskSamples = new PlanarImageSampleSource(maskImage);
        } else {
            maskSamples = new SampleSource() {
                @Override
                public int getSample(int x, int y) {
                    return 1;
                }

                @Override
                public double getSampleDouble(int x, int y) {
                    return 1.0;
                }
            };
        }
        final Raster[] tiles = lonImage.getTiles();
        final Rectangle[] rectangles = new Rectangle[tiles.length];
        for (int i = 0; i < rectangles.length; i++) {
            rectangles[i] = tiles[i].getBounds();
        }

        return GeoApproximation.createApproximations(lonSamples, latSamples, maskSamples, accuracy, rectangles,
                                                     steppingFactory);
    }


    static class PlanarImageSampleSource implements SampleSource {

        private final PlanarImage image;

        public PlanarImageSampleSource(PlanarImage image) {
            this.image = image;
        }

        @Override
        public int getSample(int x, int y) {
            return getSample(x, y, image);
        }

        @Override
        public double getSampleDouble(int x, int y) {
            return getSampleDouble(x, y, image);
        }

        private static int getSample(int pixelX, int pixelY, PlanarImage image) {
            final int x = image.getMinX() + pixelX;
            final int y = image.getMinY() + pixelY;
            final int tileX = image.XToTileX(x);
            final int tileY = image.YToTileY(y);
            final Raster data = image.getTile(tileX, tileY);

            return data.getSample(x, y, 0);
        }

        private static double getSampleDouble(int pixelX, int pixelY, PlanarImage image) {
            final int x = image.getMinX() + pixelX;
            final int y = image.getMinY() + pixelY;
            final int tileX = image.XToTileX(x);
            final int tileY = image.YToTileY(y);
            final Raster data = image.getTile(tileX, tileY);

            return data.getSampleDouble(x, y, 0);
        }
    }
}
