/*
 * $Id: SeaWiFSBinLocatorTest.java,v 1.1 2006/09/11 10:47:33 norman Exp $
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
package org.esa.beam.processor.binning.database;

import java.awt.Point;

import junit.framework.TestCase;

import org.esa.beam.framework.datamodel.GeoPos;

public class SeaWiFSBinLocatorTest extends TestCase {

    /**
     * Tests the bin locator for correct functionality when fed with index
     */
    public void testLocatorOnIndex() {

        BinLocator locator = new SeaWiFSBinLocator(2003.8f);

        float[] inLat = {-85.f, -68.f, -40.f, -32.f, -4.f, 13.f, 22.f, 50.f, 58.f, 76.f};
        float[] inLon = {33.f, 118.f, -174.f, -134.f, -102.f, -98.f, -85.f, -63.f, -13.f, 45.f};
        int[] expIdx = {1, 10, 12, 28, 48, 68, 88, 106, 120, 126};

        // check the loat/lon to index conversion
        GeoPos geoPos = new GeoPos();

        for (int n = 0; n < inLat.length; n++) {
            geoPos.lat = inLat[n];
            geoPos.lon = inLon[n];

            assertEquals(expIdx[n], locator.getIndex(geoPos));
        }

        // check index to lat/lon conversion
        float[] expLat = {-81.f, -63.f, -45.f, -27.f, -9.f, 9.f, 27.f, 45.f, 63.f, 81.f};
        float[] expLon = {0.f, 120.f, -167.14285278f, -130.f, -99.f, -99.f, -90.f, -64.28571319f, 0.f, 0.f};

        for (int n = 0; n < expLat.length; n++) {
            geoPos = locator.getLatLon(expIdx[n], geoPos);

            assertEquals(expLat[n], geoPos.lat, 1e-6);
            assertEquals(expLon[n], geoPos.lon, 1e-6);
        }
    }

    /**
     * Tests the bin locator for correct functionality when fed with index
     */
    public void testLocatorOnRowCol() {
        BinLocator locator = new SeaWiFSBinLocator(2003.8f);

        // check to get row/col from lat/lon
        float[] inLat = {-85.f, -68.f, -40.f, -32.f, -4.f, 13.f, 22.f, 50.f, 58.f, 76.f};
        float[] inLon = {33.f, 118.f, -174.f, -134.f, -102.f, -98.f, -85.f, -63.f, -13.f, 45.f};
        int[] expRow = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] expCol = {1, 7, 0, 2, 4, 4, 4, 4, 4, 1};

        GeoPos geoPos = new GeoPos();
        Point rowcol = new Point();

        for (int n = 0; n < inLat.length; n++) {
            geoPos.lat = inLat[n];
            geoPos.lon = inLon[n];

            rowcol = locator.getRowCol(geoPos, rowcol);

            assertEquals(expRow[n], rowcol.y);
            assertEquals(expCol[n], rowcol.x);
        }


        float[] expLat = {-81.f, -63.f, -45.f, -27.f, -9.f, 9.f, 27.f, 45.f, 63.f, 81.f};
        float[] expLon = {0.f, 120.f, -167.14285278f, -130.f, -99.f, -99.f, -90.f, -64.28571319f, 0.f, 0.f};

        for (int n = 0; n < inLat.length; n++) {
            rowcol.y = expRow[n];
            rowcol.x = expCol[n];

            geoPos = locator.getLatLon(rowcol, geoPos);

            assertEquals(expLat[n], geoPos.lat, 1e-6);
            assertEquals(expLon[n], geoPos.lon, 1e-6);
        }
    }
}
