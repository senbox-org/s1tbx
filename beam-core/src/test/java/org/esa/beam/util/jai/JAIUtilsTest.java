package org.esa.beam.util.jai;

import junit.framework.TestCase;

import java.awt.Dimension;

public class JAIUtilsTest extends TestCase {
    public void testPreferredTileSizeProperty() {
        // "small" images
        assertEquals(new Dimension(20, 10), JAIUtils.computePreferredTileSize(20, 10));
        assertEquals(new Dimension(256, 120), JAIUtils.computePreferredTileSize(256, 120));
        assertEquals(new Dimension(500, 500), JAIUtils.computePreferredTileSize(500, 500));
        assertEquals(new Dimension(600, 500), JAIUtils.computePreferredTileSize(600, 500));
        assertEquals(new Dimension(350, 500), JAIUtils.computePreferredTileSize(700, 500));
        assertEquals(new Dimension(430, 500), JAIUtils.computePreferredTileSize(860, 500));

        // "large" images
        assertEquals(new Dimension(512, 512), JAIUtils.computePreferredTileSize(1024, 1024));
        assertEquals(new Dimension(512, 512), JAIUtils.computePreferredTileSize(2048, 4096));
        assertEquals(new Dimension(640, 625), JAIUtils.computePreferredTileSize(12800, 25000));

        // "large" aspect ratio images
        assertEquals(new Dimension(512, 10), JAIUtils.computePreferredTileSize(1024, 10));
        assertEquals(new Dimension(10, 512), JAIUtils.computePreferredTileSize(10, 4096));
        assertEquals(new Dimension(561, 485), JAIUtils.computePreferredTileSize(1121, 970));
        assertEquals(new Dimension(380, 561), JAIUtils.computePreferredTileSize(760, 1121));

        // MERIS RR
        assertEquals(new Dimension(561, 561), JAIUtils.computePreferredTileSize(1121, 1121));
        assertEquals(new Dimension(561, 561), JAIUtils.computePreferredTileSize(1121, (1121-1) * 2 + 1));
        assertEquals(new Dimension(561, 561), JAIUtils.computePreferredTileSize(1121, (1121-1) * 3 + 1));
        assertEquals(new Dimension(561, 572), JAIUtils.computePreferredTileSize(1121, 14300));

        // MERIS FR
        assertEquals(new Dimension(561, 561), JAIUtils.computePreferredTileSize(2241, 2241));
        assertEquals(new Dimension(561, 498), JAIUtils.computePreferredTileSize(2241, (2241-1) * 2 + 1));
        assertEquals(new Dimension(561, 611), JAIUtils.computePreferredTileSize(2241, (2241-1) * 3 + 1));

        // MERIS FRS
        assertEquals(new Dimension(498, 498), JAIUtils.computePreferredTileSize(4481, 4481));
        assertEquals(new Dimension(498, 309), JAIUtils.computePreferredTileSize(4481, (4481-1) * 2 + 1));
        assertEquals(new Dimension(498, 611), JAIUtils.computePreferredTileSize(4481, (4481-1) * 3 + 1));
    }


}
