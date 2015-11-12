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
package org.esa.snap.core.datamodel;

import static org.junit.Assert.*;

import org.junit.*;

import java.util.Arrays;

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
        assertArrayEquals(new String[0], a_unc.getAncillaryRelations());
        assertArrayEquals(new String[0], a_var.getAncillaryRelations());

        a.addAncillaryVariable(a_unc, "uncertainty");

        assertEquals(a_unc, a.getAncillaryVariable("uncertainty"));
        assertArrayEquals(new RasterDataNode[]{a_unc}, a.getAncillaryVariables("uncertainty"));
        assertArrayEquals(new String[]{"uncertainty"}, a_unc.getAncillaryRelations());

        a.addAncillaryVariable(a_var, "variance");

        assertEquals(a_var, a.getAncillaryVariable("variance"));
        assertArrayEquals(new RasterDataNode[]{a_var}, a.getAncillaryVariables("variance"));
        assertArrayEquals(new String[]{"variance"}, a_var.getAncillaryRelations());

        a.addAncillaryVariable(a_var, "uncertainty");

        assertEquals(a_unc, a.getAncillaryVariable("uncertainty"));
        assertArrayEquals(new RasterDataNode[]{a_unc, a_var}, a.getAncillaryVariables("uncertainty"));
        assertArrayEquals(new String[]{"uncertainty"}, a_var.getAncillaryRelations());

        a.removeAncillaryVariable(a_var);
        a.removeAncillaryVariable(a_unc);

        assertEquals(null, a.getAncillaryVariable("uncertainty"));
        assertArrayEquals(new RasterDataNode[]{}, a.getAncillaryVariables("uncertainty"));
        assertArrayEquals(new String[]{"uncertainty"}, a_unc.getAncillaryRelations());

        assertEquals(null, a.getAncillaryVariable("variance"));
        assertArrayEquals(new RasterDataNode[]{}, a.getAncillaryVariables("variance"));
        assertArrayEquals(new String[]{"uncertainty"}, a_var.getAncillaryRelations());

        assertEquals("" +
                     "ancillaryRelations;ancillaryVariables;" +
                     "ancillaryRelations;ancillaryVariables;" +
                     "ancillaryRelations;ancillaryVariables;" +
                     "ancillaryVariables;" +
                     "ancillaryVariables;", pnl.simpleTrace);
    }

    @Test
    public void testUncertaintyRelationIsDefault() {

        a.addAncillaryVariable(a_unc);
        a.addAncillaryVariable(a_var);

        assertArrayEquals(new String[0], a_unc.getAncillaryRelations());
        assertArrayEquals(new String[0], a_var.getAncillaryRelations());

        assertEquals(a_unc, a.getAncillaryVariable());
        assertArrayEquals(new RasterDataNode[]{a_unc, a_var}, a.getAncillaryVariables());

        assertEquals(a_unc, a.getAncillaryVariable("uncertainty"));
        assertArrayEquals(new RasterDataNode[]{a_unc, a_var}, a.getAncillaryVariables("uncertainty"));

        assertEquals(null, a.getAncillaryVariable("variance"));
        assertArrayEquals(new RasterDataNode[]{}, a.getAncillaryVariables("variance"));
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

    @Test(expected = NullPointerException.class)
    public void testSetAncillaryRelationsWithNullArg() {
        a_unc.setAncillaryRelations((String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetAncillaryRelationsWithNullItems() {
        a_unc.setAncillaryRelations("arg1", null);
    }

    @Test
    public void testGetSetAncillaryRelation() {

        TracingPNL pnl = new TracingPNL();
        product.addProductNodeListener(pnl);

        assertArrayEquals(new String[0], a_unc.getAncillaryRelations());
        a_unc.setAncillaryRelations("error");
        assertArrayEquals(new String[]{"error"}, a_unc.getAncillaryRelations());
        a_unc.setAncillaryRelations();
        assertArrayEquals(new String[]{}, a_unc.getAncillaryRelations());
        a_unc.setAncillaryRelations("uncertainty");
        a_unc.setAncillaryRelations("uncertainty");

        assertEquals("" +
                     "ancillaryRelations([],[error]);" +
                     "ancillaryRelations([error],[]);" +
                     "ancillaryRelations([],[uncertainty]);", pnl.detailedTrace);
    }

    @Test
    @Ignore
    public void testAncillaryRelationLost() {
        Band an_anc_var = product.addBand("anyAncillarryBand", "X / (Y + 1)");
        Band an_other_band = product.addBand("any_Band", "Y / (X + 1)");

        a.addAncillaryVariable(an_anc_var, "relation 1", "relation 2");
        an_other_band.addAncillaryVariable(an_anc_var, "relation 3");

        String expected = Arrays.toString(new String[]{"relation 1", "relation 2", "relation 3"});
        String actual = Arrays.toString(an_anc_var.getAncillaryRelations());
        assertEquals(expected, actual);
    }

    private static class TracingPNL extends ProductNodeListenerAdapter {

        String simpleTrace = "";
        String detailedTrace = "";

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            simpleTrace += String.format("%s;", event.getPropertyName());
            detailedTrace += String.format("%s(%s,%s);", event.getPropertyName(), toString(event.getOldValue()), toString(event.getNewValue()));
        }

        private static Object toString(Object object) {
            return object instanceof Object[] ? Arrays.toString((Object[]) object) : String.valueOf(object);
        }
    }

}
