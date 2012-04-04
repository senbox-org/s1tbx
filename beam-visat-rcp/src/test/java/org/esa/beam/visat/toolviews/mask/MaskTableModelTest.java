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

package org.esa.beam.visat.toolviews.mask;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;


public class MaskTableModelTest extends TestCase {
    public void testManagementMode() {
        MaskTableModel maskTableModel = new MaskTableModel(true);
        assertEquals(false, maskTableModel.isInManagmentMode());
        assertEquals(5, maskTableModel.getColumnCount());
        assertEquals("Name", maskTableModel.getColumnName(0));
        assertEquals("Type", maskTableModel.getColumnName(1));
        assertEquals("Colour", maskTableModel.getColumnName(2));
        assertEquals("Transparency", maskTableModel.getColumnName(3));
        assertEquals("Description", maskTableModel.getColumnName(4));
        assertEquals(0, maskTableModel.getRowCount());

        Product product = MaskFormTest.createTestProduct();
        maskTableModel.setProduct(product, null);
        assertEquals(true, maskTableModel.isInManagmentMode());
        assertEquals(5, maskTableModel.getColumnCount());
        assertEquals("Name", maskTableModel.getColumnName(0));
        assertEquals("Type", maskTableModel.getColumnName(1));
        assertEquals("Colour", maskTableModel.getColumnName(2));
        assertEquals("Transparency", maskTableModel.getColumnName(3));
        assertEquals("Description", maskTableModel.getColumnName(4));
        assertEquals(10, maskTableModel.getRowCount());

        maskTableModel.setProduct(product, product.getBand("C"));
        assertEquals(true, maskTableModel.isInManagmentMode());
        assertEquals(6, maskTableModel.getColumnCount());
        assertEquals("Visibility", maskTableModel.getColumnName(0));
        assertEquals("Name", maskTableModel.getColumnName(1));
        assertEquals("Type", maskTableModel.getColumnName(2));
        assertEquals("Colour", maskTableModel.getColumnName(3));
        assertEquals("Transparency", maskTableModel.getColumnName(4));
        assertEquals("Description", maskTableModel.getColumnName(5));
        assertEquals(10, maskTableModel.getRowCount());
    }

    public void testViewMode() {
        MaskTableModel maskTableModel = new MaskTableModel(false);
        assertEquals(false, maskTableModel.isInManagmentMode());
        assertEquals(4, maskTableModel.getColumnCount());
        assertEquals("Name", maskTableModel.getColumnName(0));
        assertEquals("Colour", maskTableModel.getColumnName(1));
        assertEquals("Transparency", maskTableModel.getColumnName(2));
        assertEquals("Description", maskTableModel.getColumnName(3));
        assertEquals(0, maskTableModel.getRowCount());

        Product product = MaskFormTest.createTestProduct();
        maskTableModel.setProduct(product, null);
        assertEquals(false, maskTableModel.isInManagmentMode());
        assertEquals(4, maskTableModel.getColumnCount());
        assertEquals("Name", maskTableModel.getColumnName(0));
        assertEquals("Colour", maskTableModel.getColumnName(1));
        assertEquals("Transparency", maskTableModel.getColumnName(2));
        assertEquals("Description", maskTableModel.getColumnName(3));
        assertEquals(10, maskTableModel.getRowCount());

        maskTableModel.setProduct(product, product.getBand("C"));
        assertEquals(false, maskTableModel.isInManagmentMode());
        assertEquals(5, maskTableModel.getColumnCount());
        assertEquals("Visibility", maskTableModel.getColumnName(0));
        assertEquals("Name", maskTableModel.getColumnName(1));
        assertEquals("Colour", maskTableModel.getColumnName(2));
        assertEquals("Transparency", maskTableModel.getColumnName(3));
        assertEquals("Description", maskTableModel.getColumnName(4));
        assertEquals(10, maskTableModel.getRowCount());
    }
}
