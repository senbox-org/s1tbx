/*
 * $Id: BasicDisplayInfoTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
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
package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;

public class BasicDisplayInfoTest extends TestCase {

    public BasicDisplayInfoTest(String name) {
        super(name);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testNoHistogramAvailable() {
        final ImageInfo basicDisplayInfo = new ImageInfo(3.1f, 4.6f, null);
        assertEquals(false, basicDisplayInfo.isHistogramAvailable());
        assertEquals(-1, basicDisplayInfo.getHistogramViewBinCount(), 1e-5f);
        assertEquals(-1f, basicDisplayInfo.getFirstHistogramViewBinIndex(), 1e-5f);
    }

    public void testGetVisibleBinCount() {
        final int[] bins = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        final ImageInfo basicDisplayInfo = new ImageInfo(3.1f, 4.6f, bins);
        assertEquals(true, basicDisplayInfo.isHistogramAvailable());
        assertEquals(16, basicDisplayInfo.getHistogramViewBinCount(), 1e-5f);
        basicDisplayInfo.setMinHistogramViewSample(3.4f);
        basicDisplayInfo.setMaxHistogramViewSample(4.0f);
        assertEquals(6.4, basicDisplayInfo.getHistogramViewBinCount(), 1e-5f);
    }

    public void testGetVisibleHistoBinOffset() {
        final int[] bins = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        final ImageInfo basicDisplayInfo = new ImageInfo(3.1f, 4.6f, bins);
        assertEquals(true, basicDisplayInfo.isHistogramAvailable());
        assertEquals(0, basicDisplayInfo.getFirstHistogramViewBinIndex(), 1e-5f);
        basicDisplayInfo.setMinHistogramViewSample(3.4f);
        assertEquals(3, basicDisplayInfo.getFirstHistogramViewBinIndex(), 1e-5f);
    }
}
