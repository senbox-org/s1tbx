/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.datamodel.PlainFeatureFactory;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

import static org.junit.Assert.*;

/**
 *
 * @author Tonio Fincke
 * @author Thomas Storm
 */
public class SimpleFeatureFigureFactoryTest {

    private SimpleFeature simpleFeature;

    @Before
    public void setUp() throws Exception {
        final SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
        sftb.setName("someName");
        sftb.add(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS, String.class);
        SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(sftb.buildFeatureType());
        simpleFeature = sfb.buildFeature("someId");
    }

    @Test
    public void testGetStyleCss_WithAdditionalStyles() throws Exception {
        simpleFeature.setAttribute(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS, "fill:120,120,120;stroke:0,10,0");
        final String styleCss = SimpleFeatureFigureFactory.getStyleCss(simpleFeature, "symbol:pin");

        assertEquals("fill:120,120,120;stroke:0,10,0;symbol:pin", styleCss);
    }

    @Test
    public void testGetStyleCss_WithOverride() throws Exception {
        simpleFeature.setAttribute(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS, "symbol:cross;fill:100,100,100");
        final String styleCss = SimpleFeatureFigureFactory.getStyleCss(simpleFeature, "symbol:pin;fill:0,0,0");

        assertEquals("symbol:cross;fill:100,100,100", styleCss);
    }

    @Test
    public void testGetStyleCss_WithOverrideAndDefaultValue() throws Exception {
        simpleFeature.setAttribute(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS, "fill:120,120,120;stroke:0,10,0");
        final String styleCss = SimpleFeatureFigureFactory.getStyleCss(simpleFeature, "symbol:pin;fill:0,0,0");

        assertEquals("fill:120,120,120;stroke:0,10,0;symbol:pin", styleCss);
    }

    @Test
    public void testGetStyleCss_KeepDefault() throws Exception {
        simpleFeature.setAttribute(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS, "");
        final String styleCss = SimpleFeatureFigureFactory.getStyleCss(simpleFeature, "symbol:pin;fill:0,0,0");

        assertEquals("symbol:pin;fill:0,0,0", styleCss);
    }

    @Test
    public void testGetStyleCss_EmptyDefault() throws Exception {
        simpleFeature.setAttribute(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS, "symbol:cross;fill:100,100,100");
        final String styleCss = SimpleFeatureFigureFactory.getStyleCss(simpleFeature, "");

        assertEquals("symbol:cross;fill:100,100,100", styleCss);
    }
}
