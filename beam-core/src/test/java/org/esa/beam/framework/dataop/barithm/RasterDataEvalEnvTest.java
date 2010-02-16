/*
 * $Id: RasterDataEvalEnvTest.java,v 1.2 2007/01/26 09:09:02 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataop.barithm;

import junit.framework.TestCase;

public class RasterDataEvalEnvTest extends TestCase {

    public void testDefaultConstructor() {
        final RasterDataEvalEnv env = new RasterDataEvalEnv(0, 0, 1, 1);
        assertEquals(0, env.getPixelX());
        assertEquals(0, env.getPixelY());
        assertEquals(0, env.getElemIndex());
        assertEquals(0, env.getOffsetX());
        assertEquals(0, env.getOffsetY());
        assertEquals(1, env.getRegionWidth());
        assertEquals(1, env.getRegionHeight());
    }

    public void testConstructor() {
        final RasterDataEvalEnv env = new RasterDataEvalEnv(20, 14, 238, 548);
        assertEquals(20, env.getPixelX());
        assertEquals(14, env.getPixelY());
        assertEquals(0, env.getElemIndex());
        assertEquals(20, env.getOffsetX());
        assertEquals(14, env.getOffsetY());
        assertEquals(238, env.getRegionWidth());
        assertEquals(548, env.getRegionHeight());
    }

    public void testXY() {
        final RasterDataEvalEnv env = new RasterDataEvalEnv(50, 20, 200, 100);

        assertEquals(0, env.getElemIndex());
        assertEquals(50, env.getPixelX());
        assertEquals(20, env.getPixelY());

        env.setElemIndex(1);
        assertEquals(1, env.getElemIndex());
        assertEquals(50 + 1, env.getPixelX());
        assertEquals(20, env.getPixelY());

        env.setElemIndex(200 + 2);
        assertEquals(200 + 2, env.getElemIndex());
        assertEquals(50 + 2, env.getPixelX());
        assertEquals(20 + 1, env.getPixelY());

        env.setElemIndex(70 * 200 + 110);
        assertEquals(70 * 200 + 110, env.getElemIndex());
        assertEquals(50 + 110, env.getPixelX());
        assertEquals(20 + 70, env.getPixelY());
    }
}
