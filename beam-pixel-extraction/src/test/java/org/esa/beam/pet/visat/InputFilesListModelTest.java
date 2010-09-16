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

package org.esa.beam.pet.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

import static junit.framework.Assert.*;

/**
 * @author Thomas Storm
 */
public class InputFilesListModelTest {

    private InputListModel listModel;
    private Property inputPaths;

    @Before
    public void setUp() {
        inputPaths = Property.create("inputPaths", File[].class);
        listModel = new InputListModel(inputPaths);
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
        assertEquals(3, ((File[]) inputPaths.getValue()).length);

        ArrayList<Product> products = new ArrayList<Product>();
        products.add(new Product("abc", "meris", 10, 120));
        products.add(new Product("def", "meris", 10, 120));
        products.add(new Product("ghi", "meris", 10, 120));
        listModel.addElements(products.toArray());
        assertEquals(6, listModel.getSize());
        assertEquals(3, listModel.getSourceProducts().length);

        listModel.removeElementsAt(new int[]{0, 5});
        assertEquals(4, listModel.getSize());
        assertEquals(2, ((File[]) inputPaths.getValue()).length);
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
        assertEquals(0, ((File[]) inputPaths.getValue()).length);
    }
}
