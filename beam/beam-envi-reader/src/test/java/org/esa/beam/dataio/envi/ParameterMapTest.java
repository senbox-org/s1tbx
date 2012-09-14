package org.esa.beam.dataio.envi;

import junit.framework.TestCase;

public class ParameterMapTest extends TestCase {

    public void testMap_sameSize() {
        int[] indices = new int[] {
            2,0,1
        };

        ParameterMap map = new ParameterMap(3, indices);

        double[] sourceValues = new double[] {
                23, 34, 45
        };

        double[] targetValues = map.transform(sourceValues);
        assertEquals(3, targetValues.length);
        assertEquals(34, targetValues[0], 1e-8);
        assertEquals(45, targetValues[1], 1e-8);
        assertEquals(23, targetValues[2], 1e-8);

        indices = new int[] {
            3,1,2,0
        };
        map = new ParameterMap(4, indices);

        sourceValues = new double[] {
                23, 34, 45, 56
        };
        targetValues = map.transform(sourceValues);
        assertEquals(4, targetValues.length);
        assertEquals(56, targetValues[0], 1e-8);
        assertEquals(34, targetValues[1], 1e-8);
        assertEquals(45, targetValues[2], 1e-8);
        assertEquals(23, targetValues[3], 1e-8);
    }
}
