/*
 * $id$
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.datamodel;

import java.awt.Color;

import junit.framework.TestCase;


public class ImageInfoTest extends TestCase {
    public void testConstructors() {
        ImageInfo imageInfo = new ImageInfo(0.0f, 10.0f, null);
        assertEquals(256, imageInfo.getNumColors());
        assertEquals(256, imageInfo.getColorPaletteDef().getNumColors());

        imageInfo = new ImageInfo(0.0f, 10.0f, null, 16, new ColorPaletteDef.Point[] {
                new ColorPaletteDef.Point(100, Color.ORANGE),
                new ColorPaletteDef.Point(200, Color.MAGENTA),
                new ColorPaletteDef.Point(500, Color.BLUE),
                new ColorPaletteDef.Point(600, Color.WHITE)
        });
        assertEquals(16, imageInfo.getNumColors());
        assertEquals(16, imageInfo.getColorPaletteDef().getNumColors());

        imageInfo.getColorPaletteDef().setNumColors(4);
        assertEquals(4, imageInfo.getNumColors());
        assertEquals(4, imageInfo.getColorPaletteDef().getNumColors());
    }
}