/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package com.bc.ceres.binio.binx;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import java.util.Map;

import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;

public class ArrayFixedTest extends TestCase {
    public void testBinX() throws IOException, BinXException, URISyntaxException {
        BinX binx = createBinX();

        Map<String, Type> definitions = binx.getDefinitions();
        assertTrue(definitions.get("FixedFloat32ArrayType") instanceof CompoundType);
        CompoundType arrayCompoundType = (CompoundType) definitions.get("FixedFloat32ArrayType");
        assertEquals("FixedFloat32ArrayType", arrayCompoundType.getName());
        assertEquals(1, arrayCompoundType.getMemberCount());
        assertEquals("af", arrayCompoundType.getMember(0).getName());
        assertEquals("float[7]", arrayCompoundType.getMember(0).getType().getName());

        assertNotNull(binx.getDataset());
        assertEquals("Dataset", binx.getDataset().getName());
        assertEquals(3, binx.getDataset().getMemberCount());
        assertEquals("magic1", binx.getDataset().getMember(0).getName());
        assertEquals("data1", binx.getDataset().getMember(1).getName());
        assertEquals("magic2", binx.getDataset().getMember(2).getName());

        assertSame(arrayCompoundType, binx.getDataset().getMember(1).getType());
    }

    public void testFormat() throws URISyntaxException, IOException, BinXException {
        BinX binx = createBinX();

        DataFormat format = binx.getFormat("ArrayFixedTest");
        assertEquals("ArrayFixedTest", format.getName());
        assertNotNull(format.getType());
        assertEquals("Dataset", format.getType().getName());
        assertEquals(true, format.isTypeDef("FixedFloat32ArrayType"));
        assertTrue(format.getTypeDef("FixedFloat32ArrayType") instanceof CompoundType);
    }

    private BinX createBinX() throws URISyntaxException, IOException, BinXException {
        URL resource = getClass().getResource("ArrayFixed.binXschema.xml");
        assertNotNull(resource);
        URI uri = resource.toURI();
        return new BinX(uri);
    }
}
