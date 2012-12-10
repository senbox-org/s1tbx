package com.bc.ceres.glevel.support;

import junit.framework.TestCase;

import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * @author Norman Fomferra
 */
public class AbstractMultiLevelSourceTest extends TestCase {
    public void testImageDimension() throws Exception {
        assertEquals(new Dimension(1024, 512), AbstractMultiLevelSource.getImageDimension(512, 256, 0.5));
        assertEquals(new Dimension(768, 994), AbstractMultiLevelSource.getImageDimension(384, 497, 0.5));
        assertEquals(new Dimension(192, 248), AbstractMultiLevelSource.getImageDimension(384, 497, 2.0));
    }

    public void testImageRectangle() throws Exception {
        assertEquals(new Rectangle(20, 40, 1024, 512), AbstractMultiLevelSource.getImageRectangle(10, 20, 512, 256, 0.5));
        assertEquals(new Rectangle(2000, -120, 768, 994), AbstractMultiLevelSource.getImageRectangle(1000, -60, 384, 497, 0.5));
        assertEquals(new Rectangle(0, 248, 192, 249), AbstractMultiLevelSource.getImageRectangle(0, 497, 384, 497, 2.0));
    }
}
