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

package org.esa.snap.core.gpf.common.reproject;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marco Peters
 * @since BEAM 4.7
 */
public class FailureCasesReprojectionOpTest extends AbstractReprojectionOpTest {

    @Test(expected = OperatorException.class)
    public void testEmptyParameterMap() {
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testParameterAmbigouity_wkt_wktFile() {
        parameterMap.put("crs", UTM33N_WKT);
        parameterMap.put("wktFile", wktFile);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testParameterAmbigouity_wkt_collocateProduct() {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("source", sourceProduct);
        productMap.put("collocateWith", sourceProduct);
        parameterMap.put("crs", UTM33N_WKT);
        createReprojectedProduct(productMap);
    }

    @Test(expected = OperatorException.class)
    public void testUnknownResamplingMethode() {
        parameterMap.put("resamplingName", "Super_Duper_Resampling");
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testMissingPixelSizeY() {
        parameterMap.put("pixelSizeX", 0.024);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testMissingPixelSizeX() {
        parameterMap.put("pixelSizeY", 0.024);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testMissingReferencingPixelX() {
        parameterMap.put("referencePixelY", 0.5);
        parameterMap.put("easting", 1234.5);
        parameterMap.put("northing", 1234.5);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testMissingReferencingpixelY() {
        parameterMap.put("referencePixelX", 0.5);
        parameterMap.put("easting", 1234.5);
        parameterMap.put("northing", 1234.5);
        createReprojectedProduct();
    }


    @Test(expected = OperatorException.class)
    public void testMissingReferencingNorthing() {
        parameterMap.put("referencePixelX", 0.5);
        parameterMap.put("referencePixelY", 0.5);
        parameterMap.put("easting", 1234.5);
        createReprojectedProduct();
    }

    @Test(expected = OperatorException.class)
    public void testMissingReferencingEasting() {
        parameterMap.put("referencePixelX", 0.5);
        parameterMap.put("referencePixelY", 0.5);
        parameterMap.put("northing", 1234.5);
        createReprojectedProduct();
    }
}
