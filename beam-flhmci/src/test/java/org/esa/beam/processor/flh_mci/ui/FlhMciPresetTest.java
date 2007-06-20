/*
 * $Id: FlhMciPresetTest.java,v 1.1.1.1 2006/09/11 08:16:52 norman Exp $
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

public class FlhMciPresetTest extends TestCase {

    private FlhMciPreset _preset;

    public FlhMciPresetTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(FlhMciPresetTest.class);
    }

    protected void setUp() {
        _preset = new FlhMciPreset();
    }

    /**
     * Tests the functionality of the constructor
     */
    public void testConstructor() {
        // is already constructed in setUp

        // default constructor must provide the default values for all fields
        assertEquals(FlhMciConstants.DEFAULT_BAND_LOW, _preset.getLowBandName());
        assertEquals(FlhMciConstants.DEFAULT_BAND_HIGH, _preset.getHighBandName());
        assertEquals(FlhMciConstants.DEFAULT_BAND_SIGNAL, _preset.getSignalBandName());
        assertEquals(FlhMciConstants.DEFAULT_LINE_HEIGHT_BAND_NAME, _preset.getLineheightBandName());
        assertEquals(FlhMciConstants.DEFAULT_SLOPE_BAND_NAME, _preset.getSlopeBandName());
        assertEquals(FlhMciConstants.DEFAULT_BITMASK, _preset.getBitmaskExpression());
    }

    /**
     * Tests the copy constructor
     */
    public void testCopyConstruction() {
        String expLowBand = "low band";
        String expHighBand = "high band";
        String expSignalBand = "signal band";
        String expLineBand = "lineheight band";
        String expSlopeBand = "slope band";
        String expBitmask = "bit.mask";

        FlhMciPreset copied = null;

        // modify the set up preset so that it does not contain the default values
        _preset.setLowBandName(expLowBand);
        _preset.setHighBandName(expHighBand);
        _preset.setSignalBandName(expSignalBand);
        _preset.setLineheightBandName(expLineBand);
        _preset.setSlopeBandName(expSlopeBand);
        _preset.setBitmaskExpression(expBitmask);

        // copy constructor shall not accept null as argument
        try {
            copied = new FlhMciPreset(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // but shall work when fed with other object
        try {
            copied = new FlhMciPreset(_preset);
            // and now the values must fit
            assertEquals(expLowBand, copied.getLowBandName());
            assertEquals(expHighBand, copied.getHighBandName());
            assertEquals(expSignalBand, copied.getSignalBandName());
            assertEquals(expLineBand, copied.getLineheightBandName());
            assertEquals(expSlopeBand, copied.getSlopeBandName());
            assertEquals(expBitmask, copied.getBitmaskExpression());
        } catch (IllegalArgumentException e) {
            fail("NO exception expected");
        }
    }

    /**
     * Tests the functionality of the low band setter and getter
     */
    public void testsSetGetLowBand() {
        String testName1 = "very low";
        String testName2 = "low band test";

        // must be default name now
        assertEquals(FlhMciConstants.DEFAULT_BAND_LOW, _preset.getLowBandName());

        // null shall not be accepted
        try {
            _preset.setLowBandName(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // what is set shall be returned
        _preset.setLowBandName(testName1);
        assertEquals(testName1, _preset.getLowBandName());
        _preset.setLowBandName(testName2);
        assertEquals(testName2, _preset.getLowBandName());
    }

    /**
     * Tests the functionality of the high band setter and getter
     */
    public void testsSetGetHighBand() {
        String testName1 = "very high";
        String testName2 = "high band test";

        // must be default name now
        assertEquals(FlhMciConstants.DEFAULT_BAND_HIGH, _preset.getHighBandName());

        // null shall not be accepted
        try {
            _preset.setHighBandName(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // what is set shall be returned
        _preset.setHighBandName(testName1);
        assertEquals(testName1, _preset.getHighBandName());
        _preset.setHighBandName(testName2);
        assertEquals(testName2, _preset.getHighBandName());
    }

    /**
     * Tests the functionality of the signal band setter and getter
     */
    public void testsSetGetSignalBand() {
        String testName1 = "signalTest";
        String testName2 = "test Signal test";

        // must be default name now
        assertEquals(FlhMciConstants.DEFAULT_BAND_SIGNAL, _preset.getSignalBandName());

        // null shall not be accepted
        try {
            _preset.setSignalBandName(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // what is set shall be returned
        _preset.setSignalBandName(testName1);
        assertEquals(testName1, _preset.getSignalBandName());
        _preset.setSignalBandName(testName2);
        assertEquals(testName2, _preset.getSignalBandName());
    }

    /**
     * Tests the functionality of the lineheight band setter and getter
     */
    public void testsSetGetLineheightBand() {
        String testName1 = "lineheight band";
        String testName2 = "another lineheight band";

        // must be default name now
        assertEquals(FlhMciConstants.DEFAULT_LINE_HEIGHT_BAND_NAME, _preset.getLineheightBandName());

        // null shall not be accepted
        try {
            _preset.setLineheightBandName(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // what is set shall be returned
        _preset.setLineheightBandName(testName1);
        assertEquals(testName1, _preset.getLineheightBandName());
        _preset.setLineheightBandName(testName2);
        assertEquals(testName2, _preset.getLineheightBandName());
    }

    /**
     * Tests the functionality of the slope band setter and getter
     */
    public void testsSetGetSlopeBand() {
        String testName1 = "slope band";
        String testName2 = "another slope band";

        // must be default name now
        assertEquals(FlhMciConstants.DEFAULT_SLOPE_BAND_NAME, _preset.getSlopeBandName());

        // null shall not be accepted
        try {
            _preset.setSlopeBandName(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // what is set shall be returned
        _preset.setSlopeBandName(testName1);
        assertEquals(testName1, _preset.getSlopeBandName());
        _preset.setSlopeBandName(testName2);
        assertEquals(testName2, _preset.getSlopeBandName());
    }

    /**
     * Tests the functionality of the bitmask setter and getter
     */
    public void testsSetGetBitmask() {
        String testName1 = "bitmask.mask";
        String testName2 = "Test AND NOT Test";

        // must be default name now
        assertEquals(FlhMciConstants.DEFAULT_BITMASK, _preset.getBitmaskExpression());

        // null shall not be accepted
        try {
            _preset.setBitmaskExpression(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // what is set shall be returned
        _preset.setBitmaskExpression(testName1);
        assertEquals(testName1, _preset.getBitmaskExpression());
        _preset.setBitmaskExpression(testName2);
        assertEquals(testName2, _preset.getBitmaskExpression());
    }

    /**
     * Tests the equals operation functionality
     */
    public void testEqualOperation() {
        FlhMciPreset theOriginal = new FlhMciPreset();
        FlhMciPreset theOther = new FlhMciPreset();
        FlhMciPreset theOtherLow = new FlhMciPreset();
        FlhMciPreset theOtherHigh = new FlhMciPreset();
        FlhMciPreset theOtherSignal = new FlhMciPreset();
        FlhMciPreset theOtherLine = new FlhMciPreset();
        FlhMciPreset theOtherSlope = new FlhMciPreset();
        FlhMciPreset theOtherBitmask = new FlhMciPreset();

        // must throw exception when feed with null
        try {
            theOriginal.equals(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // must return false when fed with object of other class
        assertEquals(false, theOriginal.equals(new String("blabla")));
        assertEquals(false, theOriginal.equals(new Float(42.f)));

        // must return true when fed with an equal object
        assertEquals(true, theOriginal.equals(theOther));

        // must return false when we start to modify the other one
        theOtherLow.setLowBandName("invalid");
        assertEquals(false, theOriginal.equals(theOtherLow));
        theOtherHigh.setHighBandName("invalid");
        assertEquals(false, theOriginal.equals(theOtherHigh));
        theOtherSignal.setSignalBandName("invalid");
        assertEquals(false, theOriginal.equals(theOtherSignal));
        theOtherLine.setLineheightBandName("invalid");
        assertEquals(false, theOriginal.equals(theOtherLine));
        theOtherSlope.setSlopeBandName("invalid");
        assertEquals(false, theOriginal.equals(theOtherSlope));
        theOtherBitmask.setBitmaskExpression("invalid");
        assertEquals(false, theOriginal.equals(theOtherBitmask));
    }

}
