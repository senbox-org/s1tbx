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
package org.esa.beam.dataio.atsr;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AtsrGBTConstantsTest extends TestCase {

    public AtsrGBTConstantsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AtsrGBTConstantsTest.class);
    }

    public void testBandNames() {
        assertEquals("btemp_nadir_1200", AtsrGBTConstants.NADIR_1200_BT_NAME);
        assertEquals("btemp_nadir_1100", AtsrGBTConstants.NADIR_1100_BT_NAME);
        assertEquals("btemp_nadir_370", AtsrGBTConstants.NADIR_370_BT_NAME);
        assertEquals("btemp_fward_1200", AtsrGBTConstants.FORWARD_1200_BT_NAME);
        assertEquals("btemp_fward_1100", AtsrGBTConstants.FORWARD_1100_BT_NAME);
        assertEquals("btemp_fward_370", AtsrGBTConstants.FORWARD_370_BT_NAME);
        assertEquals("reflec_nadir_1600", AtsrGBTConstants.NADIR_1600_REF_NAME);
        assertEquals("reflec_nadir_870", AtsrGBTConstants.NADIR_870_REF_NAME);
        assertEquals("reflec_nadir_650", AtsrGBTConstants.NADIR_650_REF_NAME);
        assertEquals("reflec_nadir_550", AtsrGBTConstants.NADIR_550_REF_NAME);
        assertEquals("reflec_fward_1600", AtsrGBTConstants.FORWARD_1600_REF_NAME);
        assertEquals("reflec_fward_870", AtsrGBTConstants.FORWARD_870_REF_NAME);
        assertEquals("reflec_fward_650", AtsrGBTConstants.FORWARD_650_REF_NAME);
        assertEquals("reflec_fward_550", AtsrGBTConstants.FORWARD_550_REF_NAME);
        assertEquals("x_offs_nadir", AtsrGBTConstants.NADIR_X_OFFS_NAME);
        assertEquals("y_offs_nadir", AtsrGBTConstants.NADIR_Y_OFFS_NAME);
        assertEquals("x_offs_fward", AtsrGBTConstants.FORWARD_X_OFFS_NAME);
        assertEquals("y_offs_fward", AtsrGBTConstants.FORWARD_Y_OFFS_NAME);
    }

    public void testDescriptions() {
        // @todo 3 nf/tb - remove and avoid tests checking values not used in the system's logic
        assertEquals("Nadir-view 12.0um brightness temperature image", AtsrGBTConstants.NADIR_1200_BT_DESCRIPTION);
        assertEquals("Nadir-view 11.0um brightness temperature image", AtsrGBTConstants.NADIR_1100_BT_DESCRIPTION);
        assertEquals("Nadir-view 3.7um brightness temperature image", AtsrGBTConstants.NADIR_370_BT_DESCRIPTION);
        assertEquals("Forward-view 12.0um brightness temperature image", AtsrGBTConstants.FORWARD_1200_BT_DESCRIPTION);
        assertEquals("Forward-view 11.0um brightness temperature image", AtsrGBTConstants.FORWARD_1100_BT_DESCRIPTION);
        assertEquals("Forward-view 3.7um brightness temperature image", AtsrGBTConstants.FORWARD_370_BT_DESCRIPTION);
        assertEquals("Nadir-view 1.6um reflectance image", AtsrGBTConstants.NADIR_1600_REF_DESCRIPTION);
        assertEquals("Nadir-view 0.87um reflectance image", AtsrGBTConstants.NADIR_870_REF_DESCRIPTION);
        assertEquals("Nadir-view 0.65um reflectance image", AtsrGBTConstants.NADIR_650_REF_DESCRIPTION);
        assertEquals("Nadir-view 0.55um reflectance image", AtsrGBTConstants.NADIR_550_REF_DESCRIPTION);
        assertEquals("Forward-view 1.6um reflectance image", AtsrGBTConstants.FORWARD_1600_REF_DESCRIPTION);
        assertEquals("Forward-view 0.87um reflectance image", AtsrGBTConstants.FORWARD_870_REF_DESCRIPTION);
        assertEquals("Forward-view 0.65um reflectance image", AtsrGBTConstants.FORWARD_650_REF_DESCRIPTION);
        assertEquals("Forward-view 0.55um reflectance image", AtsrGBTConstants.FORWARD_550_REF_DESCRIPTION);
        assertEquals("X coordinate offsets (across-track) of nadir view pixels",
                     AtsrGBTConstants.NADIR_X_OFFS_DESCRIPTION);
        assertEquals("Y coordinate offsets (along-track) of nadir view pixels",
                     AtsrGBTConstants.NADIR_Y_OFFS_DESCRIPTION);
        assertEquals("X coordinate offsets (across-track) of forward view pixels",
                     AtsrGBTConstants.FORWARD_X_OFFS_DESCRIPTION);
        assertEquals("Y coordinate offsets (along-track) of forward view pixels",
                     AtsrGBTConstants.FORWARD_Y_OFFS_DESCRIPTION);

    }

    public void testUnits() {
        assertEquals("K", AtsrGBTConstants.BRIGHTNESS_TEMPERATURE_UNIT);
        assertEquals("%", AtsrGBTConstants.REFLECTANCE_UNIT);
        assertEquals("km", AtsrGBTConstants.COORDINATE_OFFSET_UNIT);
    }


}
