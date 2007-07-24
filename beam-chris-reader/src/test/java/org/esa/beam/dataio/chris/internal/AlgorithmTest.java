package org.esa.beam.dataio.chris.internal;

import junit.framework.TestCase;

/**
 * Test methods for class {@link Algorithm}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class AlgorithmTest extends TestCase {

    public void testNthElement() {

        try {
            Algorithm.nthElement(null, 0);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            Algorithm.nthElement(new double[0], 0);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            Algorithm.nthElement(new double[]{1.0, 2.0}, -1);
            fail();
        } catch (Exception expected) {
        }

        try {
            Algorithm.nthElement(new double[]{1.0, 2.0}, 2);
            fail();
        } catch (Exception expected) {
        }

        assertEquals(1.0, Algorithm.nthElement(new double[]{1.0}, 0), 1.0);
        assertEquals(1.0, Algorithm.nthElement(new double[]{3.0, 1.0, 4.0, 2.0}, 0), 0.0);
        assertEquals(2.0, Algorithm.nthElement(new double[]{3.0, 1.0, 4.0, 2.0}, 1), 0.0);
        assertEquals(3.0, Algorithm.nthElement(new double[]{3.0, 1.0, 4.0, 2.0}, 2), 0.0);
        assertEquals(4.0, Algorithm.nthElement(new double[]{3.0, 1.0, 4.0, 2.0}, 3), 0.0);
    }

}
