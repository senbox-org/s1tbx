package org.esa.beam.dataio.modis;

import junit.framework.TestCase;
import org.esa.beam.util.math.Range;

public class ModisUtilsTest extends TestCase {

    private static final String TestCoreString =
            "GROUP                  = INVENTORYMETADATA\n" +
            "  GROUPTYPE            = MASTERGROUP\n" +
            '\n' +
            "  GROUP                  = ECSDATAGRANULE\n" +
            '\n' +
            "    OBJECT                 = LOCALGRANULEID\n" +
            "      NUM_VAL              = 1\n" +
            "      VALUE                = \"MYD021KM.A2005297.2205.005.2005299100720.hdf\"\n" +
            "    END_OBJECT             = LOCALGRANULEID\n" +
            '\n' +
            "    OBJECT                 = PRODUCTIONDATETIME\n" +
            "      NUM_VAL              = 1\n" +
            "      VALUE                = \"2005-10-26T10:07:21.000Z\"\n" +
            "    END_OBJECT             = PRODUCTIONDATETIME\n" +
            '\n' +
            "    OBJECT                 = DAYNIGHTFLAG\n" +
            "      NUM_VAL              = 1\n" +
            "      VALUE                = \"Night\"\n" +
            "    END_OBJECT             = DAYNIGHTFLAG\n" +
            "END_GROUP              = ADDITIONALATTRIBUTES\n" +
            "GROUP                  = ORBITCALCULATEDSPATIALDOMAIN\n" +
            "   OBJECT                 = ORBITNUMBER\n" +
            "       CLASS                = \"1\"\n" +
            "       NUM_VAL              = 1\n" +
            "       VALUE                = 18490\n" +
            "   END_OBJECT           = ORBITNUMBER\n" +
            "END_GROUP              = ORBITCALCULATEDSPATIALDOMAIN\n" +
            '\n' +
            "END_GROUP              = INVENTORYMETADATA\n" +
            '\n' +
            "END";

    public void testExtractValueForKey() {
        assertEquals("Night", ModisUtils.extractValueForKey(TestCoreString, "DAYNIGHTFLAG"));
        assertEquals("2005-10-26T10:07:21.000Z", ModisUtils.extractValueForKey(TestCoreString, "PRODUCTIONDATETIME"));

        assertNull(ModisUtils.extractValueForKey(TestCoreString, "huppepup"));
    }

    public void testExtractIntegerValueFromObject() {
        assertEquals("18490", ModisUtils.extractValueForKey(TestCoreString, "ORBITNUMBER"));
    }

    public void testExtractValueForKeyHandlesStupidInput() {
        try {
            ModisUtils.extractValueForKey(null, "DAYNIGHTFLAG");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            ModisUtils.extractValueForKey(TestCoreString, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testGetIncrementOffset() {
        final String test1 = "3,8";
        final String test2 = "3,8,13,...";

        IncrementOffset incrementOffset = ModisUtils.getIncrementOffset(test1);
        assertNotNull(incrementOffset);
        assertEquals(2, incrementOffset.offset);
        assertEquals(5, incrementOffset.increment);

        incrementOffset = ModisUtils.getIncrementOffset(test2);
        assertNotNull(incrementOffset);
        assertEquals(2, incrementOffset.offset);
        assertEquals(5, incrementOffset.increment);
    }

    public void testGetRangeFromString() {
        final String test_1 = "0, 32767";
        final String test_2 = "6,2";

        Range result = ModisUtils.getRangeFromString(test_1);
        assertNotNull(result);
        assertEquals(0, (int) result.getMin());
        assertEquals(32767, (int) result.getMax());

        result = ModisUtils.getRangeFromString(test_2);
        assertNotNull(result);
        assertEquals(2, (int) result.getMin());
        assertEquals(6, (int) result.getMax());
    }
}