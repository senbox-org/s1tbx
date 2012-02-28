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
package org.esa.beam.processor.flh_mci;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.esa.beam.framework.processor.ProcessorException;

public class BaselineAlgorithmTest extends TestCase {

    private BaselineAlgorithm _algo;

    public BaselineAlgorithmTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(BaselineAlgorithmTest.class);
    }

    @Override
    public void setUp() {
        _algo = new BaselineAlgorithm();
    }

    /**
     * Tests the functionality of setWavelengths()
     */
    public void testSetWavelengths() {
        float correctLow = 650.f;
        float correctSignal = 700.f;
        float correctHigh = 800.f;

        // when correct settings - nothing should happen
        try {
            _algo.setWavelengths(correctLow, correctHigh, correctSignal);
        } catch (ProcessorException e) {
            fail("no exception expected!");
        }

        // when setting low and signal the same - exception
        try {
            _algo.setWavelengths(correctLow, correctHigh, correctLow);
            fail("exception expected!");
        } catch (ProcessorException e) {
        }

        // when setting high and low the same - exception
        try {
            _algo.setWavelengths(correctLow, correctLow, correctSignal);
            fail("exception expected!");
        } catch (ProcessorException e) {
        }

        // when using negative wavelengths - exception
        try {
            _algo.setWavelengths(-correctLow, correctHigh, correctSignal);
            fail("exception expected!");
        } catch (ProcessorException e) {
        }
        try {
            _algo.setWavelengths(correctLow, -correctHigh, correctSignal);
            fail("exception expected!");
        } catch (ProcessorException e) {
        }
        try {
            _algo.setWavelengths(correctLow, correctHigh, -correctSignal);
            fail("exception expected!");
        } catch (ProcessorException e) {
        }
    }

    /**
     * Tests the process function for correct parameter behaviour
     */
    public void testProcessInterface() {
        float[] low = {1, 2, 3, 4};
        float[] mid = {1, 2, 3, 4};
        float[] high = {1, 2, 3, 4};
        float[] recycle = {0, 0, 0, 0};
        boolean[] process = {true, true, true, true};

        // low array must not be null
        try {
            _algo.process(null, mid, high, process, recycle);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // mid array must not be null
        try {
            _algo.process(low, null, high, process, recycle);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // high array must not be null
        try {
            _algo.process(low, mid, null, process, recycle);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // process array must not be null
        try {
            _algo.process(low, mid, high, null, recycle);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // recycle array can be null
        try {
            _algo.process(low, mid, high, process, null);
        } catch (IllegalArgumentException e) {
            fail("no exception expected");
        }
    }

    /**
     * tests the correct use of the recycle array in process() calls
     */
    public void testProcessRecyleArrayUse() {
        float[] low = {1, 2, 3, 4};
        float[] mid = {1, 2, 3, 4};
        float[] high = {1, 2, 3, 4};
        float[] recycle = {0, 0, 0, 0};
        boolean[] process = {true, true, true, true};
        float[] fRet;

        // when not recycle array is set - the process function shall return data anyway
        fRet = _algo.process(low, mid, high, process, null);
        assertNotNull(fRet);
        // and returned array must have the same size as one of the input arrays
        assertEquals(mid.length, fRet.length);

        // now set the recycle, the returned array must have the same reference
        fRet = _algo.process(low, mid, high, process, recycle);
        assertEquals(recycle, fRet);
        // and the same size
        assertEquals(recycle.length, fRet.length);
    }

    /**
     * Checks whether the returnvalues of process() are correctly calculated when the signal is inbetween low and high
     * baseline band
     */
    public void testProcessCorrectCalculationInbetween() {
        // signal inbetween both low and high - centered
        float[] low = {6.f, 6.f, 6.f};
        float[] high = {4.f, 4.f, 4.f};
        float[] signal = {7.f, 5.f, 3.f};
        boolean[] process = {true, true, true};
        float[] fRet;

        float lowWave = 3.f;
        float hiWave = 11.f;
        float signalWave = 7.f;

        float[] expected = {2.f, 0.f, -2.f};
        float[] expected_corr = {1.975f, -0.025f, -2.025f};

        // without cloud correction
        try {
            _algo.setWavelengths(lowWave, hiWave, signalWave);
            _algo.setCloudCorrectionFactor(1.0f);
        } catch (ProcessorException e) {
            fail("no exception expected here");
        }
        fRet = _algo.process(low, high, signal, process, null);
        for (int n = 0; n < fRet.length; n++) {
            assertEquals(expected[n], fRet[n], 1e-6);
        }

        // with cloud correction
        try {
            _algo.setWavelengths(lowWave, hiWave, signalWave);
            _algo.setCloudCorrectionFactor(1.005f);
        } catch (ProcessorException e) {
            fail("no exception expected here");
        }
        fRet = _algo.process(low, high, signal, process, null);
        for (int n = 0; n < fRet.length; n++) {
            assertEquals(expected_corr[n], fRet[n], 1e-6);
        }

        // signal inbetween both low and high - more left
        float[] signal_2 = {7.f, 5.5f, 3.f};
        signalWave = 5.f;

        float[] expected_2 = {1.5f, 0.f, -2.5f};
        float[] expected_corr_2 = {1.4725f, -0.0275f, -2.5275f};

        // without cloud correction
        try {
            _algo.setWavelengths(lowWave, hiWave, signalWave);
            _algo.setCloudCorrectionFactor(1.f);
        } catch (ProcessorException e) {
            fail("no exception expected here");
        }
        fRet = _algo.process(low, high, signal_2, process, null);
        for (int n = 0; n < fRet.length; n++) {
            assertEquals(expected_2[n], fRet[n], 1e-6);
        }

        // with cloud correction
        try {
            _algo.setWavelengths(lowWave, hiWave, signalWave);
            _algo.setCloudCorrectionFactor(1.005f);
        } catch (ProcessorException e) {
            fail("no exception expected here");
        }
        fRet = _algo.process(low, high, signal_2, process, null);
        for (int n = 0; n < fRet.length; n++) {
            assertEquals(expected_corr_2[n], fRet[n], 1e-6);
        }
    }

    /**
     * Tests the correct calculation for the baseline height when the signal channel is left (lower) of the baseline
     * channels
     */
    public void testProcessCorrectCalculationLeft() {
        float[] low = {5.f, 5.f, 5.f};
        float[] high = {6.f, 6.f, 6.f};
        float[] signal = {5.f, 3.f, 1.f};
        boolean[] process = {true, true, true};
        float[] fRet;

        float lowWave = 8.f;
        float hiWave = 10.f;
        float signalWave = 4.f;

        float[] expected = {2.f, 0.f, -2.f};
        float[] expected_corr = {1.985f, -0.015f, -2.015f};

        // without cloud correction
        try {
            _algo.setWavelengths(lowWave, hiWave, signalWave);
            _algo.setCloudCorrectionFactor(1.0f);
        } catch (ProcessorException e) {
            fail("no exception expected here");
        }
        fRet = _algo.process(low, high, signal, process, null);
        for (int n = 0; n < fRet.length; n++) {
            assertEquals(expected[n], fRet[n], 1e-6);
        }

        // with cloud correction
        try {
            _algo.setWavelengths(lowWave, hiWave, signalWave);
            _algo.setCloudCorrectionFactor(1.005f);
        } catch (ProcessorException e) {
            fail("no exception expected here");
        }
        fRet = _algo.process(low, high, signal, process, null);
        for (int n = 0; n < fRet.length; n++) {
            assertEquals(expected_corr[n], fRet[n], 1e-6);
        }
    }

    /**
     * Tests the correct calculation for the baseline height when the signal channel is right (higher) of the baseline
     * channels
     */
    public void testCorrectCalculationRight() {
        float[] low = {5.f, 5.f, 5.f};
        float[] high = {4.f, 4.f, 4.f};
        float[] signal = {6.f, 3.f, 0.f};
        boolean[] process = {true, true, true};
        float[] fRet;

        float lowWave = 2.f;
        float hiWave = 7.f;
        float signalWave = 12.f;

        float[] expected = {3.f, 0.f, -3.f};
        float[] expected_corr = {2.985f, -0.015f, -3.015f};

        // without cloud correction
        try {
            _algo.setWavelengths(lowWave, hiWave, signalWave);
            _algo.setCloudCorrectionFactor(1.f);
        } catch (ProcessorException e) {
            fail("no exception expected here");
        }
        fRet = _algo.process(low, high, signal, process, null);
        for (int n = 0; n < fRet.length; n++) {
            assertEquals(expected[n], fRet[n], 1e-6);
        }

        // with cloud correction
        try {
            _algo.setWavelengths(lowWave, hiWave, signalWave);
            _algo.setCloudCorrectionFactor(1.005f);
        } catch (ProcessorException e) {
            fail("no exception expected here");
        }
        fRet = _algo.process(low, high, signal, process, null);
        for (int n = 0; n < fRet.length; n++) {
            assertEquals(expected_corr[n], fRet[n], 1e-6);
        }
    }

    /**
     * Tests whether the process array is used correctly to mask out pixels in calls to process()
     */
    public void testUseOfInvalidPixel() {
        float invalid = 0.1f;
        float[] low = {1, 2, 3, 4};
        float[] high = {7, 6, 5, 4};
        float[] signal = {5, 6, 7, 8};
        float lowWave = 1;
        float hiWave = 3;
        float signalWave = 2;
        float[] expected = {1.f, 2.f, invalid, 4.f};
        boolean[] process = {true, true, false, true};

        float[] fRet;
        _algo.setInvalidValue(invalid);
        try {
            _algo.setWavelengths(lowWave, hiWave, signalWave);
            _algo.setCloudCorrectionFactor(1.0f);
        } catch (ProcessorException e) {
            fail("no exception expected here");
        }

        fRet = _algo.process(low, high, signal, process, null);
        for (int n = 0; n < fRet.length; n++) {
            assertEquals(expected[n], fRet[n], 1e-6);
        }
    }

    /**
     * Tests the processSlope function for correct parameter behaviour
     */
    public void testProcessSlopeInterface() {
        float[] low = {1.f, 2.f, 3.f, 4.f};
        float[] high = {5.f, 6.f, 7.f, 8.f};
        float[] recycle = {0, 0, 0, 0};
        boolean[] process = {true, true, true, true};

        // null for low radiances is not allowed
        try {
            _algo.processSlope(null, high, process, recycle);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // null for high radiances is not allowed
        try {
            _algo.processSlope(low, null, process, recycle);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // null for boolean radiances is not allowed
        try {
            _algo.processSlope(low, high, null, recycle);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // null for recycle radiances is allowed
        try {
            _algo.processSlope(low, high, process, null);
        } catch (IllegalArgumentException e) {
            fail("no exception expected");
        }
    }

    /**
     * Tests the correct use of the recycle array in processSlope()
     */
    public void testProcessSlopeRecycleArray() {
        float[] low = {1.f, 2.f, 3.f, 4.f};
        float[] high = {5.f, 6.f, 7.f, 8.f};
        float[] recycle = {0, 0, 0, 0};
        boolean[] process = {true, true, true, true};

        float[] fRet;

        // when not recycle array is set - the process function shall return data anyway
        fRet = _algo.processSlope(low, high, process, null);
        assertNotNull(fRet);
        // and returned array must have the same size as one of the input arrays
        assertEquals(high.length, fRet.length);

        // now set the recycle, the returned array must have the same reference
        fRet = _algo.processSlope(low, high, process, recycle);
        assertNotNull(fRet);
        assertEquals(recycle, fRet);
        // and the same size
        assertEquals(recycle.length, fRet.length);
    }

    /**
     * Checks that processSlope returns the correct values
     */
    public void testProcessSlopeCorrectCalculation() {
        float[] low = {1.f, 2.f, 4.f, -2.f};
        float[] high = {3.f, 4.f, 2.f, -4.f};
        float[] recycle = {0, 0, 0, 0};
        boolean[] process = {true, true, true, true};
        float[] expected = {1.f, 1.f, -1.f, -1.f};

        float[] fRet = null;

        float lowWave = 2.f;
        float hiWave = 4.f;
        float signalWave = 3.f;  // not needed for the slope but checked

        try {
            _algo.setWavelengths(lowWave, hiWave, signalWave);
            fRet = _algo.processSlope(low, high, process, recycle);
        } catch (ProcessorException e) {
            fail("no exception expected here");
        }

        for (int n = 0; n < fRet.length; n++) {
            assertEquals(expected[n], fRet[n], 1e-6);
        }

        // now some more advanced values
        float[] low_2 = {39, 50, 41, 84, 53};
        float[] high_2 = {26, 38, 31, 70, 40};
        boolean[] process_2 = {true, true, true, true, true};

        lowWave = 664;
        hiWave = 708;
        signalWave = 680;

        float[] expected_2 = {-0.29545454f, -0.27272727f, -0.2272727f, -0.31818181f, -0.29545454f};

        try {
            _algo.setWavelengths(lowWave, hiWave, signalWave);
            fRet = _algo.processSlope(low_2, high_2, process_2, null);
        } catch (ProcessorException e) {
            fail("no exception expected here");
        }

        for (int n = 0; n < fRet.length; n++) {
            assertEquals(expected_2[n], fRet[n], 1e-6);
        }
    }

    /**
     * Tests if the value for invalid pixels in used correctly
     */
    public void testProcessSlopeUseOfInvalidPixel() {
        float invalid = 0.56f;
        float[] low = {1.f, 2.f, 4.f, -2.f};
        float[] high = {3.f, 4.f, 2.f, -4.f};
        float[] recycle = {0, 0, 0, 0};
        boolean[] process = {true, false, false, true};
        float[] expected = {1.f, invalid, invalid, -1.f};

        float[] fRet = null;

        float lowWave = 2.f;
        float hiWave = 4.f;
        float signalWave = 3.f;  // not needed for the slope but checked

        _algo.setInvalidValue(invalid);
        try {
            _algo.setWavelengths(lowWave, hiWave, signalWave);
            fRet = _algo.processSlope(low, high, process, recycle);
        } catch (ProcessorException e) {
            fail("no exception expected here");
        }

        for (int n = 0; n < fRet.length; n++) {
            assertEquals(expected[n], fRet[n], 1e-6);
        }
    }

    /**
     * Tests the cloud correction factor default value constant for correct value
     */
    public void testDefaultCloudCorrectionFactor() {
        assertEquals(1.005f, BaselineAlgorithm.DEFAULT_CLOUD_CORRECT, 1e-6);
    }

}

