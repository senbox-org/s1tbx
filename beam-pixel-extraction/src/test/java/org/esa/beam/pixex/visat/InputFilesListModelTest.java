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

package org.esa.beam.pixex.visat;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import org.esa.beam.framework.datamodel.Product;
import org.junit.*;

import java.io.File;
import java.util.ArrayList;

/**
 * @author Thomas Storm
 */
public class InputFilesListModelTest {

    private InputListModel listModel;
    private Property property;

    @Before
    public void setUp() {
        property = Property.create("property", String[].class);
        property.setContainer(new PropertyContainer());
        listModel = new InputListModel(property);
    }


    @Test
    public void testAddAndRemove() throws ValidationException {
        listModel.addElements(new File("."));
        listModel.addElements(new File("."));
        assertEquals(1, listModel.getSize());

        listModel.removeElementsAt(new int[]{0});
        assertEquals(0, listModel.getSize());

        ArrayList<File> files = new ArrayList<File>();
        files.add(new File("abc"));
        files.add(new File("def"));
        files.add(new File("ghi"));
        listModel.addElements(files.toArray());
        assertEquals(3, listModel.getSize());
        assertEquals(3, ((String[]) property.getValue()).length);

        ArrayList<Product> products = new ArrayList<Product>();
        products.add(new Product("abc", "meris", 10, 120));
        products.add(new Product("def", "meris", 10, 120));
        products.add(new Product("ghi", "meris", 10, 120));
        listModel.addElements(products.toArray());
        assertEquals(6, listModel.getSize());
        assertEquals(3, listModel.getSourceProducts().length);

        listModel.removeElementsAt(new int[]{0, 5});
        assertEquals(4, listModel.getSize());
        assertEquals(2, ((String[]) property.getValue()).length);
        assertEquals(2, listModel.getSourceProducts().length);
    }

    @Test(expected = IllegalStateException.class)
    public void testFailingWithIllegalStateException() throws Exception {
        listModel.addElements("");
    }

    @Test
    public void testClear() throws Exception {
        listModel.addElements(new File("abc"));
        listModel.addElements(new File("def"));
        listModel.addElements(new Product("def", "producttype", 10, 10));
        listModel.addElements(new Product("dummy", "producttype", 10, 10));
        assertEquals(4, listModel.getSize());
        listModel.clear();
        assertEquals(0, listModel.getSize());
        assertEquals(0, listModel.getSourceProducts().length);
        assertEquals(0, ((String[]) property.getValue()).length);
    }

    @Test
    public void testSetElements() throws ValidationException {
        final String[] elements1 = {"a", "b", "c"};
        listModel.setElements(elements1);
        assertEquals(3, listModel.getSize());
        final String[] values1 = property.getValue();
        assertArrayEquals(elements1, values1);

        final String[] elements2 = {"f", "s", "g", "k"};
        listModel.setElements(elements2);
        assertEquals(4, listModel.getSize());
        final String[] values2 = property.getValue();
        assertArrayEquals(elements2, values2);
    }

    @Test
    public void testExternalPropertyValueChange() throws ValidationException {
        assertEquals(0, listModel.getSize());

        final String[] values = {"h", "i"};
        property.setValue(values);
        assertEquals(2, listModel.getSize());
        assertEquals("h", ((File)listModel.getElementAt(0)).getPath());
        assertEquals("i", ((File)listModel.getElementAt(1)).getPath());
    }
}
