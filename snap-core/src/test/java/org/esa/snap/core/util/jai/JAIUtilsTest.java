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

package org.esa.snap.core.util.jai;

import org.esa.snap.core.util.IntMap;
import org.junit.Assert;
import org.junit.Test;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class JAIUtilsTest {

    @Test
    public void testIntMapOp() {

        testIntMapOp(BufferedImage.TYPE_USHORT_GRAY, new int[]{
                868, 393, 565, 101,
                393, 454, 868, 393,
                747, 191, 101, 393,
                393, 565, 191, 101
        }, DataBuffer.TYPE_BYTE, new int[]{
                0, 1, 2, 3,
                1, 4, 0, 1,
                5, 6, 3, 1,
                1, 2, 6, 3
        });

        testIntMapOp(BufferedImage.TYPE_BYTE_GRAY, new int[]{
                86, 39, 56, 10,
                39, 45, 86, 39,
                74, 19, 10, 39,
                39, 56, 19, 10
        }, DataBuffer.TYPE_BYTE, new int[]{
                0, 1, 2, 3,
                1, 4, 0, 1,
                5, 6, 3, 1,
                1, 2, 6, 3
        });
    }

    @Test
    public void testComputePreferredTileSize_WithDifferentGranularity() throws Exception {
        assertEquals(new Dimension(408, 424), JAIUtils.computePreferredTileSize(4481, 2113, 4));
        assertEquals(new Dimension(498, 302), JAIUtils.computePreferredTileSize(4481, 2113, 1));
    }

    @Test
    public void testPreferredTileSizeProperty() {
        // "small" images
        assertEquals(new Dimension(20, 10), JAIUtils.computePreferredTileSize(20, 10, 1));
        assertEquals(new Dimension(256, 120), JAIUtils.computePreferredTileSize(256, 120, 1));
        assertEquals(new Dimension(500, 500), JAIUtils.computePreferredTileSize(500, 500, 1));
        assertEquals(new Dimension(600, 500), JAIUtils.computePreferredTileSize(600, 500, 1));
        assertEquals(new Dimension(350, 500), JAIUtils.computePreferredTileSize(700, 500, 1));
        assertEquals(new Dimension(430, 500), JAIUtils.computePreferredTileSize(860, 500, 1));

        // "large" images
        assertEquals(new Dimension(512, 512), JAIUtils.computePreferredTileSize(1024, 1024, 1));
        assertEquals(new Dimension(512, 512), JAIUtils.computePreferredTileSize(2048, 4096, 1));
        assertEquals(new Dimension(640, 625), JAIUtils.computePreferredTileSize(12800, 25000, 1));

        // "large" aspect ratio images
        assertEquals(new Dimension(512, 10), JAIUtils.computePreferredTileSize(1024, 10, 1));
        assertEquals(new Dimension(10, 512), JAIUtils.computePreferredTileSize(10, 4096, 1));
        assertEquals(new Dimension(561, 485), JAIUtils.computePreferredTileSize(1121, 970, 1));
        assertEquals(new Dimension(380, 561), JAIUtils.computePreferredTileSize(760, 1121, 1));

        // MERIS RR
        assertEquals(new Dimension(561, 561), JAIUtils.computePreferredTileSize(1121, 1121, 1));
        assertEquals(new Dimension(561, 561), JAIUtils.computePreferredTileSize(1121, (1121 - 1) * 2 + 1, 1));
        assertEquals(new Dimension(561, 561), JAIUtils.computePreferredTileSize(1121, (1121 - 1) * 3 + 1, 1));
        assertEquals(new Dimension(561, 572), JAIUtils.computePreferredTileSize(1121, 14300, 1));

        // MERIS RR, granularity 4
        assertEquals(new Dimension(564, 564), JAIUtils.computePreferredTileSize(1121, 1121, 4));
        assertEquals(new Dimension(564, 564), JAIUtils.computePreferredTileSize(1121, (1121 - 1) * 2 + 1, 4));
        assertEquals(new Dimension(564, 260), JAIUtils.computePreferredTileSize(1121, (1121 - 1) * 3 + 1, 4));
        assertEquals(new Dimension(564, 572), JAIUtils.computePreferredTileSize(1121, 14300, 4));

        // MERIS FR
        assertEquals(new Dimension(561, 561), JAIUtils.computePreferredTileSize(2241, 2241, 1));
        assertEquals(new Dimension(561, 498), JAIUtils.computePreferredTileSize(2241, (2241 - 1) * 2 + 1, 1));
        assertEquals(new Dimension(561, 611), JAIUtils.computePreferredTileSize(2241, (2241 - 1) * 3 + 1, 1));

        // MERIS FRS
        assertEquals(new Dimension(498, 498), JAIUtils.computePreferredTileSize(4481, 4481, 1));
        assertEquals(new Dimension(498, 309), JAIUtils.computePreferredTileSize(4481, (4481 - 1) * 2 + 1, 1));
        assertEquals(new Dimension(498, 611), JAIUtils.computePreferredTileSize(4481, (4481 - 1) * 3 + 1, 1));

        // A NEST SAR Image
        assertEquals(new Dimension(624, 436), JAIUtils.computePreferredTileSize(5602, 5221, 4));
    }

    private void testIntMapOp(int sourceType, int[] sourceSamples, int expectedTargetType,
                              int[] expectedTargetSamples) {
        final BufferedImage sourceImage = createSourceImage(sourceType, sourceSamples);
        final IntMap intMap = createIntMap(sourceImage);
        final PlanarImage targetImage = JAIUtils.createIndexedImage(sourceImage, intMap, 0);
        Assert.assertNotNull(targetImage);
        assertEquals(1, targetImage.getNumBands());
        assertEquals(sourceImage.getWidth(), targetImage.getWidth());
        assertEquals(sourceImage.getHeight(), targetImage.getHeight());
        assertEquals(expectedTargetType, targetImage.getSampleModel().getDataType());
        final Raster targetData = targetImage.getData();
        final DataBuffer dataBuffer = targetData.getDataBuffer();
        for (int i = 0; i < expectedTargetSamples.length; i++) {
            final int elem = dataBuffer.getElem(i);
            assertEquals("i=" + i, expectedTargetSamples[i], elem);
        }
    }

    private static IntMap createIntMap(BufferedImage sourceImage) {
        final DataBuffer dataBuffer = sourceImage.getRaster().getDataBuffer();
        final Set<Integer> set = new HashSet<Integer>();
        final IntMap intMap = new IntMap(0, 1000);
        for (int i = 0; i < dataBuffer.getSize(); i++) {
            final int elem = dataBuffer.getElem(i);
            if (!set.contains(elem)) {
                //System.out.println("elem = " + elem);
                intMap.putValue(elem, intMap.getSize());
                set.add(elem);
            }
        }
        return intMap;
    }

    private static BufferedImage createSourceImage(int sourceType, int[] sourceValues) {
        final BufferedImage sourceImage = new BufferedImage(4, 4, sourceType);
        for (int i = 0; i < sourceValues.length; i++) {
            sourceImage.getRaster().getDataBuffer().setElem(i, sourceValues[i]);
        }
        return sourceImage;
    }


}
