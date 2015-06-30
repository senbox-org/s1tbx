/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.framework.datamodel;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class RasterDataNodeAncillaryTest {

    private Product product;
    private Band a;
    private Band a_unc;
    private Band a_var;

    @Before
    public void setUp() throws Exception {
        product = new Product("N", "T", 10, 10);
        a = product.addBand("a", "X + Y");
        a_unc = product.addBand("a_unc", "X + Y");
        a_var = product.addBand("a_var", "X + Y");
    }

    @Test
    public void testAddGetRemoveAncillaryVariable() {

        TracingPNL pnl = new TracingPNL();
        product.addProductNodeListener(pnl);

        assertEquals(null, a.getAncillaryVariable("uncertainty"));
        assertArrayEquals(new RasterDataNode[0], a.getAncillaryVariables("uncertainty"));
        assertEquals(null, a_unc.getAncillaryRelation());
        assertEquals(null, a_var.getAncillaryRelation());

        a.addAncillaryVariable(a_unc, "uncertainty");

        assertEquals(a_unc, a.getAncillaryVariable("uncertainty"));
        assertArrayEquals(new RasterDataNode[]{a_unc}, a.getAncillaryVariables("uncertainty"));
        assertEquals("uncertainty", a_unc.getAncillaryRelation());

        a.addAncillaryVariable(a_var, "variance");

        assertEquals(a_var, a.getAncillaryVariable("variance"));
        assertArrayEquals(new RasterDataNode[]{a_var}, a.getAncillaryVariables("variance"));
        assertEquals("variance", a_var.getAncillaryRelation());

        a.addAncillaryVariable(a_var, "uncertainty");

        assertEquals(a_unc, a.getAncillaryVariable("uncertainty"));
        assertArrayEquals(new RasterDataNode[]{a_unc, a_var}, a.getAncillaryVariables("uncertainty"));
        assertEquals("uncertainty", a_var.getAncillaryRelation());

        a.removeAncillaryVariable(a_var);
        a.removeAncillaryVariable(a_unc);

        assertEquals(null, a.getAncillaryVariable("uncertainty"));
        assertArrayEquals(new RasterDataNode[]{}, a.getAncillaryVariables("uncertainty"));
        assertEquals("uncertainty", a_unc.getAncillaryRelation());

        assertEquals(null, a.getAncillaryVariable("variance"));
        assertArrayEquals(new RasterDataNode[]{}, a.getAncillaryVariables("variance"));
        assertEquals("uncertainty", a_var.getAncillaryRelation());

        assertEquals("" +
                             "ancillaryRelation;ancillaryVariables;" +
                             "ancillaryRelation;ancillaryVariables;" +
                             "ancillaryRelation;ancillaryVariables;" +
                             "ancillaryVariables;" +
                             "ancillaryVariables;", pnl.simpleTrace);
    }

    @Test
    public void testUncertaintyRelationIsDefault() {

        a.addAncillaryVariable(a_unc);
        a.addAncillaryVariable(a_var);

        assertEquals(null, a_unc.getAncillaryRelation());
        assertEquals(null, a_var.getAncillaryRelation());

        assertEquals(a_unc, a.getAncillaryVariable(null));
        assertArrayEquals(new RasterDataNode[] {a_unc, a_var}, a.getAncillaryVariables(null));

        assertEquals(a_unc, a.getAncillaryVariable("uncertainty"));
        assertArrayEquals(new RasterDataNode[] {a_unc, a_var}, a.getAncillaryVariables("uncertainty"));

        assertEquals(null, a.getAncillaryVariable("variance"));
        assertArrayEquals(new RasterDataNode[] {}, a.getAncillaryVariables("variance"));
    }

    @Test
    public void testBandRemovalAlsoRemovesAncillaryVariable() {

        a.addAncillaryVariable(a_unc, "uncertainty");
        a.addAncillaryVariable(a_var, "uncertainty");

        assertArrayEquals(new RasterDataNode[]{a_unc, a_var}, a.getAncillaryVariables("uncertainty"));

        product.removeBand(a_unc);
        assertArrayEquals(new RasterDataNode[]{a_var}, a.getAncillaryVariables("uncertainty"));

        product.removeBand(a_var);
        assertArrayEquals(new RasterDataNode[]{}, a.getAncillaryVariables("uncertainty"));
    }

    @Test
    public void testGetSetAncillaryRelation() {

        TracingPNL pnl = new TracingPNL();
        product.addProductNodeListener(pnl);

        assertEquals(null, a_unc.getAncillaryRelation());
        a_unc.setAncillaryRelation("error");
        assertEquals("error", a_unc.getAncillaryRelation());
        a_unc.setAncillaryRelation(null);
        assertEquals(null, a_unc.getAncillaryRelation());
        a_unc.setAncillaryRelation("uncertainty");
        a_unc.setAncillaryRelation("uncertainty");

        assertEquals("" +
                             "ancillaryRelation(null,error);" +
                             "ancillaryRelation(error,null);" +
                             "ancillaryRelation(null,uncertainty);", pnl.detailedTrace);
    }


    private static class TracingPNL extends ProductNodeListenerAdapter {
        String simpleTrace = "";
        String detailedTrace = "";

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            simpleTrace += String.format("%s;", event.getPropertyName());
            detailedTrace += String.format("%s(%s,%s);", event.getPropertyName(), event.getOldValue(), event.getNewValue());
        }
    }

}
