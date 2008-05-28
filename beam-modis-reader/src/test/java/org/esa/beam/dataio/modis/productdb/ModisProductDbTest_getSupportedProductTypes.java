/*
 * $Id$
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
package org.esa.beam.dataio.modis.productdb;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.util.StringUtils;

public class ModisProductDbTest_getSupportedProductTypes extends TestCase {

    public void testFail() throws ProductIOException {
        final String[] expectedProductTypes = new String[]{
                "MOD021KM", "MYD021KM",
                "MOD021KM_IMAPP",
                "MOD02HKM", "MYD02HKM",
                "MOD02QKM", "MYD02QKM",
                "MODOCL2", "MYDOCL2",
                "MODOCL2A", "MYDOCL2A",
                "MODOCL2B", "MYDOCL2B",
                "MOD28L2", "MYD28L2",
                "MODOCQC", "MYDOCQC",
                "MOD13A2", "MYD13A2",
                "MOD15A2", "MYD15A2"
        };

        final String[] types = ModisProductDb.getInstance().getSupportetProductTypes();
        assertNotNull(types);
        assertEquals(expectedProductTypes.length, types.length);
        for (int i = 0; i < expectedProductTypes.length; i++) {
            final String type = expectedProductTypes[i];
            assertTrue("type <" + type + "> not in the result", StringUtils.contains(types, type));
        }
    }
}
