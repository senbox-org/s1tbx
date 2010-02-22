/*
 * $Id$
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
package org.esa.beam.dataio.obpg;

import org.esa.beam.dataio.obpg.ObpgUtils;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;

import ucar.nc2.Attribute;

import java.util.ArrayList;

import junit.framework.TestCase;

public class ObpgUtils_createProductBody_Test extends TestCase {

    private ObpgUtils obpgUtils;

    @Override
    protected void setUp() throws Exception {
        obpgUtils = new ObpgUtils();
    }

    public void test_ok() throws ProductIOException {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "ProductName"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "ProductType"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_WIDTH, 2030));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_HEIGHT, 1354));
        
        final Product product = obpgUtils.createProductBody(globalAttributes);

        assertNotNull(product);
        assertEquals("ProductName", product.getName());
        assertEquals("OBPG ProductType", product.getProductType());
        assertEquals(2030, product.getSceneRasterWidth());
        assertEquals(1354, product.getSceneRasterHeight());
    }

    public void test_WithoutNameAttribute() {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        //globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "ProductName"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "ProductType"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_WIDTH, 2030));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_HEIGHT, 1354));

        try {
            obpgUtils.createProductBody(globalAttributes);
            fail("should not come here");
        } catch (ProductIOException e) {
            assertTrue(e.getMessage().contains(ObpgUtils.KEY_NAME));
            assertTrue(e.getMessage().contains("is missing"));
        }
    }

    public void test_WithoutTypeAttribute() {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "ProductName"));
        //globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "ProductType"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_WIDTH, 2030));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_HEIGHT, 1354));

        try {
            obpgUtils.createProductBody(globalAttributes);
            fail("should not come here");
        } catch (ProductIOException e) {
            assertTrue(e.getMessage().contains(ObpgUtils.KEY_TYPE));
            assertTrue(e.getMessage().contains("is missing"));
        }
    }

    public void test_WithoutWidthAttribute() {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "ProductName"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "ProductType"));
        //globalAttributes.add(new Attribute(ObpgUtils.KEY_WIDTH, 2030));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_HEIGHT, 1354));

        try {
            obpgUtils.createProductBody(globalAttributes);
            fail("should not come here");
        } catch (ProductIOException e) {
            assertTrue(e.getMessage().contains(ObpgUtils.KEY_WIDTH));
            assertTrue(e.getMessage().contains("is missing"));
        }
    }

    public void test_WithoutHeightAttribute() {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "ProductName"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "ProductType"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_WIDTH, 2030));
        //globalAttributes.add(new Attribute(ObpgUtils.KEY_HEIGHT, 1354));

        try {
            obpgUtils.createProductBody(globalAttributes);
            fail("should not come here");
        } catch (ProductIOException e) {
            assertTrue(e.getMessage().contains(ObpgUtils.KEY_HEIGHT));
            assertTrue(e.getMessage().contains("is missing"));
        }
    }
}
