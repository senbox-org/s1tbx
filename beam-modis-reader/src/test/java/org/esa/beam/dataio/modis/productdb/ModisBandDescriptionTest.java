/*
 * $Id: ModisBandDescriptionTest.java,v 1.1 2006/09/19 07:00:04 marcop Exp $
 *
 * Copyright (C) 2002,2003  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the 
 * Free Software Foundation. This program is distributed in the hope it will 
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 */
package org.esa.beam.dataio.modis.productdb;

import junit.framework.TestCase;

public class ModisBandDescriptionTest extends TestCase {

    public void testTheFunctionality() {
        String expName = "band_name";
        String expSpectral = "true";
        String expScaleMethod = "scale_method";
        String expScale = "scale_name";
        String expOffset = "offsetName";
        String expUnit = "unit_name";
        String expBandNames = "band_names";
        String expDescName = "band_description";

        // all value null allowed
        ModisBandDescription desc = new ModisBandDescription(null, null, null, null, null, null, null, null);
        assertEquals(null, desc.getName());
        assertEquals(false, desc.isSpectral());
        assertEquals(null, desc.getScalingMethod());
        assertEquals(null, desc.getScaleAttribName());
        assertEquals(null, desc.getOffsetAttribName());
        assertEquals(null, desc.getUnitAttribName());
        assertEquals(null, desc.getBandAttribName());
        assertEquals(null, desc.getDescriptionAttribName());

        // check the values one by one
        desc = new ModisBandDescription(expName, null, null, null, null, null, null, null);
        assertEquals(expName, desc.getName());
        assertEquals(false, desc.isSpectral());
        assertEquals(null, desc.getScalingMethod());
        assertEquals(null, desc.getScaleAttribName());
        assertEquals(null, desc.getOffsetAttribName());
        assertEquals(null, desc.getUnitAttribName());
        assertEquals(null, desc.getBandAttribName());
        assertEquals(null, desc.getDescriptionAttribName());

        desc = new ModisBandDescription(null, expSpectral, null, null, null, null, null, null);
        assertEquals(null, desc.getName());
        assertEquals(true, desc.isSpectral());
        assertEquals(null, desc.getScalingMethod());
        assertEquals(null, desc.getScaleAttribName());
        assertEquals(null, desc.getOffsetAttribName());
        assertEquals(null, desc.getUnitAttribName());
        assertEquals(null, desc.getBandAttribName());
        assertEquals(null, desc.getDescriptionAttribName());

        desc = new ModisBandDescription(null, null, expScaleMethod, null, null, null, null, null);
        assertEquals(null, desc.getName());
        assertEquals(false, desc.isSpectral());
        assertEquals(expScaleMethod, desc.getScalingMethod());
        assertEquals(null, desc.getScaleAttribName());
        assertEquals(null, desc.getOffsetAttribName());
        assertEquals(null, desc.getUnitAttribName());
        assertEquals(null, desc.getBandAttribName());
        assertEquals(null, desc.getDescriptionAttribName());

        desc = new ModisBandDescription(null, null, null, expScale, null, null, null, null);
        assertEquals(null, desc.getName());
        assertEquals(false, desc.isSpectral());
        assertEquals(null, desc.getScalingMethod());
        assertEquals(expScale, desc.getScaleAttribName());
        assertEquals(null, desc.getOffsetAttribName());
        assertEquals(null, desc.getUnitAttribName());
        assertEquals(null, desc.getBandAttribName());
        assertEquals(null, desc.getDescriptionAttribName());

        desc = new ModisBandDescription(null, null, null, null, expOffset, null, null, null);
        assertEquals(null, desc.getName());
        assertEquals(false, desc.isSpectral());
        assertEquals(null, desc.getScalingMethod());
        assertEquals(null, desc.getScaleAttribName());
        assertEquals(expOffset, desc.getOffsetAttribName());
        assertEquals(null, desc.getUnitAttribName());
        assertEquals(null, desc.getBandAttribName());
        assertEquals(null, desc.getDescriptionAttribName());

        desc = new ModisBandDescription(null, null, null, null, null, expUnit, null, null);
        assertEquals(null, desc.getName());
        assertEquals(false, desc.isSpectral());
        assertEquals(null, desc.getScalingMethod());
        assertEquals(null, desc.getScaleAttribName());
        assertEquals(null, desc.getOffsetAttribName());
        assertEquals(expUnit, desc.getUnitAttribName());
        assertEquals(null, desc.getBandAttribName());
        assertEquals(null, desc.getDescriptionAttribName());

        desc = new ModisBandDescription(null, null, null, null, null, null, expBandNames, null);
        assertEquals(null, desc.getName());
        assertEquals(false, desc.isSpectral());
        assertEquals(null, desc.getScalingMethod());
        assertEquals(null, desc.getScaleAttribName());
        assertEquals(null, desc.getOffsetAttribName());
        assertEquals(null, desc.getUnitAttribName());
        assertEquals(expBandNames, desc.getBandAttribName());
        assertEquals(null, desc.getDescriptionAttribName());

        desc = new ModisBandDescription(null, null, null, null, null, null, null, expDescName);
        assertEquals(null, desc.getName());
        assertEquals(false, desc.isSpectral());
        assertEquals(null, desc.getScalingMethod());
        assertEquals(null, desc.getScaleAttribName());
        assertEquals(null, desc.getOffsetAttribName());
        assertEquals(null, desc.getUnitAttribName());
        assertEquals(null, desc.getBandAttribName());
        assertEquals(expDescName, desc.getDescriptionAttribName());
    }
}
