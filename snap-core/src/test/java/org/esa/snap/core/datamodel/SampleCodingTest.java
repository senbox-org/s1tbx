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
package org.esa.snap.core.datamodel;

import org.junit.Assert;
import org.junit.Test;

public class SampleCodingTest {

    @Test
    public void testFlagCoding() {
        final FlagCoding fc = new FlagCoding("FC");
        fc.addFlag("F1", 0x040, "");
        fc.addFlag("F2", 0x800, "");
        fc.addFlag("F3", 0x80000000, "last bit");
        Assert.assertEquals(3, fc.getNumAttributes());
        Assert.assertEquals(0x040, fc.getFlagMask("F1"));
        Assert.assertEquals(0x800, fc.getFlagMask("F2"));
        Assert.assertEquals(0x80000000, fc.getFlagMask("F3"));
        Assert.assertEquals(0x80000000L, fc.getFlag("F3").getData().getElemUInt());
        testIntValuesAllowedOnly(fc);
        testScalarValuesAllowedOnly(fc);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddingDuplicatedFlagName() {
        final FlagCoding fc = new FlagCoding("FC");
        fc.addFlag("F1", 0x040, "");
        fc.addFlag("F1", 0x800, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddingDuplicatedFlagName_ByAttribute() {
        final FlagCoding fc = new FlagCoding("FC");
        fc.addFlag("F1", 0x040, "");
        fc.addAttribute(new MetadataAttribute("F1", ProductData.TYPE_INT8));
    }

    @Test
    public void testIndexCoding() {
        final IndexCoding ic = new IndexCoding("IC");
        ic.addIndex("I1", 100, "");
        ic.addIndex("I2", 300, "");
        Assert.assertEquals(2, ic.getNumAttributes());
        Assert.assertEquals(100, ic.getIndexValue("I1"));
        Assert.assertEquals(300, ic.getIndexValue("I2"));
        testIntValuesAllowedOnly(ic);
        testScalarValuesAllowedOnly(ic);
    }

    private void testIntValuesAllowedOnly(SampleCoding sampleCoding) {
        final int numAttributes = sampleCoding.getNumAttributes();
        sampleCoding.addAttribute(new MetadataAttribute("A", ProductData.TYPE_INT32));
        sampleCoding.addAttribute(new MetadataAttribute("B", ProductData.TYPE_INT32));
        Assert.assertEquals(numAttributes + 2, sampleCoding.getNumAttributes());

        try {
            sampleCoding.addAttribute(new MetadataAttribute("C", ProductData.TYPE_FLOAT32));
            Assert.fail("IllegalArgumentException?");
        } catch (IllegalArgumentException e) {
            // OK
        }

        try {
            sampleCoding.addAttribute(new MetadataAttribute("C", ProductData.TYPE_FLOAT64));
            Assert.fail("IllegalArgumentException?");
        } catch (IllegalArgumentException e) {
            // OK
        }

        Assert.assertEquals(numAttributes + 2, sampleCoding.getNumAttributes());
    }

    private void testScalarValuesAllowedOnly(SampleCoding sampleCoding) {
        final int numAttributes = sampleCoding.getNumAttributes();
        sampleCoding.addAttribute(new MetadataAttribute("C", ProductData.TYPE_INT32, 1));
        sampleCoding.addAttribute(new MetadataAttribute("D", ProductData.TYPE_INT32, 1));
        Assert.assertEquals(numAttributes + 2, sampleCoding.getNumAttributes());

        try {
            sampleCoding.addAttribute(new MetadataAttribute("E", ProductData.createInstance(new int[0]), true));
            Assert.fail("IllegalArgumentException?");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }
}
