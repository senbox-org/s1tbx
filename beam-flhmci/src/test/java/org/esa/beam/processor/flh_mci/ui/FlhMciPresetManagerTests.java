/*
 * $Id: FlhMciPresetManagerTests.java,v 1.1.1.1 2006/09/11 08:16:52 norman Exp $
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
package org.esa.beam.processor.flh_mci.ui;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.esa.beam.processor.flh_mci.FlhMciConstants;

public class FlhMciPresetManagerTests extends TestCase {

    public FlhMciPresetManagerTests(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(FlhMciPresetManagerTests.class);
    }

    /**
     * Tests the correct functionality of the singleton interface
     */
    public void testSingletonInterface() {
        FlhMciPresetManager manager1;
        FlhMciPresetManager manager2;

        // when calling get instance, we expect a valid reference to be returned
        manager1 = FlhMciPresetManager.getInstance();
        assertNotNull(manager1);

        // when calling once again, we expect to get the same reference
        manager2 = FlhMciPresetManager.getInstance();
        assertNotNull(manager2);
        assertEquals(manager1, manager2);
    }


    /**
     * Tests that the preset by name interface is working correctly
     */
    public void testGetPresetByNameInterface() {
        FlhMciPresetManager manager = FlhMciPresetManager.getInstance();
        assertNotNull(manager);
        FlhMciPreset preset;

        // null is not allowed as argument
        try {
            preset = manager.getPresetByName(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // check all valid preset names - must return something
        preset = manager.getPresetByName(FlhMciConstants.PRESET_PARAM_VALUE_SET[0]);
        assertNotNull(preset);
        preset = manager.getPresetByName(FlhMciConstants.PRESET_PARAM_VALUE_SET[1]);
        assertNotNull(preset);
        preset = manager.getPresetByName(FlhMciConstants.PRESET_PARAM_VALUE_SET[2]);
        assertNotNull(preset);

        // invalid name - must return null
        preset = manager.getPresetByName("veryInvalidPresetName");
        assertNull(preset);
    }

    /**
     * Tests that the content of preset 1 is correct
     */
    public void testGetPresetOne() {
        FlhMciPresetManager manager = FlhMciPresetManager.getInstance();
        assertNotNull(manager);
        FlhMciPreset preset = manager.getPresetByName(FlhMciConstants.PRESET_PARAM_VALUE_SET[0]);
        assertNotNull(preset);

        String expLowBand = "reflec_7";
        String expHighBand = "reflec_9";
        String expSignalBand = "reflec_8";
        String expLineheightBand = "FLH";
        String expSlopeBand = "FLH_SLOPE";
        String expBitmask = "l2_flags.WATER";

        assertEquals(expLowBand, preset.getLowBandName());
        assertEquals(expHighBand, preset.getHighBandName());
        assertEquals(expSignalBand, preset.getSignalBandName());
        assertEquals(expLineheightBand, preset.getLineheightBandName());
        assertEquals(expSlopeBand, preset.getSlopeBandName());
        assertEquals(expBitmask, preset.getBitmaskExpression());
    }

    /**
     * Tests that the content of preset 2 is correct
     */
    public void testGetPresetTwo() {
        FlhMciPresetManager manager = FlhMciPresetManager.getInstance();
        assertNotNull(manager);
        FlhMciPreset preset = manager.getPresetByName(FlhMciConstants.PRESET_PARAM_VALUE_SET[1]);
        assertNotNull(preset);

        String expLowBand = "radiance_8";
        String expHighBand = "radiance_10";
        String expSignalBand = "radiance_9";
        String expLineheightBand = "MCI";
        String expSlopeBand = "MCI_SLOPE";
        String expBitmask = "NOT l1_flags.LAND_OCEAN AND NOT l1_flags.BRIGHT AND NOT l1_flags.INVALID";

        assertEquals(expLowBand, preset.getLowBandName());
        assertEquals(expHighBand, preset.getHighBandName());
        assertEquals(expSignalBand, preset.getSignalBandName());
        assertEquals(expLineheightBand, preset.getLineheightBandName());
        assertEquals(expSlopeBand, preset.getSlopeBandName());
        assertEquals(expBitmask, preset.getBitmaskExpression());
    }

    /**
     * Tests that the content of preset 3 is correct
     */
    public void testGetPresetThree() {
        FlhMciPresetManager manager = FlhMciPresetManager.getInstance();
        assertNotNull(manager);
        FlhMciPreset preset = manager.getPresetByName(FlhMciConstants.PRESET_PARAM_VALUE_SET[2]);
        assertNotNull(preset);

        String expLowBand = "reflec_8";
        String expHighBand = "reflec_10";
        String expSignalBand = "reflec_9";
        String expLineheightBand = "MCI";
        String expSlopeBand = "MCI_SLOPE";
        String expBitmask = "l2_flags.WATER";

        assertEquals(expLowBand, preset.getLowBandName());
        assertEquals(expHighBand, preset.getHighBandName());
        assertEquals(expSignalBand, preset.getSignalBandName());
        assertEquals(expLineheightBand, preset.getLineheightBandName());
        assertEquals(expSlopeBand, preset.getSlopeBandName());
        assertEquals(expBitmask, preset.getBitmaskExpression());
    }

    /**
     * Tests the functionality of getPresetName
     */
    public void testGetPresetName() {
        FlhMciPresetManager manager = FlhMciPresetManager.getInstance();
        assertNotNull(manager);
        FlhMciPreset preset_0 = null;
        FlhMciPreset preset_1 = null;
        FlhMciPreset preset_2 = null;
        FlhMciPreset preset_3 = null;
        String presetName = null;

        // method shall not accept null
        try {
            manager.getPresetName(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // when feeding the correct preset we expect to get the correct name
        preset_0 = manager.getPresetByName(FlhMciConstants.PRESET_PARAM_VALUE_SET[0]);
        assertNotNull(preset_0);
        presetName = manager.getPresetName(preset_0);
        assertNotNull(presetName);
        assertEquals(FlhMciConstants.PRESET_PARAM_VALUE_SET[0], presetName);

        preset_1 = manager.getPresetByName(FlhMciConstants.PRESET_PARAM_VALUE_SET[1]);
        assertNotNull(preset_1);
        presetName = manager.getPresetName(preset_1);
        assertNotNull(presetName);
        assertEquals(FlhMciConstants.PRESET_PARAM_VALUE_SET[1], presetName);

        preset_2 = manager.getPresetByName(FlhMciConstants.PRESET_PARAM_VALUE_SET[2]);
        assertNotNull(preset_2);
        presetName = manager.getPresetName(preset_2);
        assertNotNull(presetName);
        assertEquals(FlhMciConstants.PRESET_PARAM_VALUE_SET[2], presetName);

        // modify third one
        preset_3 = manager.getPresetByName(FlhMciConstants.PRESET_PARAM_VALUE_SET[2]);
        assertNotNull(preset_3);
        preset_3.setSignalBandName("some name");
        preset_3.setLowBandName("some other name");
        presetName = manager.getPresetName(preset_3);
        assertNotNull(presetName);
        assertEquals(FlhMciConstants.PRESET_PARAM_VALUE_SET[3], presetName);
    }
}
