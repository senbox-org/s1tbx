/*
 * $Id: BinDatabaseConstantsTest.java,v 1.1 2006/09/11 10:47:33 norman Exp $
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BinDatabaseConstantsTest extends TestCase {

    public BinDatabaseConstantsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(BinDatabaseConstantsTest.class);
    }

    public void testDatabaseLocationConstants() {
        assertEquals("algorithm.props", BinDatabaseConstants.ALGORITHM_PROPERTIES_FILE);
        assertEquals("context.props", BinDatabaseConstants.CONTEXT_PROPERTIES_FILE);
        assertEquals("temp.dat", BinDatabaseConstants.TEMP_DB_NAME);
        assertEquals("final.dat", BinDatabaseConstants.FINAL_DB_NAME);
        assertEquals(".binData", BinDatabaseConstants.DIRECTORY_EXTENSION);
        assertEquals(".bindb", BinDatabaseConstants.FILE_EXTENSION);
        assertEquals("Bin Database Files", BinDatabaseConstants.FILE_EXTENSION_DESCRIPTION);
    }

    public void testPropertyKeys() {
        assertEquals("cell_size", BinDatabaseConstants.CELL_SIZE_KEY);
        assertEquals("product_count", BinDatabaseConstants.PRODUCT_COUNT_KEY);
        assertEquals("product", BinDatabaseConstants.PROCESSED_PRODUCT_BASE_KEY);
        assertEquals("storage_type", BinDatabaseConstants.STORAGE_TYPE_KEY);
        assertEquals("lat_min", BinDatabaseConstants.LAT_MIN_KEY);
        assertEquals("lat_max", BinDatabaseConstants.LAT_MAX_KEY);
        assertEquals("lon_min", BinDatabaseConstants.LON_MIN_KEY);
        assertEquals("lon_max", BinDatabaseConstants.LON_MAX_KEY);
    }

    public void testOtherConstants() {
        assertEquals("simple", BinDatabaseConstants.DATABASE_SIMPLE_VALUE);
        assertEquals("quad", BinDatabaseConstants.DATABASE_QUAD_TREE_VALUE);
        assertEquals((float) (Math.PI * 6378.137), BinDatabaseConstants.PI_EARTH_RADIUS, 1e-6);
        assertEquals(9.28f, BinDatabaseConstants.SEA_WIFS_CELL_SIZE, 1e-6);
    }

}
