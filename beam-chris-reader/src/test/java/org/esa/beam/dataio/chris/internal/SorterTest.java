package org.esa.beam.dataio.chris.internal;

import junit.framework.TestCase;

/**
 * Test methods for class {@link Sorter}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SorterTest extends TestCase {

    public void testNthElement() {

        try {
            Sorter.nthElement(null, 0);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            Sorter.nthElement(new double[0], 0);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            Sorter.nthElement(new double[]{1.0, 2.0}, -1);
            fail();
        } catch (Exception expected) {
        }

        try {
            Sorter.nthElement(new double[]{1.0, 2.0}, 2);
            fail();
        } catch (Exception expected) {
        }

        assertEquals(1.0, Sorter.nthElement(new double[]{1.0}, 0), 1.0);
        assertEquals(1.0, Sorter.nthElement(new double[]{3.0, 1.0, 4.0, 2.0}, 0), 0.0);
        assertEquals(2.0, Sorter.nthElement(new double[]{3.0, 1.0, 4.0, 2.0}, 1), 0.0);
        assertEquals(3.0, Sorter.nthElement(new double[]{3.0, 1.0, 4.0, 2.0}, 2), 0.0);
        assertEquals(4.0, Sorter.nthElement(new double[]{3.0, 1.0, 4.0, 2.0}, 3), 0.0);
    }

}
