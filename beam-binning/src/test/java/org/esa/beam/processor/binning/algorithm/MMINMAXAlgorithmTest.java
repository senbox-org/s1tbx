/*
 * $Id: MMINMAXAlgorithmTest.java,v 1.1 2006/09/11 10:47:33 norman Exp $
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
package org.esa.beam.processor.binning.algorithm;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.esa.beam.processor.binning.database.Bin;
import org.esa.beam.processor.binning.database.FloatArrayBin;

public class MMINMAXAlgorithmTest extends TestCase {

    private Algorithm _algo;

    public MMINMAXAlgorithmTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(MMINMAXAlgorithmTest.class);
    }

    protected void setUp() {
        AlgorithmFactory factory = new AlgorithmFactory();
        assertNotNull(factory);
        _algo = factory.getAlgorithm("Minimum/Maximum");
        assertNotNull(_algo);
    }

    /**
     * Tests the correct functionality if the setRequest method with a request
     */
    public void testAccumulateSpatial() {
        float testVal = 1.f;
        Bin bin = createTestBin(0, 0.f, 0.f);

        // test with count zero (initial)
        _algo.accumulateSpatial(testVal, bin);
        assertEquals(testVal, bin.read(0), 1e-6);   // min
        assertEquals(testVal, bin.read(1), 1e-6);   // max
        assertEquals(1.f, bin.read(2), 1e-6);       // count

        testVal = 3.f;
        bin = createTestBin(0, 0.f, 0.f);
        _algo.accumulateSpatial(testVal, bin);
        assertEquals(testVal, bin.read(0), 1e-6);   // min
        assertEquals(testVal, bin.read(1), 1e-6);   // max
        assertEquals(1.f, bin.read(2), 1e-6);       // count

        // test with given count
        bin = createTestBin(6, 0.f, 0.f);
        _algo.accumulateSpatial(testVal, bin);
        assertEquals(0.f, bin.read(0), 1e-6);       // min
        assertEquals(testVal, bin.read(1), 1e-6);   // max
        assertEquals(7.f, bin.read(2), 1e-6);       // count

        bin = createTestBin(6, 8.f, 9.f);
        _algo.accumulateSpatial(testVal, bin);
        assertEquals(testVal, bin.read(0), 1e-6);   // min
        assertEquals(9.f, bin.read(1), 1e-6);       // max
        assertEquals(7.f, bin.read(2), 1e-6);       // count
    }

    /**
     * Tests the correct return value of needsFinishSpatial
     */
    public void testFinishSpatial() {
        assertEquals(false, _algo.needsFinishSpatial());
    }

    /**
     * Tests the correct functionality if the init method with a request
     */
    public void testAccumulateTemporal() {
        Bin bin = createTestBin(4, 1.f, 3.f);
        Bin finalBin = createTestBin(0, 0.f, 0.f);

        // test with count zero (initial)
        _algo.accumulateTemporal(bin, finalBin);
        assertEquals(1.f, finalBin.read(0), 1e-6);   // min
        assertEquals(3.f, finalBin.read(1), 1e-6);   // max
        assertEquals(4.f, finalBin.read(2), 1e-6);   // count

        bin = createTestBin(45, 5.f, 9.f);
        finalBin = createTestBin(0, 0.f, 0.f);
        _algo.accumulateTemporal(bin, finalBin);
        assertEquals(5.f, finalBin.read(0), 1e-6);   // min
        assertEquals(9.f, finalBin.read(1), 1e-6);   // max
        assertEquals(45.f, finalBin.read(2), 1e-6);  // count

        // now some accumulating bins
        // nothing shall happen
        bin = createTestBin(6, 5.f, 9.f);
        finalBin = createTestBin(5, 4.f, 11.f);
        _algo.accumulateTemporal(bin, finalBin);
        assertEquals(4.f, finalBin.read(0), 1e-6);   // min
        assertEquals(11.f, finalBin.read(1), 1e-6);   // max
        assertEquals(11.f, finalBin.read(2), 1e-6);  // count

        // new minimum value
        bin = createTestBin(5, -8.f, 9.f);
        finalBin = createTestBin(8, 4.f, 11.f);
        _algo.accumulateTemporal(bin, finalBin);
        assertEquals(-8.f, finalBin.read(0), 1e-6);   // min
        assertEquals(11.f, finalBin.read(1), 1e-6);   // max
        assertEquals(13.f, finalBin.read(2), 1e-6);  // count

        // new maximum value
        bin = createTestBin(56, 6.f, 44.f);
        finalBin = createTestBin(31, 4.f, 11.f);
        _algo.accumulateTemporal(bin, finalBin);
        assertEquals(4.f, finalBin.read(0), 1e-6);   // min
        assertEquals(44.f, finalBin.read(1), 1e-6);   // max
        assertEquals(87.f, finalBin.read(2), 1e-6);  // count

        // both values
        bin = createTestBin(5, -123.f, -6.f);
        finalBin = createTestBin(38, -34.f, -32.f);
        _algo.accumulateTemporal(bin, finalBin);
        assertEquals(-123.f, finalBin.read(0), 1e-6);   // min
        assertEquals(-6.f, finalBin.read(1), 1e-6);   // max
        assertEquals(43.f, finalBin.read(2), 1e-6);  // count
    }

    /**
     * Tests the correct return value of needsInterpretation
     */
    public void testNeedsInterprete() {
        assertEquals(false, _algo.needsInterpretation());
    }

    /**
     * Tests the correct behaviour of the accumulated variables interface
     */
    public void testAccumulatedVariables() {
        int expectedVars = 3;

        // check the number and variable names
        assertEquals(expectedVars, _algo.getNumberOfAccumulatedVariables());
        assertEquals("min", _algo.getAccumulatedVariableNameAt(0));
        assertEquals("max", _algo.getAccumulatedVariableNameAt(1));
        assertEquals("count", _algo.getAccumulatedVariableNameAt(2));

        // out of range names must be null
        assertEquals(null, _algo.getAccumulatedVariableNameAt(-5));
        assertEquals(null, _algo.getAccumulatedVariableNameAt(19));
    }

    /**
     * Tests the correct behaviour of the interpreted variables interface
     */
    public void testInterpretedVariables() {
        int expectedVars = 3;

        // check the number and variable names
        assertEquals(expectedVars, _algo.getNumberOfInterpretedVariables());
        assertEquals("min", _algo.getInterpretedVariableNameAt(0));
        assertEquals("max", _algo.getInterpretedVariableNameAt(1));
        assertEquals("count", _algo.getInterpretedVariableNameAt(2));

        // out of range names must be null
        assertEquals(null, _algo.getInterpretedVariableNameAt(-34));
        assertEquals(null, _algo.getInterpretedVariableNameAt(7));
    }

    /**
     * Tests for correct type string
     */
    public void testTypeString() {
        assertEquals("Minimum/Maximum", _algo.getTypeString());
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private Bin createTestBin(int count, float minVal, float maxVal) {
        Bin binRet = new FloatArrayBin(new int[]{3});

        binRet.write(0, minVal);
        binRet.write(1, maxVal);
        binRet.write(2, count);
        return binRet;
    }
}
