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

package org.esa.beam.dataio.modis;

import junit.framework.TestCase;
import org.esa.beam.dataio.modis.productdb.ModisProductDb;
import org.esa.beam.framework.dataio.ProductIOException;

public class ModisDaacUtilsTest_extractProductType extends TestCase {

    public void testOk_MOD13A2_InTheMiddle() throws ProductIOException {
        final String typeString = ModisDaacUtils.extractProductType("MOD_SS.MOD13A2.somthing other");

        assertEquals("MOD13A2", typeString);
    }

    public void testOk_MYD13A2_AtStart() throws ProductIOException {
        final String typeString = ModisDaacUtils.extractProductType("MYD13A2.somthing other");

        assertEquals("MYD13A2", typeString);
    }

    public void testAllProductTypes_InTheMiddle() throws ProductIOException {
        final String[] supportetProductTypes = ModisProductDb.getInstance().getSupportetProductTypes();
        for (int i = 0; i < supportetProductTypes.length; i++) {
            final String type = supportetProductTypes[i];
            final String toTest = "anyPrefix." + type + ".anySuffix";
            assertEquals("Index = " + i, ModisDaacUtils.extractProductType(toTest), type);
        }
    }

    public void testAllProductTypes_AtStart() throws ProductIOException {
        final String[] supportetProductTypes = ModisProductDb.getInstance().getSupportetProductTypes();
        for (int i = 0; i < supportetProductTypes.length; i++) {
            final String type = supportetProductTypes[i];
            final String toTest = type + ".anySuffix";

            assertEquals("Index = " + i, ModisDaacUtils.extractProductType(toTest), type);

        }
    }

    public void testShittyEsaNotFollowingTheConventionsNameHandling() throws ProductIOException {
        final String[] supportetProductTypes = ModisProductDb.getInstance().getSupportetProductTypes();
        for (int i = 0; i < supportetProductTypes.length; i++) {
            final String type = supportetProductTypes[i];
            final String toTest = type + "_anySuffix";

            if (isImapp(toTest)) {
                // this does not work for IMAPP types - intentionally! tb 2008-06-17
                continue;
            }
            assertEquals("Index = " + i, ModisDaacUtils.extractProductType(toTest), type);
        }
    }

    public void testExtractProdcutTypeReturnsEmptyStringOnUnknown() throws ProductIOException {
        assertEquals("", ModisDaacUtils.extractProductType("This.is.an.invalid.type"));
        assertEquals("", ModisDaacUtils.extractProductType("This_is_an_invalid_type"));
    }

    private boolean isImapp(String toTest) {
        return toTest.indexOf("IMAPP") != -1;
    }
}