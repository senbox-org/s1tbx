/*
 * $Id: ProductDataByteTest.java,v 1.2 2007/02/19 12:58:42 marcoz Exp $
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

import java.io.File;
import java.io.IOException;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;

import junit.framework.TestCase;

import org.esa.beam.GlobalTestConfig;
import org.esa.beam.util.SystemUtils;

public class ProductDataBooleanTest extends TestCase {

    private FileImageOutputStream _outputStream;
    private FileImageInputStream _inputStream;

    @Override
	protected void setUp() {
        File outputFile = GlobalTestConfig.getBeamTestDataOutputFile("ProductData");
        outputFile.mkdirs();
        File streamFile = new File(outputFile, "stream.img");
        try {
            streamFile.createNewFile();
            _inputStream = new FileImageInputStream(streamFile);
            _outputStream = new FileImageOutputStream(streamFile);
        } catch (IOException e) {
        }
        assertNotNull(_inputStream);
        assertNotNull(_outputStream);
    }

    @Override
	protected void tearDown() {
        try {
            _inputStream.close();
            _outputStream.close();
        } catch (IOException e) {
        }
        SystemUtils.deleteFileTree(GlobalTestConfig.getBeamTestDataOutputDirectory());
    }

    public void testSingleValueConstructor() {
        ProductData instance = ProductData.createInstance(ProductData.TYPE_BOOLEAN);
        instance.setElems(new boolean[]{true});

        assertEquals(ProductData.TYPE_BOOLEAN, instance.getType());
        assertEquals(1, instance.getElemInt());
        assertEquals(1L, instance.getElemUInt());
        assertEquals(1F, instance.getElemFloat(), 0.0e-12F);
        assertEquals(1D, instance.getElemDouble(), 0.0e-12D);
        assertEquals("true", instance.getElemString());
        assertEquals(1, instance.getNumElems());
        Object data = instance.getElems();
        assertEquals(true, data instanceof boolean[]);
        assertEquals(1, ((boolean[]) data).length);
        assertEquals(true, instance.isScalar());
        assertEquals(false, instance.isInt());
        assertEquals("true", instance.toString());
        assertEquals(true, instance.getElemBoolean());

        ProductData expectedEqual = ProductData.createInstance(ProductData.TYPE_BOOLEAN);
        expectedEqual.setElems(new boolean[]{true});
        assertEquals(true, instance.equalElems(expectedEqual));

        ProductData expectedUnequal = ProductData.createInstance(ProductData.TYPE_BOOLEAN);
        expectedUnequal.setElems(new boolean[]{false});
        assertEquals(false, instance.equalElems(expectedUnequal));

//        StreamTest
        ProductData dataFromStream = null;
        try {
            instance.writeTo(_outputStream);
            dataFromStream = ProductData.createInstance(ProductData.TYPE_BOOLEAN);
            dataFromStream.readFrom(_inputStream);
        } catch (IOException e) {
            fail("IOException not expected");
        }
        assertEquals(true, instance.equalElems(dataFromStream));
    }

    public void testConstructor() {
        ProductData instance = ProductData.createInstance(ProductData.TYPE_BOOLEAN, 2);
        instance.setElems(new boolean[]{true, false});

        assertEquals(ProductData.TYPE_BOOLEAN, instance.getType());
        assertEquals(1, instance.getElemIntAt(0));
        assertEquals(0, instance.getElemIntAt(1));
        assertEquals(1L, instance.getElemUIntAt(0));
        assertEquals(0L, instance.getElemUIntAt(1));
        assertEquals(1.0F, instance.getElemFloatAt(0), 0.0e-12F);
        assertEquals(0.0F, instance.getElemFloatAt(1), 0.0e-12F);
        assertEquals(1.0D, instance.getElemDoubleAt(0), 0.0e-12D);
        assertEquals(0.0D, instance.getElemDoubleAt(1), 0.0e-12D);
        assertEquals("true", instance.getElemStringAt(0));
        assertEquals("false", instance.getElemStringAt(1));
        assertEquals(true, instance.getElemBooleanAt(0));
        assertEquals(false, instance.getElemBooleanAt(1));
        assertEquals(2, instance.getNumElems());
        Object data2 = instance.getElems();
        assertEquals(true, data2 instanceof boolean[]);
        assertEquals(2, ((boolean[]) data2).length);
        assertEquals(false, instance.isScalar());
        assertEquals(false, instance.isInt());
        assertEquals("true,false", instance.toString());

        ProductData expectedEqual = ProductData.createInstance(ProductData.TYPE_BOOLEAN, 2);
        expectedEqual.setElems(new boolean[]{true, false});
        assertEquals(true, instance.equalElems(expectedEqual));

        ProductData expectedUnequal = ProductData.createInstance(ProductData.TYPE_BOOLEAN, 2);
        expectedUnequal.setElems(new boolean[]{true, true});
        assertEquals(false, instance.equalElems(expectedUnequal));

//        StreamTest
        ProductData dataFromStream = null;
        try {
            instance.writeTo(_outputStream);
            dataFromStream = ProductData.createInstance(ProductData.TYPE_BOOLEAN, 2);
            dataFromStream.readFrom(_inputStream);
        } catch (IOException e) {
            fail("IOException not expected");
        }
        assertEquals(true, instance.equalElems(dataFromStream));
    }

    public void testSetElemsAsString() {
        final ProductData pd = ProductData.createInstance(ProductData.TYPE_BOOLEAN, 4);
        pd.setElems(new String[]{
                String.valueOf(Boolean.FALSE),
                String.valueOf(Boolean.TRUE),
                String.valueOf(0),
                String.valueOf(1)
        });

        assertEquals(4, pd.getNumElems());
        assertEquals(0, pd.getElemIntAt(0));
        assertEquals(1, pd.getElemIntAt(1));
        assertEquals(0, pd.getElemIntAt(2));
        assertEquals(0, pd.getElemIntAt(3)); // only "true" is parsed to TRUE, everthing else is "false" 
    }
    
    public void testSetter() throws Exception {
    	ProductData instance = ProductData.createInstance(ProductData.TYPE_BOOLEAN, 2);
        instance.setElems(new boolean[]{true, false});

        assertEquals(true, instance.getElemBooleanAt(0));
        assertEquals(false, instance.getElemBooleanAt(1));
        assertEquals(true, instance.getElemBoolean());
        
        instance.setElemBooleanAt(0, false);
        assertEquals(false, instance.getElemBooleanAt(0));
        assertEquals(false, instance.getElemBooleanAt(1));
        
        instance.setElemBoolean(true);
        assertEquals(true, instance.getElemBooleanAt(0));
        assertEquals(false, instance.getElemBooleanAt(1));
	}
}
