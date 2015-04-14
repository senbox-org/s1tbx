package org.esa.snap.statistics.tools;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MapInverterTest {

    @Test
    public void testInverting() {
        //preparation
        final Map<String, String> map = new HashMap<String, String>();
        map.put("Key1", "Value1");
        map.put("Key2", "Value2");
        map.put("Key3", "Value3");

        //execution
        final Map<String, String> swappedMap = MapInverter.createInvertedTreeMap(map);

        //verification
        assertNotNull(swappedMap);
        assertEquals(3, swappedMap.size());
        assertEquals("Key1", swappedMap.get("Value1"));
        assertEquals("Key2", swappedMap.get("Value2"));
        assertEquals("Key3", swappedMap.get("Value3"));
    }

    @Test
    public void testIllegalArgumentException_IfMapNotContainsOnlyUniqueValues() {
        //preparation
        final Map<String, String> map = new HashMap<String, String>();
        map.put("Key1", "Value1");
        map.put("Key2", "Value"); // not unique
        map.put("Key3", "Value"); // not unique

        //execution
        try {
            MapInverter.createInvertedTreeMap(map);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            //Expected
        } catch (Throwable e) {
            fail("IllegalArgumentException expected");
        }
    }
}
