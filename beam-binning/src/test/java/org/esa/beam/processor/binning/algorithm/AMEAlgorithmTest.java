/*
 * $Id: AMEAlgorithmTest.java,v 1.1 2006/09/11 10:47:33 norman Exp $
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

import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.processor.binning.database.Bin;
import org.esa.beam.processor.binning.database.FloatArrayBin;

public class AMEAlgorithmTest extends TestCase {

    private Algorithm _algo;

    public AMEAlgorithmTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(AMEAlgorithmTest.class);
    }

    @Override
    protected void setUp() {
        try {
            AlgorithmFactory factory = new AlgorithmFactory();
            assertNotNull(factory);
            _algo = factory.getAlgorithm("Arithmetic Mean");
            assertNotNull(_algo);
            _algo.init("0.5");
        } catch (ProcessorException e) {
            fail("NO ProcessorException expected");
        }
    }

    /**
     * Tests the correct functionality if the init method with a request
     */
    public void testAccumulateSpatial() {
        float testVal = 1.f;
        Bin bin = createTestBin(0, 0.f, 0.f, 0.f);

        // test with count zero (initial)
        _algo.accumulateSpatial(testVal, bin);
        assertEquals(testVal, bin.read(0), 1e-6);               // sum
        assertEquals(testVal * testVal, bin.read(1), 1e-6);     // sum square
        assertEquals(1.f, bin.read(2), 1e-6);                   // count
        assertEquals(0.f, bin.read(3), 1e-6);                   // weight

        testVal = 3.5f;
        bin = createTestBin(0, 0.f, 0.f, 0.f);
        _algo.accumulateSpatial(testVal, bin);
        assertEquals(testVal, bin.read(0), 1e-6);               // sum
        assertEquals(testVal * testVal, bin.read(1), 1e-6);     // sum square
        assertEquals(1.f, bin.read(2), 1e-6);                   // count
        assertEquals(0.f, bin.read(3), 1e-6);                   // weight

        testVal = 1.f;
        bin = createTestBin(3, 4.f, 14.f, 5.f);
        _algo.accumulateSpatial(testVal, bin);
        assertEquals(5.f, bin.read(0), 1e-6);               // sum
        assertEquals(15.f, bin.read(1), 1e-6);              // sum square
        assertEquals(4.f, bin.read(2), 1e-6);               // count
        assertEquals(5.f, bin.read(3), 1e-6);               // weight

        testVal = 1.8f;
        bin = createTestBin(5, 6.f, 17.f, 8.f);
        _algo.accumulateSpatial(testVal, bin);
        assertEquals(7.8f, bin.read(0), 1e-6);      // sum
        assertEquals(20.24f, bin.read(1), 1e-6);    // sum square
        assertEquals(6.f, bin.read(2), 1e-6);       // count
        assertEquals(8.f, bin.read(3), 1e-6);       // weight
    }

    /**
     * Tests the correct return value of needsFinishSpatial
     */
    public void testNeedsFinishSpatial() {
        assertEquals(true, _algo.needsFinishSpatial());
    }

    /**
     * Tests the correct functionality of spatial finishing. Must calculate the weight coefficient and not touch the
     * other values
     */
    public void testFinishSpatial() {

        try {
            Bin bin = createTestBin(0, 0.f, 0.f, 0.f);

            // test with count zero (initial)
            _algo.finishSpatial(bin);
            assertEquals(0.f, bin.read(0), 1e-6);   // sum
            assertEquals(0.f, bin.read(1), 1e-6);   // sum square
            assertEquals(0.f, bin.read(2), 1e-6);   // count
            assertEquals(0.f, bin.read(3), 1e-6);   // weight

            bin = createTestBin(4, 2.f, 3.f, 4.f);
            _algo.finishSpatial(bin);
            assertEquals(1.f, bin.read(0), 1e-6);   // sum
            assertEquals(1.5f, bin.read(1), 1e-6);   // sum square
            assertEquals(4.f, bin.read(2), 1e-6);   // count
            assertEquals(2.f, bin.read(3), 1e-6);   // weight

            bin = createTestBin(9, 10.f, 11.f, 6.f);
            _algo.finishSpatial(bin);
            assertEquals(3.3333334922790527f, bin.read(0), 1e-6);  // sum
            assertEquals(3.6666667461395264f, bin.read(1), 1e-6);  // sum square
            assertEquals(9.f, bin.read(2), 1e-6);   // count
            assertEquals(3.f, bin.read(3), 1e-6);   // weight

            // now change weight coefficient and test again
            _algo.init("1.0");

            bin = createTestBin(5, 6.f, 8.f, 1.f);
            _algo.finishSpatial(bin);
            assertEquals(6.f, bin.read(0), 1e-6);   // sum
            assertEquals(8.f, bin.read(1), 1e-6);   // sum square
            assertEquals(5.f, bin.read(2), 1e-6);   // count
            assertEquals(5.f, bin.read(3), 1e-6);   // weight

            bin = createTestBin(9, 10.f, 11.f, 6.f);
            _algo.finishSpatial(bin);
            assertEquals(10.f, bin.read(0), 1e-6);  // sum
            assertEquals(11.f, bin.read(1), 1e-6);  // sum square
            assertEquals(9.f, bin.read(2), 1e-6);   // count
            assertEquals(9.f, bin.read(3), 1e-6);   // weight

            // now change weight coefficient and test again
            _algo.init("0.68");

            bin = createTestBin(5, 6.f, 8.f, 1.f);
            _algo.finishSpatial(bin);
            assertEquals(3.5849313735961914f, bin.read(0), 1e-6);   // sum
            assertEquals(4.779908657073975f, bin.read(1), 1e-6);   // sum square
            assertEquals(5.f, bin.read(2), 1e-6);   // count
            assertEquals(2.9874428f, bin.read(3), 1e-6);// weight

            bin = createTestBin(9, 10.f, 11.f, 6.f);
            _algo.finishSpatial(bin);
            assertEquals(4.9504241943359375f, bin.read(0), 1e-6);  // sum
            assertEquals(5.4454665184021f, bin.read(1), 1e-6);  // sum square
            assertEquals(9.f, bin.read(2), 1e-6);   // count
            assertEquals(4.45538159f, bin.read(3), 1e-6);// weight
        } catch (ProcessorException e) {
            fail("No exception expected");
        }
    }

    public void testAccumulateTemporal() {
        Bin binIn = createTestBin(0, 0.f, 0.f, 0.f);
        Bin binOut = createTestBin(0, 0.f, 0.f, 0.f);

        _algo.accumulateTemporal(binIn, binOut);
        assertEquals(0.f, binOut.read(0), 1e-6);    // sum
        assertEquals(0.f, binOut.read(1), 1e-6);    // sum square
        assertEquals(0.f, binOut.read(2), 1e-6);    // count
        assertEquals(0.f, binOut.read(3), 1e-6);    // weight

        // when input has no counts nothing shall happen
        binIn = createTestBin(0, 1.f, 2.f, 3.f);
        binOut = createTestBin(4, 5.f, 6.f, 6.f);
        _algo.accumulateTemporal(binIn, binOut);
        assertEquals(5.f, binOut.read(0), 1e-6);    // sum
        assertEquals(6.f, binOut.read(1), 1e-6);    // sum square
        assertEquals(4.f, binOut.read(2), 1e-6);    // count
        assertEquals(6.f, binOut.read(3), 1e-6);    // weight

        // when input has counts, things shall add up
        binIn = createTestBin(2, 1.f, 2.f, 3.f);
        binOut = createTestBin(4, 5.f, 6.f, 6.f);
        _algo.accumulateTemporal(binIn, binOut);
        assertEquals(6.f, binOut.read(0), 1e-6);    // sum
        assertEquals(8.f, binOut.read(1), 1e-6);    // sum square
        assertEquals(6.f, binOut.read(2), 1e-6);    // count
        assertEquals(9.f, binOut.read(3), 1e-6);    // weight

        binIn = createTestBin(5, 4.f, 3.f, 2.f);
        binOut = createTestBin(1, 9.f, 8.f, 7.f);
        _algo.accumulateTemporal(binIn, binOut);
        assertEquals(13.f, binOut.read(0), 1e-6);    // sum
        assertEquals(11.f, binOut.read(1), 1e-6);    // sum square
        assertEquals(6.f, binOut.read(2), 1e-6);    // count
        assertEquals(9.f, binOut.read(3), 1e-6);    // weight
    }

    /**
     * Tests the correct return value of needsInterprete
     */
    public void testNeedsInterpretation() {
        assertEquals(true, _algo.needsInterpretation());
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private Bin createTestBin(int count, float sum, float sum_2, float weight) {
        Bin binRet = new FloatArrayBin(new int[]{4});

        binRet.write(0, sum);
        binRet.write(1, sum_2);
        binRet.write(2, count);
        binRet.write(3, weight);

        return binRet;
    }
}
