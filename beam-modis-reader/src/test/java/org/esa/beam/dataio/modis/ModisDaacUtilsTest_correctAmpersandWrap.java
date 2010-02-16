package org.esa.beam.dataio.modis;

import junit.framework.TestCase;

public class ModisDaacUtilsTest_correctAmpersandWrap extends TestCase {

    public void testStandardString() {
        final String corrected = ModisDaacUtils.correctAmpersandWrap(
                " any\n standard\r String\n\r without & wrap  ");

        assertEquals(
                " any\n standard\r String\n\r without & wrap  ",
                corrected);
    }

    public void testAmpersandWithoutWrap() {
        final String corrected = ModisDaacUtils.correctAmpersandWrap(
                "   dkjhaf & da\n" +
                "   lsmf\n");

        assertEquals(
                "   dkjhaf & da\n" +
                "   lsmf\n",
                corrected);
    }

    public void testAmpersandWithWrap() {
        final String corrected = ModisDaacUtils.correctAmpersandWrap(
                "   FistPartAnd&\n" +
                "   SecondPart");

        assertEquals(
                "   FistPartAndSecondPart",
                corrected);
    }

    public void testLeaveSpecialCharactersUnchanged() {
        final String corrected = ModisDaacUtils.correctAmpersandWrap("ßÄÖÜ?#~@|<>€µ}][{");

        assertEquals("ßÄÖÜ?#~@|<>€µ}][{", corrected);
    }

    public void testConcrete() {

        final String corrected = ModisDaacUtils.correctAmpersandWrap(
                "    OBJECT                 = LOCALGRANULEID\n" +
                "      NUM_VAL              = 1\n" +
                "      VALUE                = \"/mnt/juggle//TERRA/MOD_SS.MOD13A2.A2000&\n" +
                "          049.cs_bartonbe.005.2006257073624.hdf\"\n" +
                "    END_OBJECT             = LOCALGRANULEID");

        assertEquals(
                "    OBJECT                 = LOCALGRANULEID\n" +
                "      NUM_VAL              = 1\n" +
                "      VALUE                = \"/mnt/juggle//TERRA/MOD_SS.MOD13A2.A2000049.cs_bartonbe.005.2006257073624.hdf\"\n" +
                "    END_OBJECT             = LOCALGRANULEID",
                corrected);
    }
}