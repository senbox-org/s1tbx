/*
 * $Id: WorldMapPainter.java,v 1.1 2007/04/19 10:28:50 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.plugins.pgrab.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

public class WorldMapPainter {

    private final static  Color _fillColor = new Color(255, 200, 200, 70);
    private Image _worldMapImage;
    private int _width;
    private int _height;
    private GeoCoding _geoCoding;

    public WorldMapPainter(final Image worldMapImage) {
        setWorldMapImage(worldMapImage);
    }

    public WorldMapPainter(){

    }

    public void setWorldMapImage(final Image worldMapImage) {
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
            for (int i = 0; i < geoBoundaryPaths.length; i++) {
                final GeneralPath boundaryPath = ProductUtils.convertToPixelPath(geoBoundaryPaths[i], _geoCoding);
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
            public boolean isCrossingMeridianAt180() {
                return false;
            }

            public boolean canGetPixelPos() {
                return true;
            }

            public boolean canGetGeoPos() {
                return false;
            }

            public PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos) {
                if (pixelPos == null) {
                    pixelPos = new PixelPos();
                }
                pixelPos.setLocation(_width / 360f * (geoPos.getLon() + 180f),
                                 _height - (_height / 180f * (geoPos.getLat() + 90f)));
                return pixelPos;
            }

            public GeoPos getGeoPos(final PixelPos pixelPos, final GeoPos geoPos) {
                return null;
            }

            public Datum getDatum() {
                return Datum.WGS_84;
            }

            public void dispose() {
            }
        };
    }
}
