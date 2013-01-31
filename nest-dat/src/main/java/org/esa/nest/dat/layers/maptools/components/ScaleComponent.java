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
package org.esa.nest.dat.layers.maptools.components;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.nest.dat.layers.ScreenPixelConverter;
import org.esa.nest.eo.GeoUtils;

import java.awt.*;

/**
 * map tools scale component
 */
public class ScaleComponent implements MapToolsComponent {

    private final static int margin = 50;
    private final static int tick = 5;
    private final int width;
    private final int height;
    private PixelPos startPix = new PixelPos();
    private PixelPos endPix = new PixelPos();
    private final double[] pts, vpts;
    private BasicStroke stroke = new BasicStroke(2);

    public ScaleComponent(final RasterDataNode raster) {
        width = raster.getRasterWidth();
        height = raster.getRasterHeight();
        final GeoCoding geoCoding = raster.getGeoCoding();

        final int length = 100;
        startPix.setLocation(0 + margin, height - margin);
        endPix.setLocation(margin+length, height-margin);
        final GeoPos startGeo = geoCoding.getGeoPos(startPix, null);
        final GeoPos endGeo = geoCoding.getGeoPos(endPix, null);

        // get heading in x direction
        GeoUtils.DistanceHeading heading = GeoUtils.vincenty_inverse(startGeo.getLon(), startGeo.getLat(),
                                                                     endGeo.getLon(), endGeo.getLat());
        // get position for 1000m at that heading
        GeoUtils.LatLonHeading LatLon = GeoUtils.vincenty_direct(startGeo.getLon(), startGeo.getLat(),
                                                                 1000, heading.heading1);
        geoCoding.getPixelPos(new GeoPos((float)LatLon.lat, (float)LatLon.lon), endPix);

        pts = new double[] { startPix.getX(), startPix.getY(), endPix.getX(), endPix.getY(),    //line
                startPix.getX(), startPix.getY()-tick-tick,
                endPix.getX(), endPix.getY()-tick-tick};                                                   //ticks
        vpts = new double[pts.length];
    }

    public void render(final Graphics2D graphics, final ScreenPixelConverter screenPixel) {

        screenPixel.pixelToScreen(pts, vpts);

        graphics.setStroke(stroke);
        graphics.setColor(Color.YELLOW);
        graphics.drawLine((int)vpts[0], (int)vpts[1], (int)vpts[2], (int)vpts[3]);

        //ticks
        graphics.drawLine((int)vpts[0], (int)vpts[1], (int)vpts[4], (int)vpts[5]);
        graphics.drawLine((int)vpts[2], (int)vpts[3], (int)vpts[6], (int)vpts[7]);
    }
}
