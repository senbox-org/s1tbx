/*
 * $Id: AtsrGSSTConstantsTest.java,v 1.1 2006/09/12 13:19:07 marcop Exp $
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
package org.esa.beam.dataio.atsr;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AtsrGSSTConstantsTest extends TestCase {

    public AtsrGSSTConstantsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AtsrGSSTConstantsTest.class);
    }

    public void testBandNames() {
        assertEquals("nadir_view_sst", AtsrGSSTConstants.NADIR_SST_NAME);
        assertEquals("dual_view_sst", AtsrGSSTConstants.DUAL_SST_NAME);
        assertEquals("x_offs_nadir", AtsrGSSTConstants.NADIR_X_OFFS_NAME);
        assertEquals("y_offs_nadir", AtsrGSSTConstants.NADIR_Y_OFFS_NAME);
        assertEquals("x_offs_fward", AtsrGSSTConstants.FORWARD_X_OFFS_NAME);
        assertEquals("y_offs_fward", AtsrGSSTConstants.FORWARD_Y_OFFS_NAME);
    }

    public void testUnits() {
        assertEquals("K", AtsrGSSTConstants.SST_UNIT);
        assertEquals("km", AtsrGSSTConstants.COORDINATE_OFFSET_UNIT);
    }

    // @todo 3 nf/tb - remove and avoid tests checking values not used in the system's logic
    public void testDescriptions() {
        assertEquals("Nadir-only sea-surface temperature", AtsrGSSTConstants.NADIR_SST_DESCRIPTION);
        assertEquals("Dual-view sea-surface temperature", AtsrGSSTConstants.DUAL_SST_DESCRIPTION);
        assertEquals("X coordinate offsets (across-track) of nadir view pixels",
                     AtsrGSSTConstants.NADIR_X_OFFS_DESCRIPTION);
        assertEquals("Y coordinate offsets (along-track) of nadir view pixels",
                     AtsrGSSTConstants.NADIR_Y_OFFS_DESCRIPTION);
        assertEquals("X coordinate offsets (across-track) of forward view pixels",
                     AtsrGSSTConstants.FORWARD_X_OFFS_DESCRIPTION);
        assertEquals("Y coordinate offsets (along-track) of forward view pixels",
                     AtsrGSSTConstants.FORWARD_Y_OFFS_DESCRIPTION);
    }


    // @todo 3 nf/tb - remove and avoid tests checking values not used in the system's logic
    public void testFlagsConstants() {

        assertEquals("confid_flags", AtsrGSSTConstants.CONFIDENCE_FLAGS_NAME);

        assertEquals("NADIR_SST_VALID", AtsrGSSTConstants.NADIR_SST_VALID_FLAG_NAME);
        assertEquals(0x1, AtsrGSSTConstants.NADIR_SST_VALID_FLAG_MASK);
        assertEquals(
                "Nadir-only sea-surface temperature is valid (if not set, pixel contains nadir-view 11 um brightness temperature",
                AtsrGSSTConstants.NADIR_SST_VALID_FLAG_DESCRIPTION);

        assertEquals("NADIR_SST_37_INCLUDED", AtsrGSSTConstants.NADIR_SST_37_FLAG_NAME);
        assertEquals(0x2, AtsrGSSTConstants.NADIR_SST_37_FLAG_MASK);
        assertEquals(
                "Nadir-only sea-surface temperature retrieval includes 3.7 um channel (if not set, retrieval includes 12 um and 11 um only)",
                AtsrGSSTConstants.NADIR_SST_37_FLAG_DESCRIPTION);

        assertEquals("DUAL_SST_VALID", AtsrGSSTConstants.DUAL_SST_VALID_FLAG_NAME);
        assertEquals(0x4, AtsrGSSTConstants.DUAL_SST_VALID_FLAG_MASK);
        assertEquals(
                "Dual-view sea-surface temperature is valid (if not set, pixel contains nadir-view 11 um brightness temperature",
                AtsrGSSTConstants.DUAL_SST_VALID_FLAG_DESCRIPTION);

        assertEquals("DUAL_SST_37_INCLUDED", AtsrGSSTConstants.DUAL_SST_37_FLAG_NAME);
        assertEquals(0x8, AtsrGSSTConstants.DUAL_SST_37_FLAG_MASK);
        assertEquals(
                "Dual-view sea-surface temperature retrieval includes 3.7 um channel (if not set, retrieval includes 12 um and 11 um only)",
                AtsrGSSTConstants.DUAL_SST_37_FLAG_DESCRIPTION);

        assertEquals("LAND", AtsrGSSTConstants.LAND_FLAG_NAME);
        assertEquals(0x10, AtsrGSSTConstants.LAND_FLAG_MASK);
        assertEquals("Pixel is over land", AtsrGSSTConstants.LAND_FLAG_DESCRIPTION);

        assertEquals("NADIR_CLOUDY", AtsrGSSTConstants.NADIR_CLOUDY_FLAG_NAME);
        assertEquals(0x20, AtsrGSSTConstants.NADIR_CLOUDY_FLAG_MASK);
        assertEquals("Nadir-view pixel is cloudy", AtsrGSSTConstants.NADIR_CLOUDY_FLAG_DESCRIPTION);

        assertEquals("NADIR_BLANKING", AtsrGSSTConstants.NADIR_BLANKING_FLAG_NAME);
        assertEquals(0x40, AtsrGSSTConstants.NADIR_BLANKING_FLAG_MASK);
        assertEquals("Nadir-view pixel has blanking-pulse", AtsrGSSTConstants.NADIR_BLANKING_FLAG_DESCRIPTION);

        assertEquals("NADIR_COSMETIC", AtsrGSSTConstants.NADIR_COSMETIC_FLAG_NAME);
        assertEquals(0x80, AtsrGSSTConstants.NADIR_COSMETIC_FLAG_MASK);
        assertEquals("Nadir-view pixel is cosmetic (nearest-neighbour fill)",
                     AtsrGSSTConstants.NADIR_COSMETIC_FLAG_DESCRIPTION);

        assertEquals("FWARD_CLOUDY", AtsrGSSTConstants.FORWARD_CLOUDY_FLAG_NAME);
        assertEquals(0x100, AtsrGSSTConstants.FORWARD_CLOUDY_FLAG_MASK);
        assertEquals("Forward-view pixel is cloudy", AtsrGSSTConstants.FORWARD_CLOUDY_FLAG_DESCRIPTION);

        assertEquals("FWARD_BLANKING", AtsrGSSTConstants.FORWARD_BLANKING_FLAG_NAME);
        assertEquals(0x200, AtsrGSSTConstants.FORWARD_BLANKING_FLAG_MASK);
        assertEquals("Forward-view pixel has blanking-pulse", AtsrGSSTConstants.FORWARD_BLANKING_FLAG_DESCRIPTION);

        assertEquals("FWARD_COSMETIC", AtsrGSSTConstants.FORWARD_COSMETIC_FLAG_NAME);
        assertEquals(0x400, AtsrGSSTConstants.FORWARD_COSMETIC_FLAG_MASK);
        assertEquals("Forward-view pixel is cosmetic (nearest-neighbour fill)",
                     AtsrGSSTConstants.FORWARD_COSMETIC_FLAG_DESCRIPTION);
    }
}
