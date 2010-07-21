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
import org.esa.beam.dataio.modis.hdf.HdfAttributeContainer;
import org.esa.beam.dataio.modis.hdf.HdfAttributes;

import java.util.ArrayList;

public class ModisDaacUtilsTest_ExtractCoreString extends TestCase {

    private HdfAttributes hdfGlobalAttributes;

    @Override
    protected void setUp() throws Exception {
        final ArrayList<HdfAttributeContainer> attribList = new ArrayList<HdfAttributeContainer>();
        attribList.add(new HdfAttributeContainer("CoreMetadata.0.somewhatelse", 4, "otherPart", 9));
        attribList.add(new HdfAttributeContainer("CoreMetadata.1", 4, "secondPart", 10));
        attribList.add(new HdfAttributeContainer("OtherMetadata.0", 4, "anyString", 9));
        attribList.add(new HdfAttributeContainer("CoreMetadata.0", 4, "firstPart", 9));
        hdfGlobalAttributes = new HdfAttributes(attribList);
    }

    public void testOk() {
        final String coreString = ModisDaacUtils.extractCoreString(hdfGlobalAttributes);

        assertEquals("firstPartsecondPart", coreString);
    }
}
