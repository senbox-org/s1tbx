/*
 * $Id: SmileAuxDataTest.java,v 1.3 2006/11/15 09:22:13 norman Exp $
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
 *//*
 * $Id: SmileAuxDataTest.java,v 1.3 2006/11/15 09:22:13 norman Exp $
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
package org.esa.beam.processor.smile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;

public class SmileAuxDataTest extends TestCase {

    SmileAuxData _rrData;
    SmileAuxData _frData;

    /**
     * Constructs the test case
     */
    public SmileAuxDataTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        new SmileProcessor().installAuxdata(); // just to extract auxdata
        _rrData = SmileAuxData.loadRRAuxData();
        _frData = SmileAuxData.loadFRAuxData();
    }

    /**
     * Exports the test class to framework
     */
    public static Test suite() {
        return new TestSuite(SmileAuxDataTest.class);
    }

    public void testThatDataIsReloadedWithoutUsingStaticArrayInstances() {
        SmileAuxData rrData = null;
        try {
            rrData = SmileAuxData.loadRRAuxData();
        } catch (IOException e) {
            fail("IOException not expected: " + e.getMessage());
        }
        assertNotSame(rrData.getRadCorrFlagsLand(), _rrData.getRadCorrFlagsLand());
        assertNotSame(rrData.getRadCorrFlagsWater(), _rrData.getRadCorrFlagsWater());
        assertNotSame(rrData.getLowerBandIndexesLand(), _rrData.getLowerBandIndexesLand());
        assertNotSame(rrData.getLowerBandIndexesWater(), _rrData.getLowerBandIndexesWater());
        assertNotSame(rrData.getUpperBandIndexesLand(), _rrData.getUpperBandIndexesLand());
        assertNotSame(rrData.getUpperBandIndexesWater(), _rrData.getUpperBandIndexesWater());
        assertNotSame(rrData.getTheoreticalWavelengths(), _rrData.getTheoreticalWavelengths());
        assertNotSame(rrData.getTheoreticalSunSpectralFluxes(), _rrData.getTheoreticalSunSpectralFluxes());
        assertNotSame(rrData.getDetectorWavelengths(), _rrData.getDetectorWavelengths());
        assertNotSame(rrData.getDetectorSunSpectralFluxes(), _rrData.getDetectorSunSpectralFluxes());
    }

    public void testCurrentRrData() {
        //       	band    switch_land	lower_land	upper_land	switch_water	lower_water	upper_water	lam_theo	E0_theo
        //         	12  0	13	14	1	13	14	865	958.885498
        SmileAuxData data = _rrData;
        assertEquals(false, data.getRadCorrFlagsLand()[12]);
        assertEquals(13 - 1, data.getLowerBandIndexesLand()[12]);
        assertEquals(14 - 1, data.getUpperBandIndexesLand()[12]);
        assertEquals(true, data.getRadCorrFlagsWater()[12]);
        assertEquals(13 - 1, data.getLowerBandIndexesWater()[12]);
        assertEquals(14 - 1, data.getUpperBandIndexesWater()[12]);
        assertEquals(865.0, data.getTheoreticalWavelengths()[12], 1.0e-6);
        assertEquals(958.885498, data.getTheoreticalSunSpectralFluxes()[12], 1.0e-6);

        assertEquals(490.0209579, data.getDetectorWavelengths()[20][2], 1.0e-6);
        assertEquals(1929.29938966317, data.getDetectorSunSpectralFluxes()[20][2], 1.0e-10);
    }

    public void testCurrentFrData() {
        //        band	switch_land	lower_land	upper_land	switch_water	lower_water	upper_water	lam_theo	E0_theo
        //        2	1	2	4	1	2	4	490	1929.325562
        SmileAuxData data = _frData;
        assertEquals(true, data.getRadCorrFlagsLand()[2]);
        assertEquals(2 - 1, data.getLowerBandIndexesLand()[2]);
        assertEquals(4 - 1, data.getUpperBandIndexesLand()[2]);
        assertEquals(true, data.getRadCorrFlagsWater()[2]);
        assertEquals(2 - 1, data.getLowerBandIndexesWater()[2]);
        assertEquals(4 - 1, data.getUpperBandIndexesWater()[2]);
        assertEquals(490.0, data.getTheoreticalWavelengths()[2], 1.0e-6);
        assertEquals(1929.325562, data.getTheoreticalSunSpectralFluxes()[2], 1.0e-6);

        assertEquals(490.1104498, data.getDetectorWavelengths()[20][2], 1.0e-6);
        assertEquals(1930.1682671039, data.getDetectorSunSpectralFluxes()[20][2], 1.0e-10);
    }
}
