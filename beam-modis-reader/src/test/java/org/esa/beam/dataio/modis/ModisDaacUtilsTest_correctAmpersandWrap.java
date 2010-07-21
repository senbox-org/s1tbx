/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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