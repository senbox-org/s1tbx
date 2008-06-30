/*
 * $Id: RGBImageProfileTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;

public class RGBImageProfileTest extends TestCase {

    public void testDefaults() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        assertEquals("X", profile.getName());
        assertEquals(false, profile.isInternal());
        assertEquals("", profile.getExpression(0));
        assertEquals("", profile.getExpression(1));
        assertEquals("", profile.getExpression(2));
        assertEquals("", profile.getExpression(3));
        assertNotNull(profile.getExpressions());
        assertEquals(4, profile.getExpressions().length);
    }

    public void testEqualsAndHashCode() {
        final RGBImageProfile profile1 = new RGBImageProfile("X", new String[] {"A", "B", "C"});
        final RGBImageProfile profile2 = new RGBImageProfile("X", new String[] {"A", "B", "C"});
        final RGBImageProfile profile3 = new RGBImageProfile("Y", new String[] {"A", "B", "C"});
        final RGBImageProfile profile4 = new RGBImageProfile("X", new String[] {"A", "B", "V"});
        final RGBImageProfile profile5 = new RGBImageProfile("X", new String[] {"A", "B", "C", "D"});

        assertTrue(profile1.equals(profile1));
        assertFalse(profile1.equals(null));
        assertTrue(profile1.equals(profile2));
        assertFalse(profile1.equals(profile3));
        assertFalse(profile1.equals(profile4));
        assertFalse(profile1.equals(profile5));

        assertTrue(profile1.hashCode() == profile2.hashCode());
        assertTrue(profile1.hashCode() != profile3.hashCode());
        assertTrue(profile1.hashCode() != profile4.hashCode());
        assertTrue(profile1.hashCode() != profile5.hashCode());
    }

    public void testThatComponentsMustNotBeNull() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        try {
            profile.setExpression(0, null);
            fail();
        } catch (NullPointerException e) {
        }
        try {
            profile.setExpression(1, null);
            fail();
        } catch (NullPointerException e) {
        }
        try {
            profile.setExpression(2, null);
            fail();
        } catch (NullPointerException e) {
        }
        try {
            profile.setExpression(3, null);
            fail();
        } catch (NullPointerException e) {
        }
    }

    public void testComponentsAsArrays() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        profile.setExpression(0, "radiance_1");
        profile.setExpression(1, "radiance_2");
        profile.setExpression(2, "radiance_4");
        profile.setExpression(3, "l1_flags.LAND ? 0 : 1");

        final String[] rgbaExpressions = profile.getExpressions();
        assertNotNull(rgbaExpressions);
        assertEquals(4, rgbaExpressions.length);
        assertEquals("radiance_1", rgbaExpressions[0]);
        assertEquals("radiance_2", rgbaExpressions[1]);
        assertEquals("radiance_4", rgbaExpressions[2]);
        assertEquals("l1_flags.LAND ? 0 : 1", rgbaExpressions[3]);
    }

    public void testApplicabilityOfEmptyProfile() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        assertEquals(false, profile.isApplicableTo(createTestProduct()));
    }

    public void testApplicabilityIfAlphaComponentIsMissing() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        profile.setRedExpression("U+V");
        profile.setGreenExpression("V+W");
        profile.setBlueExpression("W+X");
        profile.setAlphaExpression("");
        assertEquals(true, profile.isApplicableTo(createTestProduct()));
    }

    public void testApplicabilityIfOneComponentIsMissing() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        profile.setRedExpression("U+V");
        profile.setGreenExpression("");
        profile.setBlueExpression("W+X");
        profile.setAlphaExpression("Y+Z");
        assertEquals(true, profile.isApplicableTo(createTestProduct()));
    }

    public void testApplicabilityIfUnknownBandIsUsed() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        profile.setRedExpression("U+V");
        profile.setGreenExpression("V+K"); // unknown band K
        profile.setBlueExpression("W+X");
        profile.setAlphaExpression("");
        assertEquals(false, profile.isApplicableTo(createTestProduct()));
    }

    public void testStoreRgbaExpressions() {
        final Product p1 = createTestProduct();
        RGBImageProfile.storeRgbaExpressions(p1, new String[]{"U", "V", "W", "X"});
        assertNotNull(p1.getBand(RGBImageProfile.RED_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.GREEN_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.BLUE_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.ALPHA_BAND_NAME));
    }

    public void testStoreRgbaExpressionsWithoutAlpha() {
        final Product p1 = createTestProduct();
        RGBImageProfile.storeRgbaExpressions(p1, new String[]{"U", "V", "W", ""});
        assertNotNull(p1.getBand(RGBImageProfile.RED_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.GREEN_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.BLUE_BAND_NAME));
        assertNull(p1.getBand(RGBImageProfile.ALPHA_BAND_NAME));
    }

    public void testStoreRgbaExpressionsWithoutGreen() {
        final Product p1 = createTestProduct();
        RGBImageProfile.storeRgbaExpressions(p1, new String[]{"U", "", "W", ""});
        assertNotNull(p1.getBand(RGBImageProfile.RED_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.GREEN_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.BLUE_BAND_NAME));
        assertNull(p1.getBand(RGBImageProfile.ALPHA_BAND_NAME));

        assertEquals("0", ((VirtualBand) p1.getBand(RGBImageProfile.GREEN_BAND_NAME)).getExpression());
    }

    public void testStoreRgbaExpressionsOverwrite() {
        final Product p1 = createTestProduct();
        RGBImageProfile.storeRgbaExpressions(p1, new String[]{"U", "V", "W", "X"});
        assertNotNull(p1.getBand(RGBImageProfile.RED_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.GREEN_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.BLUE_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.ALPHA_BAND_NAME));
        RGBImageProfile.storeRgbaExpressions(p1, new String[]{"0.3", "2.0", "6.7", ""});
        assertNotNull(p1.getBand(RGBImageProfile.RED_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.GREEN_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.BLUE_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.ALPHA_BAND_NAME)); // since exist before
        assertEquals("0.3", ((VirtualBand) p1.getBand(RGBImageProfile.RED_BAND_NAME)).getExpression());
        assertEquals("2.0", ((VirtualBand) p1.getBand(RGBImageProfile.GREEN_BAND_NAME)).getExpression());
        assertEquals("6.7", ((VirtualBand) p1.getBand(RGBImageProfile.BLUE_BAND_NAME)).getExpression());
        assertEquals("", ((VirtualBand) p1.getBand(RGBImageProfile.ALPHA_BAND_NAME)).getExpression());
    }

    private Product createTestProduct() {
        final Product product = new Product("N", "T", 4, 4);
        product.addBand("U", ProductData.TYPE_FLOAT32);
        product.addBand("V", ProductData.TYPE_FLOAT32);
        product.addBand("W", ProductData.TYPE_FLOAT32);
        product.addBand("X", ProductData.TYPE_FLOAT32);
        product.addBand("Y", ProductData.TYPE_FLOAT32);
        product.addBand("Z", ProductData.TYPE_FLOAT32);
        return product;
    }


}
