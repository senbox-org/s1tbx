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

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.nest.dat.layers.GraphicsUtils;
import org.esa.nest.dat.layers.ScreenPixelConverter;
import org.esa.nest.datamodel.AbstractMetadata;

import java.awt.*;
import java.util.ArrayList;

/**
 * map tools scale component
 */
public class InfoComponent implements MapToolsComponent {

    private final double[] pts, vpts;
    private final ArrayList<String> infoList = new ArrayList<String>();
    private int maxLength;
    private static final Font font = new Font("Arial", Font.PLAIN, 12);

    public InfoComponent(final RasterDataNode raster) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(raster.getProduct());
        final int rasterWidth = raster.getRasterWidth();
        final int startPosX = rasterWidth;
        final int startPosY = 100;

        pts = new double[] { startPosX, startPosY };
        vpts = new double[pts.length];

        infoList.add(absRoot.getAttributeString(AbstractMetadata.PRODUCT));
        infoList.add(absRoot.getAttributeString(AbstractMetadata.first_line_time));
        infoList.add(absRoot.getAttributeString(AbstractMetadata.MISSION)+' '+
                absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE)+' '+
                absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE));

        for(String str : infoList) {
            if(str.length() > maxLength)
                maxLength = str.length();
        }
    }

    public void render(final Graphics2D g, final ScreenPixelConverter screenPixel) {
        if(pts == null)
            return;

        screenPixel.pixelToScreen(pts, vpts);
        final Point[] pt = ScreenPixelConverter.arrayToPoints(vpts);

        g.setFont(font);
        int w = g.getFontMetrics().charWidth('B');
        int h = g.getFontMetrics().getHeight();

        final int x = pt[0].x - (w*(maxLength+5));
        int y = pt[0].y;

        for(String str : infoList) {
            GraphicsUtils.outlineText(g, Color.YELLOW, str, x, y);
            y+=h;
        }
    }
}
