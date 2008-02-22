package org.esa.beam.util;

import junit.framework.TestCase;

import java.util.Set;
import java.util.HashSet;

public class IntMapTest extends TestCase {

    public void testIntMap() {
        IntMap intMap = new IntMap(-10, 110);

        assertEquals(0, intMap.size());

        intMap.put(-10000, -4);
        intMap.put(-1000, -3);
        intMap.put(-100, -2);
        intMap.put(-10, -1);
        intMap.put(1, 0);
        intMap.put(10, 1);
        intMap.put(100, 2);
        intMap.put(1000, 3);
        intMap.put(10000, 4);

        assertEquals(9, intMap.size());

        assertEquals(-4, intMap.get(-10000));
        assertEquals(-3, intMap.get(-1000));
        assertEquals(-2, intMap.get(-100));
        assertEquals(-1, intMap.get(-10));
        assertEquals(0, intMap.get(1));
        assertEquals(1, intMap.get(10));
        assertEquals(2, intMap.get(100));
        assertEquals(3, intMap.get(1000));
        assertEquals(4, intMap.get(10000));

        assertEquals(IntMap.NULL, intMap.get(-10000 + 1));
        assertEquals(IntMap.NULL, intMap.get(-1000 + 1));
        assertEquals(IntMap.NULL, intMap.get(-100 + 1));
        assertEquals(IntMap.NULL, intMap.get(-10 + 1));
        assertEquals(IntMap.NULL, intMap.get(1 - 1));
        assertEquals(IntMap.NULL, intMap.get(10 - 1));
        assertEquals(IntMap.NULL, intMap.get(100 - 1));
        assertEquals(IntMap.NULL, intMap.get(1000 - 1));
        assertEquals(IntMap.NULL, intMap.get(10000 - 1));

        intMap.remove(1);
        assertEquals(8, intMap.size());
        assertEquals(IntMap.NULL, intMap.get(1));

        intMap.remove(1000);
        assertEquals(7, intMap.size());
        assertEquals(IntMap.NULL, intMap.get(1000));

        intMap.remove(10000 - 1);
        assertEquals(7, intMap.size());

        try {
            intMap.put(1, IntMap.NULL);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testSequentialAccess() {
        IntMap intMap = new IntMap();
        assertEquals(0, intMap.size());
        for (int i = -10000; i <= 10000; i++) {
            intMap.put(i, i + 99);
        }
        for (int i = -10000; i <= 10000; i++) {
            assertEquals(i + 99, intMap.get(i));
        }
        assertEquals(20001, intMap.size());
        for (int i = -10000; i <= 10000; i++) {
            intMap.remove(i);
        }
        assertEquals(0, intMap.size());
    }

    public void testKeys() {
        IntMap intMap = new IntMap(10, 100);
        intMap.put(234, 5);
        intMap.put(2, 9);
        intMap.put(534, 1);
        intMap.put(22, 43);
        final int[] keys = intMap.keys();
        Set<Integer> keySet = new HashSet<Integer>();
        for (int key : keys) {
            keySet.add(key);
        }
        assertEquals(4, keySet.size());
        assertTrue(keySet.contains(234));
        assertTrue(keySet.contains(2));
        assertTrue(keySet.contains(534));
        assertTrue(keySet.contains(22));
    }
}
