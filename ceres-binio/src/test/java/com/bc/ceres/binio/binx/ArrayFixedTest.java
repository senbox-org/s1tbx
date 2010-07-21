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
package com.bc.ceres.binio.binx;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;

import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;

public class ArrayFixedTest extends TestCase {
    public void testBinX() throws IOException, BinXException, URISyntaxException {
        URL resource = getClass().getResource("ArrayFixed.binXschema.xml");
        assertNotNull(resource);
        URI uri = resource.toURI();
        BinX binx = new BinX();
        DataFormat dataFormat = binx.readDataFormat(uri);

        assertTrue(binx.getDefinition("FixedFloat32ArrayType") instanceof CompoundType);
        CompoundType arrayCompoundType = (CompoundType) binx.getDefinition("FixedFloat32ArrayType");
        assertEquals("FixedFloat32ArrayType", arrayCompoundType.getName());
        assertEquals(1, arrayCompoundType.getMemberCount());
        assertEquals("af", arrayCompoundType.getMember(0).getName());
        assertEquals("float[8]", arrayCompoundType.getMember(0).getType().getName());

        CompoundType datasetType = dataFormat.getType();
        assertNotNull(datasetType);
        assertEquals("Dataset", datasetType.getName());
        assertEquals(3, datasetType.getMemberCount());
        assertEquals("magic1", datasetType.getMember(0).getName());
        assertEquals("data1", datasetType.getMember(1).getName());
        assertEquals("magic2", datasetType.getMember(2).getName());

        assertSame(arrayCompoundType, datasetType.getMember(1).getType());
    }

    public void testFormat() throws URISyntaxException, IOException, BinXException {
        URL resource = getClass().getResource("ArrayFixed.binXschema.xml");
        assertNotNull(resource);
        URI uri = resource.toURI();
        BinX binx = new BinX();

        DataFormat format = binx.readDataFormat(uri, "ArrayFixedTest");
        assertEquals("ArrayFixedTest", format.getName());
        assertNotNull(format.getType());
        assertEquals("Dataset", format.getType().getName());
        assertEquals(true, format.isTypeDef("FixedFloat32ArrayType"));
        assertTrue(format.getTypeDef("FixedFloat32ArrayType") instanceof CompoundType);
    }

}
