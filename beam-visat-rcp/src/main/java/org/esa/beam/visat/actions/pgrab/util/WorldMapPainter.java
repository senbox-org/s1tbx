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
package org.esa.beam.visat.actions.pgrab.util;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

public class WorldMapPainter {

    private static final Color _fillColor = new Color(255, 200, 200, 70);
    private Image _worldMapImage;
    private int _width;
    private int _height;
    private GeoCoding _geoCoding;

    public WorldMapPainter(final Image worldMapImage) {
        setWorldMapImage(worldMapImage);
    }

    public WorldMapPainter() {

    }

    public final void setWorldMapImage(final Image worldMapImage) {
        Guardian.assertNotNull("worldMapImage", worldMapImage);
        _worldMapImage = worldMapImage;
        _width = worldMapImage.getWidth(null);
        _height = worldMapImage.getHeight(null);
        if (_width != _height * 2) {
            throw new IllegalArgumentException("The world map image mut have the aspect ratio of 'width = 2 * height'");
        }
        _geoCoding = createGeocoding();
    }

    public BufferedImage createWorldMapImage(final GeneralPath[] geoBoundaryPaths) {
        final BufferedImage image = new BufferedImage(_width, _height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.drawImage(_worldMapImage, 0, 0, null);

        if (geoBoundaryPaths != null) {
            for (GeneralPath geoBoundaryPath : geoBoundaryPaths) {
                final GeneralPath boundaryPath = ProductUtils.convertToPixelPath(geoBoundaryPath, _geoCoding);
                g2d.setColor(_fillColor);
                g2d.fill(boundaryPath);
                g2d.setColor(Color.red);
                g2d.draw(boundaryPath);
            }
        }
        return image;
    }

    public GeoCoding getGeoCoding() {
        return _geoCoding;
    }

    private GeoCoding createGeocoding() {
        return new GeoCoding() {

            @Override
            public boolean isCrossingMeridianAt180() {
                return false;
            }

            @Override
            public CoordinateReferenceSystem getImageCRS() {
                return null;
            }

            @Override
            public boolean canGetPixelPos() {
                return true;
            }

            @Override
            public boolean canGetGeoPos() {
                return false;
            }

            @Override
            public PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos) {
                if (pixelPos == null) {
                    pixelPos = new PixelPos();
                }
                pixelPos.setLocation(_width / 360.0f * (geoPos.getLon() + 180.0f),
                                     _height - (_height / 180.0f * (geoPos.getLat() + 90.0f)));
                return pixelPos;
            }

            @Override
            public GeoPos getGeoPos(final PixelPos pixelPos, final GeoPos geoPos) {
                return null;
            }

            @Override
            public Datum getDatum() {
                return Datum.WGS_84;
            }

            @Override
            public void dispose() {
            }

            @Override
            public CoordinateReferenceSystem getMapCRS() {
                return null;
            }

            @Override
            public CoordinateReferenceSystem getGeoCRS() {
                return null;
            }

            @Override
            public MathTransform getImageToMapTransform() {
                return null;
            }
        };
    }
}
