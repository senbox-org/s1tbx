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

package org.esa.beam.collocation.visat;

import javax.swing.ComboBoxModel;

import junit.framework.TestCase;

import org.esa.beam.collocation.ResamplingType;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;

/**
 * Tests for class {@link CollocationFormModel}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class CollocationFormModelTest extends TestCase {

    private CollocationFormModel collocationFormModel;
    private ComboBoxModel resamplingComboBoxModel;

    @Override
    protected void setUp() throws Exception {
        collocationFormModel = new CollocationFormModel(new TargetProductSelector().getModel());
        resamplingComboBoxModel = collocationFormModel.getResamplingComboBoxModel();
    }

    @Override
    protected void tearDown() throws Exception {
        collocationFormModel = null;
    }

    public void testSetMasterProduct() {
        final Product product = new Product("name", "type", 10, 10);

        collocationFormModel.setMasterProduct(product);
        assertSame(product, collocationFormModel.getMasterProduct());
    }

    public void testSetSlaveProduct() {
        final Product product = new Product("name", "type", 10, 10);

        collocationFormModel.setMasterProduct(product);
        assertSame(product, collocationFormModel.getMasterProduct());
    }

//    public void testSetCreateNewProduct() {
//        collocationFormModel.setCreateNewProduct(true);
//        assertTrue(collocationFormModel.isCreateNewProductSelected());
//
//        collocationFormModel.setCreateNewProduct(false);
//        assertFalse(collocationFormModel.isCreateNewProductSelected());
//    }

    public void testSetRenameMasterComponents() {
        collocationFormModel.setRenameMasterComponents(true);
        assertTrue(collocationFormModel.isRenameMasterComponentsSelected());

        collocationFormModel.setRenameMasterComponents(false);
        assertFalse(collocationFormModel.isRenameMasterComponentsSelected());
    }

    public void testSetRenameSlaveComponents() {
        collocationFormModel.setRenameSlaveComponents(true);
        assertTrue(collocationFormModel.isRenameSlaveComponentsSelected());

        collocationFormModel.setRenameSlaveComponents(false);
        assertFalse(collocationFormModel.isRenameSlaveComponentsSelected());
    }

    public void testSetMasterComponentPattern() {
        final String pattern = "master_${component_name}";

        collocationFormModel.setMasterComponentPattern(pattern);
        assertEquals(pattern, collocationFormModel.getMasterComponentPattern());
    }

    public void testSetSlaveComponentPattern() {
        final String pattern = "slave_${component_name}";

        collocationFormModel.setSlaveComponentPattern(pattern);
        assertEquals(pattern, collocationFormModel.getSlaveComponentPattern());
    }

    public void testSetResampling() {
        collocationFormModel.setResamplingType(ResamplingType.BILINEAR_INTERPOLATION);
        assertEquals(ResamplingType.BILINEAR_INTERPOLATION, collocationFormModel.getResamplingType());

        collocationFormModel.setResamplingType(ResamplingType.CUBIC_CONVOLUTION);
        assertEquals(ResamplingType.CUBIC_CONVOLUTION, collocationFormModel.getResamplingType());

        collocationFormModel.setResamplingType(ResamplingType.NEAREST_NEIGHBOUR);
        assertEquals(ResamplingType.NEAREST_NEIGHBOUR, collocationFormModel.getResamplingType());
    }

    public void testAdaptResamplingComboBoxModel() {
        final Product product = new Product("name", "type", 10, 10);
        final Band band1 = product.addBand("band1", ProductData.TYPE_INT32);
        final Band band2 = product.addBand("band2", ProductData.TYPE_INT32);

        collocationFormModel.setSlaveProduct(product);
        collocationFormModel.adaptResamplingComboBoxModel();
        assertEquals(3, resamplingComboBoxModel.getSize());

        band1.setValidPixelExpression("true");
        collocationFormModel.adaptResamplingComboBoxModel();
        assertEquals(1, resamplingComboBoxModel.getSize());
        assertEquals(ResamplingType.NEAREST_NEIGHBOUR, collocationFormModel.getResamplingType());

        band1.setValidPixelExpression(null);
        band2.setValidPixelExpression("  ");
        collocationFormModel.adaptResamplingComboBoxModel();
        assertEquals(3, resamplingComboBoxModel.getSize());
        assertEquals(ResamplingType.NEAREST_NEIGHBOUR, collocationFormModel.getResamplingType());
    }
}
