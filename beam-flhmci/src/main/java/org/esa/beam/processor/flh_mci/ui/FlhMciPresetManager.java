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
package org.esa.beam.processor.flh_mci.ui;

import java.util.HashMap;
import java.util.Map;

import org.esa.beam.processor.flh_mci.FlhMciConstants;
import org.esa.beam.util.Guardian;

/**
 * This class supports the FlhMciUI in terms of preset handling. It contains all default preset parameter and can
 * generate presets by a given name or return a preset name for a given FlhMciPreset object.
 * <p/>
 * The manager is implemented as a singleton.
 */
@Deprecated
public class FlhMciPresetManager {

    private final Map<String, FlhMciPreset> _presetMap;
    private static final String L2_FLH_LOW_BAND = "reflec_7";
    private static final String L2_FLH_HIGH_BAND = "reflec_9";
    private static final String L2_FLH_SIGNAL_BAND = "reflec_8";
    private static final String L2_FLH_LINEHEIGHT_BAND = "FLH";
    private static final String L2_FLH_SLOPE_BAND = "FLH_SLOPE";

    private static final String L1_MCI_LOW_BAND = "radiance_8";
    private static final String L1_MCI_HIGH_BAND = "radiance_10";
    private static final String L1_MCI_SIGNAL_BAND = "radiance_9";
    private static final String L1_MCI_LINEHEIGHT_BAND = "MCI";
    private static final String L1_MCI_SLOPE_BAND = "MCI_SLOPE";

    private static final String L2_MCI_LOW_BAND = "reflec_8";
    private static final String L2_MCI_HIGH_BAND = "reflec_10";
    private static final String L2_MCI_SIGNAL_BAND = "reflec_9";
    private static final String L2_MCI_LINEHEIGHT_BAND = "MCI";
    private static final String L2_MCI_SLOPE_BAND = "MCI_SLOPE";

    private static final String L1_BITMASK = "NOT l1_flags.LAND_OCEAN AND NOT l1_flags.BRIGHT AND NOT l1_flags.INVALID";
    private static final String L2_BITMASK = "l2_flags.WATER";

    /**
     * Singleton interface. Retrieves the one and only instance of this class. Creates the instance if none exists.
     */
    public static FlhMciPresetManager getInstance() {
        return Holder.instance;
    }

    /**
     * Retrieves a preset by the given name. Returns <code>null</code> when no appropriate preset is found
     */
    public FlhMciPreset getPresetByName(String presetName) {
        Guardian.assertNotNull("presetName", presetName);
        FlhMciPreset preset = null;
        FlhMciPreset presetReturn = null;

        preset = _presetMap.get(presetName);
        if (preset != null) {
            presetReturn = new FlhMciPreset(preset);
        }

        return presetReturn;
    }

    /**
     * Retrieves a name for the preset passed in.
     *
     * @param preset the <code>FlhMciPreset</code> to be checked
     */
    public String getPresetName(FlhMciPreset preset) {
        Guardian.assertNotNull("preset", preset);
        String name;

        if (preset.equals(_presetMap.get(FlhMciConstants.PRESET_PARAM_VALUE_SET[0]))) {
            name = FlhMciConstants.PRESET_PARAM_VALUE_SET[0];
        } else if (preset.equals(_presetMap.get(FlhMciConstants.PRESET_PARAM_VALUE_SET[1]))) {
            name = FlhMciConstants.PRESET_PARAM_VALUE_SET[1];
        } else if (preset.equals(_presetMap.get(FlhMciConstants.PRESET_PARAM_VALUE_SET[2]))) {
            name = FlhMciConstants.PRESET_PARAM_VALUE_SET[2];
        } else {
            name = FlhMciConstants.PRESET_PARAM_VALUE_SET[3];
        }
        return name;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Constructs the object, creates the preset map.
     */
    private FlhMciPresetManager() {
        _presetMap = new HashMap<String, FlhMciPreset>();

        fillPresetMap();
    }

    /**
     * Fills the preset map with all presets supported by the manager
     */
    private void fillPresetMap() {
        _presetMap.put(FlhMciConstants.PRESET_PARAM_VALUE_SET[0], getL2FlhPreset());
        _presetMap.put(FlhMciConstants.PRESET_PARAM_VALUE_SET[1], getL1MciPreset());
        _presetMap.put(FlhMciConstants.PRESET_PARAM_VALUE_SET[2], getL2MciPreset());
    }

    /**
     * Retrieves a preset with all values set to the default flh L2 processing
     */
    private static FlhMciPreset getL2FlhPreset() {
        FlhMciPreset preset = new FlhMciPreset();

        preset.setLowBandName(L2_FLH_LOW_BAND);
        preset.setHighBandName(L2_FLH_HIGH_BAND);
        preset.setSignalBandName(L2_FLH_SIGNAL_BAND);
        preset.setLineheightBandName(L2_FLH_LINEHEIGHT_BAND);
        preset.setSlopeBandName(L2_FLH_SLOPE_BAND);
        preset.setBitmaskExpression(L2_BITMASK);

        return preset;
    }

    /**
     * Retrieves a preset with all values set to the default mci L1 processing
     */
    private static FlhMciPreset getL1MciPreset() {
        FlhMciPreset preset = new FlhMciPreset();

        preset.setLowBandName(L1_MCI_LOW_BAND);
        preset.setHighBandName(L1_MCI_HIGH_BAND);
        preset.setSignalBandName(L1_MCI_SIGNAL_BAND);
        preset.setLineheightBandName(L1_MCI_LINEHEIGHT_BAND);
        preset.setSlopeBandName(L1_MCI_SLOPE_BAND);
        preset.setBitmaskExpression(L1_BITMASK);

        return preset;
    }

    /**
     * Retrieves a preset with all values set to the default mci L2 processing
     */
    private static FlhMciPreset getL2MciPreset() {
        FlhMciPreset preset = new FlhMciPreset();

        preset.setLowBandName(L2_MCI_LOW_BAND);
        preset.setHighBandName(L2_MCI_HIGH_BAND);
        preset.setSignalBandName(L2_MCI_SIGNAL_BAND);
        preset.setLineheightBandName(L2_MCI_LINEHEIGHT_BAND);
        preset.setSlopeBandName(L2_MCI_SLOPE_BAND);
        preset.setBitmaskExpression(L2_BITMASK);

        return preset;
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final FlhMciPresetManager instance = new FlhMciPresetManager();
    }
}
