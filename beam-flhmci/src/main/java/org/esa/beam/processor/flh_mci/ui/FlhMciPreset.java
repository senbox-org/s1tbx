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

import org.esa.beam.processor.flh_mci.FlhMciConstants;
import org.esa.beam.util.Guardian;

/**
 * This class contains all the parameter needed to hold a complete preset parameter set.
 */
@Deprecated
public final class FlhMciPreset {

    private String _lowBandName;
    private String _highBandName;
    private String _signalBandName;
    private String _lineheightBandName;
    private String _slopeBandName;
    private String _bitmaskExpression;

    /**
     * Constructs the object with default values.
     */
    public FlhMciPreset() {
        _lowBandName = new String(FlhMciConstants.DEFAULT_BAND_LOW);
        _highBandName = new String(FlhMciConstants.DEFAULT_BAND_HIGH);
        _signalBandName = new String(FlhMciConstants.DEFAULT_BAND_SIGNAL);
        _lineheightBandName = new String(FlhMciConstants.DEFAULT_LINE_HEIGHT_BAND_NAME);
        _slopeBandName = new String(FlhMciConstants.DEFAULT_SLOPE_BAND_NAME);
        _bitmaskExpression = new String(FlhMciConstants.DEFAULT_BITMASK);
    }

    /**
     * Constructs the object from the template object passed in
     */
    public FlhMciPreset(FlhMciPreset template) {
        Guardian.assertNotNull("template", template);

        _lowBandName = template.getLowBandName();
        _highBandName = template.getHighBandName();
        _signalBandName = template.getSignalBandName();
        _lineheightBandName = template.getLineheightBandName();
        _slopeBandName = template.getSlopeBandName();
        _bitmaskExpression = template.getBitmaskExpression();
    }

    /**
     * Retrieves the low baseline band name of the preset.
     */
    public final String getLowBandName() {
        return _lowBandName;
    }

    /**
     * Sets the low baseline band name of the preset
     *
     * @param lowBandName the name of the low baseline band
     */
    public final void setLowBandName(String lowBandName) {
        Guardian.assertNotNull("lowBandName", lowBandName);
        _lowBandName = lowBandName;
    }

    /**
     * Retrieves the high baseline band name of the preset.
     */
    public final String getHighBandName() {
        return _highBandName;
    }

    /**
     * Sets the high baseline band name of the preset
     *
     * @param highBandName the name of the high baseline band
     */
    public final void setHighBandName(String highBandName) {
        Guardian.assertNotNull("highBandName", highBandName);
        _highBandName = highBandName;
    }

    /**
     * Retrieves the signal band name of the preset.
     */
    public final String getSignalBandName() {
        return _signalBandName;
    }

    /**
     * Sets the signal band name of the preset
     *
     * @param signalBandName the name of the signal band
     */
    public final void setSignalBandName(String signalBandName) {
        Guardian.assertNotNull("signalBandName", signalBandName);
        _signalBandName = signalBandName;
    }

    /**
     * Retrieves the lineheight band name of the preset.
     */
    public final String getLineheightBandName() {
        return _lineheightBandName;
    }

    /**
     * Sets the lineheight band name of the preset
     *
     * @param lineheightBandName the name of the lineheight band
     */
    public final void setLineheightBandName(String lineheightBandName) {
        Guardian.assertNotNull("lineheightBandName", lineheightBandName);
        _lineheightBandName = lineheightBandName;
    }

    /**
     * Retrieves the slope band name of the preset.
     */
    public final String getSlopeBandName() {
        return _slopeBandName;
    }

    /**
     * Sets the slope band name of the preset
     *
     * @param slopeBandName the name of the slope band
     */
    public final void setSlopeBandName(String slopeBandName) {
        Guardian.assertNotNull("slopeBandName", slopeBandName);
        _slopeBandName = slopeBandName;
    }

    /**
     * Retrieves the bitmask expression of the preset.
     */
    public final String getBitmaskExpression() {
        return _bitmaskExpression;
    }

    /**
     * Sets the bitmask expression of the preset
     *
     * @param bitmaskExpr the bitmask expression of the preset
     */
    public final void setBitmaskExpression(String bitmaskExpr) {
        Guardian.assertNotNull("bitmaskExpression", bitmaskExpr);
        _bitmaskExpression = bitmaskExpr;
    }

    /**
     * Compares this Object with the one passed in
     */
    @Override
    public final boolean equals(Object other) {
        Guardian.assertNotNull("other", other);

        if (!(other instanceof FlhMciPreset)) {
            return false;
        }

        FlhMciPreset otherPreset = (FlhMciPreset) other;

        if (!otherPreset.getLowBandName().equals(_lowBandName)) {
            return false;
        }
        if (!otherPreset.getHighBandName().equals(_highBandName)) {
            return false;
        }
        if (!otherPreset.getSignalBandName().equals(_signalBandName)) {
            return false;
        }
        if (!otherPreset.getLineheightBandName().equals(_lineheightBandName)) {
            return false;
        }
        if (!otherPreset.getSlopeBandName().equals(_slopeBandName)) {
            return false;
        }
        if (!otherPreset.getBitmaskExpression().equals(_bitmaskExpression)) {
            return false;
        }
        return true;
    }
}
