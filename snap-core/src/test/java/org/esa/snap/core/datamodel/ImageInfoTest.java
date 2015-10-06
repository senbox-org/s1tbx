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
package org.esa.snap.core.datamodel;

import junit.framework.TestCase;

import java.awt.Color;


public class ImageInfoTest extends TestCase {
    public void testImageInfo1Band() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(100, Color.ORANGE),
                new ColorPaletteDef.Point(200, Color.MAGENTA),
                new ColorPaletteDef.Point(500, Color.BLUE),
                new ColorPaletteDef.Point(600, Color.WHITE)
        });
        ImageInfo imageInfo = new ImageInfo(cpd);
        assertSame(cpd, imageInfo.getColorPaletteDef());
        assertEquals(null, imageInfo.getRgbChannelDef());
        assertEquals(4, imageInfo.getColorComponentCount());
        assertEquals(ImageInfo.NO_COLOR, imageInfo.getNoDataColor());
        assertEquals(ImageInfo.HistogramMatching.None, imageInfo.getHistogramMatching());
        assertNotNull(imageInfo.getColors());
        assertEquals(4, imageInfo.getColors().length);
        assertEquals(Color.ORANGE, imageInfo.getColors()[0]);
        assertEquals(Color.MAGENTA, imageInfo.getColors()[1]);
        assertEquals(Color.BLUE, imageInfo.getColors()[2]);
        assertEquals(Color.WHITE, imageInfo.getColors()[3]);

        imageInfo.setNoDataColor(Color.RED);
        assertEquals(Color.RED, imageInfo.getNoDataColor());
        assertEquals(3, imageInfo.getColorComponentCount());

        imageInfo.setHistogramMatching(ImageInfo.HistogramMatching.Equalize);
        assertEquals(ImageInfo.HistogramMatching.Equalize, imageInfo.getHistogramMatching());
    }

    public void testImageInfo3Bands() {
        final RGBChannelDef rgbcd = new RGBChannelDef(new String[]{
                "radiance_13", "radiance_5", "radiance_2"
        });
        ImageInfo imageInfo = new ImageInfo(rgbcd);
        assertEquals(null, imageInfo.getColorPaletteDef());
        assertSame(rgbcd, imageInfo.getRgbChannelDef());
        assertEquals(4, imageInfo.getColorComponentCount());

        imageInfo.setNoDataColor(Color.RED);
        assertEquals(Color.RED, imageInfo.getNoDataColor());
        assertEquals(3, imageInfo.getColorComponentCount());

        imageInfo.setHistogramMatching(ImageInfo.HistogramMatching.Normalize);
        assertEquals(ImageInfo.HistogramMatching.Normalize, imageInfo.getHistogramMatching());

        final RGBChannelDef rgbcdAlpha = new RGBChannelDef(new String[]{
                "radiance_13", "radiance_5", "radiance_2", "50.0"
        });
        imageInfo = new ImageInfo(rgbcdAlpha);
        assertEquals(4, imageInfo.getColorComponentCount());
        imageInfo.setNoDataColor(Color.RED);
        assertEquals(4, imageInfo.getColorComponentCount());
    }
}
