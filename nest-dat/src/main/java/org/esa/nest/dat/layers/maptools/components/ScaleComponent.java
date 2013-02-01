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
import org.esa.nest.dat.layers.GraphicsUtils;
import org.esa.nest.dat.layers.ScreenPixelConverter;
import org.esa.nest.eo.GeoUtils;

import java.awt.*;

/**
 * map tools scale component
 */
public class ScaleComponent implements MapToolsComponent {

    private final static double marginPct = 0.05;
    private final int margin;
    private final static int tick = 5;
    private final static int h = 3;
    private final double[] pts, vpts;
    private BasicStroke stroke = new BasicStroke(1);

    public ScaleComponent(final RasterDataNode raster) {
        final int width = raster.getRasterWidth();
        final int height = raster.getRasterHeight();
        final GeoCoding geoCoding = raster.getGeoCoding();

        margin = (int)(width * marginPct);
        final int length = 100;
        final PixelPos startPix = new PixelPos(0 + margin, height - margin);
        final PixelPos endPix = new PixelPos(margin+length, height-margin);
        final GeoPos startGeo = geoCoding.getGeoPos(startPix, null);
        final GeoPos endGeo = geoCoding.getGeoPos(endPix, null);

        // get heading in x direction
        GeoUtils.DistanceHeading heading = GeoUtils.vincenty_inverse(startGeo.getLon(), startGeo.getLat(),
                                                                     endGeo.getLon(), endGeo.getLat());
        // get position for 1000m at that heading
        GeoUtils.LatLonHeading LatLon = GeoUtils.vincenty_direct(startGeo.getLon(), startGeo.getLat(),
                1000, heading.heading1);
        final PixelPos pix1K = geoCoding.getPixelPos(new GeoPos((float)LatLon.lat, (float)LatLon.lon), null);
        LatLon = GeoUtils.vincenty_direct(startGeo.getLon(), startGeo.getLat(),
                2000, heading.heading1);
        final PixelPos pix2K = geoCoding.getPixelPos(new GeoPos((float)LatLon.lat, (float)LatLon.lon), null);
        LatLon = GeoUtils.vincenty_direct(startGeo.getLon(), startGeo.getLat(),
                3000, heading.heading1);
        final PixelPos pix3K = geoCoding.getPixelPos(new GeoPos((float)LatLon.lat, (float)LatLon.lon), null);
        LatLon = GeoUtils.vincenty_direct(startGeo.getLon(), startGeo.getLat(),
                4000, heading.heading1);
        final PixelPos pix4K = geoCoding.getPixelPos(new GeoPos((float)LatLon.lat, (float)LatLon.lon), null);
        LatLon = GeoUtils.vincenty_direct(startGeo.getLon(), startGeo.getLat(),
                5000, heading.heading1);
        final PixelPos pix5K = geoCoding.getPixelPos(new GeoPos((float)LatLon.lat, (float)LatLon.lon), null);
        LatLon = GeoUtils.vincenty_direct(startGeo.getLon(), startGeo.getLat(),
                10000, heading.heading1);
        final PixelPos pix10K = geoCoding.getPixelPos(new GeoPos((float)LatLon.lat, (float)LatLon.lon), null);

        pts = new double[] { startPix.getX(), startPix.getY(), pix10K.getX(), pix10K.getY(),    //line
                pix1K.getX(), pix1K.getY(), pix2K.getX(), pix2K.getY(), pix3K.getX(), pix3K.getY(),
                pix4K.getX(), pix4K.getY(), pix5K.getX(), pix5K.getY()};
        vpts = new double[pts.length];
    }

    public void render(final Graphics2D g, final ScreenPixelConverter screenPixel) {

        screenPixel.pixelToScreen(pts, vpts);
        final Point[] pt = ScreenPixelConverter.arrayToPoints(vpts);

        final int y = pt[0].y;

        g.setStroke(stroke);
        g.setColor(Color.YELLOW);

        //ticks
        g.drawLine(pt[0].x, y-h, pt[0].x, y-h-tick); // 0
        g.drawLine(pt[1].x, y-h, pt[1].x, y-h-tick); // 10
        g.drawLine(pt[2].x, y-h, pt[2].x, y-h-tick); // 1
        g.drawLine(pt[6].x, y-h, pt[6].x, y-h-tick); // 5

        //labels
        GraphicsUtils.outlineText(g, Color.YELLOW, "10km", pt[1].x+2, y-h-tick);
        GraphicsUtils.outlineText(g, Color.YELLOW, "1km", pt[2].x+2, y-h-tick);
        GraphicsUtils.outlineText(g, Color.YELLOW, "5km", pt[6].x+2, y-h-tick);

        //fill rects
        g.setColor(Color.BLACK);
        g.fillRect(pt[0].x, y-h, pt[2].x-pt[0].x, h);
        g.fillRect(pt[3].x, y-h, pt[4].x-pt[3].x, h);
        g.fillRect(pt[5].x, y-h, pt[6].x-pt[5].x, h);

        g.setColor(Color.WHITE);
        g.fillRect(pt[2].x, y-h, pt[3].x-pt[2].x, h);
        g.fillRect(pt[4].x, y-h, pt[5].x-pt[4].x, h);
        g.fillRect(pt[6].x, y-h, pt[1].x-pt[6].x, h);

        g.setColor(Color.YELLOW);
        g.drawRect(pt[0].x, y-h, pt[1].x-pt[0].x, h);
    }
}
