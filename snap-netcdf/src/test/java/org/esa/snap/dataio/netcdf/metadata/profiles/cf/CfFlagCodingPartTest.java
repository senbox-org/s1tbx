/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.nc.NWritableFactory;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;

/**
 * @author Marco Peters
 */
public class CfFlagCodingPartTest extends TestCase {

    public void testReplaceNonWordCharacters() {
       assertEquals("a_b", CfFlagCodingPart.replaceNonWordCharacters("a/b"));
       assertEquals("a_b", CfFlagCodingPart.replaceNonWordCharacters("a / b"));
       assertEquals("a_b", CfFlagCodingPart.replaceNonWordCharacters("a.b"));
    }

    public void testWriteFlagCoding() throws Exception {
        Band flagBand = new Band("flag_band", ProductData.TYPE_UINT8, 10, 10);
        FlagCoding flagCoding = new FlagCoding("some_flags");
        flagBand.setSampleCoding(flagCoding);
        flagCoding.setDescription("A Flag Coding");
        for (int i = 0; i < 8; i++) {
            addFlag(flagCoding, i);
        }
        NFileWriteable n3writable = NWritableFactory.create("not stored", "netcdf3");
        n3writable.addDimension("y", flagBand.getRasterHeight());
        n3writable.addDimension("x", flagBand.getRasterWidth());
        final DataType ncDataType = DataTypeUtils.getNetcdfDataType(flagBand.getDataType());
        NVariable variable = n3writable.addVariable(flagBand.getName(), ncDataType, null,"y x");
        CfBandPart.writeCfBandAttributes(flagBand, variable);
        CfFlagCodingPart.writeFlagCoding(flagBand, n3writable);

        NVariable someFlagsVariable = n3writable.findVariable("flag_band");
        assertNotNull(someFlagsVariable);
        Attribute flagMasksAttrib = someFlagsVariable.findAttribute("flag_masks");
        assertNotNull(flagMasksAttrib);
        if (someFlagsVariable.findAttribute("_Unsigned").getStringValue().equals("true")) {
            someFlagsVariable.setDataType(someFlagsVariable.getDataType().withSignedness(DataType.Signedness.UNSIGNED));
        }

        assertEquals(someFlagsVariable.getDataType(), flagMasksAttrib.getDataType());
        assertEquals(8, flagMasksAttrib.getLength());
        assertTrue(flagMasksAttrib.getDataType().isUnsigned());
        for (int i = 0; i < 8; i++) {
            assertEquals(1 << i, flagMasksAttrib.getValues().getInt(i));
        }

        Attribute descriptionAttrib = someFlagsVariable.findAttribute("long_name");
        assertNotNull(flagCoding.getDescription(), descriptionAttrib.getStringValue());

    }

    private void addFlag(FlagCoding flagCoding, int index) {
        MetadataAttribute attribute;

        attribute = new MetadataAttribute(String.format("%d_FLAG", index + 1), ProductData.TYPE_UINT8);
        final int maskValue = 1 << index;
        attribute.getData().setElemInt(maskValue);
        flagCoding.addAttribute(attribute);
    }
}
