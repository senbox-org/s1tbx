package org.esa.snap.binning.support;


import org.junit.Test;

import static org.junit.Assert.*;

public class GrowableVectorTest {
    @Test
    public void testGrowth() {
        GrowableVector v = new GrowableVector(2);

        assertEquals(0, v.size());
        v.add(2.1F);
        v.add(1.9F);
        v.add(3.4F);
        assertEquals(3, v.size());
        assertEquals(2.1F, v.get(0), 1E-5F);
        assertEquals(1.9F, v.get(1), 1E-5F);
        assertEquals(3.4F, v.get(2), 1E-5F);

        float[] elements = v.getElements();
        assertNotNull(elements);
        assertEquals(3, elements.length);
        assertEquals(2.1F, elements[0], 1E-5F);
        assertEquals(1.9F, elements[1], 1E-5F);
        assertEquals(3.4F, elements[2], 1E-5F);

        for (int i = 0; i < 1000; i++) {
            v.add(i * 0.1F);
        }
        assertEquals(1003, v.size());
        assertEquals(0.0F, v.get(3), 1E-5F);
        assertEquals(0.1F, v.get(4), 1E-5F);
        assertEquals(0.2F, v.get(5), 1E-5F);

        elements = v.getElements();
        assertNotNull(elements);
        assertEquals(1003, elements.length);
        assertEquals(0.0F, elements[3], 1E-5F);
        assertEquals(0.1F, elements[4], 1E-5F);
        assertEquals(0.2F, elements[5], 1E-5F);
    }

    @Test
    public void testToString() {
        GrowableVector v = new GrowableVector(2);

        assertEquals("[]", v.toString());

        v.add(2.1F);
        v.add(1.9F);
        v.add(3.4F);

        assertEquals("[2.1, 1.9, 3.4]", v.toString());
    }
}
