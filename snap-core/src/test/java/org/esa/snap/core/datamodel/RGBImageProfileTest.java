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
package org.esa.snap.core.datamodel;

import com.bc.ceres.core.runtime.ConfigurationElement;
import org.junit.Test;

import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RGBImageProfileTest {

    @Test
    public void testDefaults() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        assertEquals("X", profile.getName());
        assertEquals(false, profile.isInternal());
        assertEquals("", profile.getRedExpression());
        assertEquals("", profile.getGreenExpression());
        assertEquals("", profile.getBlueExpression());
        assertEquals("", profile.getAlphaExpression());
        assertNotNull(profile.getRgbExpressions());
        assertEquals(3, profile.getRgbExpressions().length);
        assertNotNull(profile.getRgbaExpressions());
        assertEquals(4, profile.getRgbaExpressions().length);
        assertNull(profile.getPattern());
    }

    @Test
    public void testEqualsAndHashCode() {
        final RGBImageProfile profile1 = new RGBImageProfile("X", new String[]{"A", "B", "C"}, new String[]{
                "prod_type",
                "prod_name",
                "prod_desc"
        });
        final RGBImageProfile profile2 = new RGBImageProfile("X", new String[]{"A", "B", "C"}, new String[]{
                "prod_type",
                "prod_name",
                "prod_desc"
        });
        final RGBImageProfile profile3 = new RGBImageProfile("X", new String[]{"A", "B", "C"}, new String[]{
                "different_pattern",
                "diff",
                "diff"
        });
        final RGBImageProfile profile4 = new RGBImageProfile("Y", new String[]{"A", "B", "C"});
        final RGBImageProfile profile5 = new RGBImageProfile("X", new String[] {"A", "B", "V"});
        final RGBImageProfile profile6 = new RGBImageProfile("X", new String[] {"A", "B", "C", "D"});

        assertTrue(profile1.equals(profile1));
        assertFalse(profile1.equals(profile3));
        assertTrue(profile1.equals(profile2));
        assertFalse(profile1.equals(profile4));
        assertFalse(profile1.equals(profile5));
        assertFalse(profile1.equals(profile6));

        assertTrue(profile1.hashCode() == profile2.hashCode());
        assertTrue(profile1.hashCode() == profile3.hashCode());
        assertTrue(profile1.hashCode() == profile4.hashCode());
        assertTrue(profile1.hashCode() != profile5.hashCode());
        assertTrue(profile1.hashCode() == profile6.hashCode());
    }

    @Test
    public void testThatComponentsMustNotBeNull() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        try {
            profile.setRedExpression(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            profile.setGreenExpression(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            profile.setBlueExpression(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            profile.setAlphaExpression(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testComponentsAsArrays() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        profile.setRedExpression("radiance_1");
        profile.setGreenExpression("radiance_2");
        profile.setBlueExpression("radiance_4");
        profile.setAlphaExpression("l1_flags.LAND ? 0 : 1");

        final String[] rgbExpressions = profile.getRgbExpressions();
        assertNotNull(rgbExpressions);
        assertEquals(3, rgbExpressions.length);
        assertEquals("radiance_1", rgbExpressions[0]);
        assertEquals("radiance_2", rgbExpressions[1]);
        assertEquals("radiance_4", rgbExpressions[2]);

        final String[] rgbaExpressions = profile.getRgbaExpressions();
        assertNotNull(rgbaExpressions);
        assertEquals(4, rgbaExpressions.length);
        assertEquals("radiance_1", rgbaExpressions[0]);
        assertEquals("radiance_2", rgbaExpressions[1]);
        assertEquals("radiance_4", rgbaExpressions[2]);
        assertEquals("l1_flags.LAND ? 0 : 1", rgbaExpressions[3]);
    }

    @Test
    public void testApplicabilityOfEmptyProfile() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        assertEquals(false, profile.isApplicableTo(createTestProduct()));
    }

    @Test
    public void testApplicabilityIfAlphaComponentIsMissing() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        profile.setRedExpression("U+V");
        profile.setGreenExpression("V+W");
        profile.setBlueExpression("W+X");
        profile.setAlphaExpression("");
        assertEquals(true, profile.isApplicableTo(createTestProduct()));
    }

    @Test
    public void testApplicabilityIfOneComponentIsMissing() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        profile.setRedExpression("U+V");
        profile.setGreenExpression("");
        profile.setBlueExpression("W+X");
        profile.setAlphaExpression("Y+Z");
        assertEquals(true, profile.isApplicableTo(createTestProduct()));
    }

    @Test
    public void testApplicabilityIfUnknownBandIsUsed() {
        final RGBImageProfile profile = new RGBImageProfile("X");
        profile.setRedExpression("U+V");
        profile.setGreenExpression("V+K"); // unknown band K
        profile.setBlueExpression("W+X");
        profile.setAlphaExpression("");
        assertEquals(false, profile.isApplicableTo(createTestProduct()));
    }

    @Test
    public void testStoreRgbaExpressions() {
        final Product p1 = createTestProduct();
        RGBImageProfile.storeRgbaExpressions(p1, new String[]{"U", "V", "W", "X"});
        assertNotNull(p1.getBand(RGBImageProfile.RED_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.GREEN_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.BLUE_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.ALPHA_BAND_NAME));
    }

    @Test
    public void testStoreRgbaExpressionsWithoutAlpha() {
        final Product p1 = createTestProduct();
        RGBImageProfile.storeRgbaExpressions(p1, new String[]{"U", "V", "W", ""});
        assertNotNull(p1.getBand(RGBImageProfile.RED_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.GREEN_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.BLUE_BAND_NAME));
        assertNull(p1.getBand(RGBImageProfile.ALPHA_BAND_NAME));
    }

    @Test
    public void testStoreRgbaExpressionsWithoutGreen() {
        final Product p1 = createTestProduct();
        RGBImageProfile.storeRgbaExpressions(p1, new String[]{"U", "", "W", ""});
        assertNotNull(p1.getBand(RGBImageProfile.RED_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.GREEN_BAND_NAME));
        assertNotNull(p1.getBand(RGBImageProfile.BLUE_BAND_NAME));
        assertNull(p1.getBand(RGBImageProfile.ALPHA_BAND_NAME));

        assertEquals("0", ((VirtualBand) p1.getBand(RGBImageProfile.GREEN_BAND_NAME)).getExpression());
    }

    @Test
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

    @Test
    public void testConfigure_withoutPattern() throws Exception {
        RGBImageProfile profile = new RGBImageProfile();
        ConfigurationElement config = mock(ConfigurationElement.class);
        ConfigurationElement nameConfig = mock(ConfigurationElement.class);
        ConfigurationElement redConfig = mock(ConfigurationElement.class);
        ConfigurationElement greenConfig = mock(ConfigurationElement.class);
        ConfigurationElement blueConfig = mock(ConfigurationElement.class);

        when(nameConfig.getValue()).thenReturn("test_name");

        when(redConfig.getValue()).thenReturn("radiance_12");
        when(greenConfig.getValue()).thenReturn("radiance_6");
        when(blueConfig.getValue()).thenReturn("radiance_2");


        when(config.getChild("name")).thenReturn(nameConfig);
        when(config.getChild("red")).thenReturn(redConfig);
        when(config.getChild("green")).thenReturn(greenConfig);
        when(config.getChild("blue")).thenReturn(blueConfig);
        when(config.getChild("alpha")).thenReturn(null);

        profile.configure(config);

        assertEquals("test_name", profile.getName());
        assertEquals("", profile.getAlphaExpression());
        assertEquals("radiance_2", profile.getBlueExpression());
        assertEquals("radiance_12", profile.getRedExpression());
        assertEquals("radiance_6", profile.getGreenExpression());
        assertNull(profile.getPattern());
    }

    @Test
    public void testConfigure() throws Exception {
        RGBImageProfile profile = new RGBImageProfile();
        ConfigurationElement config = mock(ConfigurationElement.class);
        ConfigurationElement nameConfig = mock(ConfigurationElement.class);
        ConfigurationElement redConfig = mock(ConfigurationElement.class);
        ConfigurationElement greenConfig = mock(ConfigurationElement.class);
        ConfigurationElement blueConfig = mock(ConfigurationElement.class);
        ConfigurationElement patternConfig = mock(ConfigurationElement.class);
        ConfigurationElement productTypeConfig = mock(ConfigurationElement.class);
        ConfigurationElement productNameConfig = mock(ConfigurationElement.class);
        ConfigurationElement productDescConfig = mock(ConfigurationElement.class);

        when(nameConfig.getValue()).thenReturn("test_name");

        when(redConfig.getValue()).thenReturn("radiance_12");
        when(greenConfig.getValue()).thenReturn("radiance_6");
        when(blueConfig.getValue()).thenReturn("radiance_2");

        when(productTypeConfig.getValue()).thenReturn("MER_*_1*");
        when(productNameConfig.getValue()).thenReturn("ATS_*_1*");
        when(productDescConfig.getValue()).thenReturn("");

        when(config.getChild("name")).thenReturn(nameConfig);
        when(config.getChild("red")).thenReturn(redConfig);
        when(config.getChild("green")).thenReturn(greenConfig);
        when(config.getChild("blue")).thenReturn(blueConfig);
        when(config.getChild("alpha")).thenReturn(null);
        when(config.getChild("pattern")).thenReturn(patternConfig);
        when(patternConfig.getChild("productType")).thenReturn(productTypeConfig);
        when(patternConfig.getChild("productName")).thenReturn(productNameConfig);
        when(patternConfig.getChild("productDesc")).thenReturn(productDescConfig);

        profile.configure(config);

        assertEquals("test_name", profile.getName());
        assertEquals("", profile.getAlphaExpression());
        assertEquals("radiance_2", profile.getBlueExpression());
        assertEquals("radiance_12", profile.getRedExpression());
        assertEquals("radiance_6", profile.getGreenExpression());
        assertTrue(Arrays.equals(new String[]{"MER_*_1*", "ATS_*_1*", ""}, profile.getPattern()));
    }

    @Test
    public void testSetProperties() throws Exception {
        RGBImageProfile profile = new RGBImageProfile();
        Properties properties = new Properties();
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_NAME, "name");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_ALPHA, "alpha");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_BLUE, "blue");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_GREEN, "green");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_RED, "red");
        properties.setProperty(RGBImageProfile.PROPERTY_KEY_INTERNAL, String.valueOf(true));
        profile.setProperties(properties);

        assertEquals("name", profile.getName());
        assertTrue(profile.isInternal());
        assertTrue(Arrays.equals(new String[] {"red", "green", "blue", "alpha"}, profile.getRgbaExpressions()));
    }
}
