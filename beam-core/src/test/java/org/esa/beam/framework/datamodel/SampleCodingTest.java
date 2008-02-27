/*
 * $Id: BitmaskDefTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.datamodel;

import java.awt.Color;

import junit.framework.TestCase;

public class SampleCodingTest extends TestCase {

    public void testFlagCoding() {
        final FlagCoding fc = new FlagCoding("FC");
        fc.addFlag("F1", 0x040, "");
        fc.addFlag("F2", 0x800, "");
        assertEquals(2, fc.getNumAttributes());
        assertEquals(0x040, fc.getFlagMask("F1"));
        assertEquals(0x800, fc.getFlagMask("F2"));
        testIntValuesAllowedOnly(fc);
        testScalarValuesAllowedOnly(fc);
    }

    public void testIndexCoding() {
        final IndexCoding ic = new IndexCoding("IC");
        ic.addIndex("I1", 100, "");
        ic.addIndex("I2", 300, "");
        assertEquals(2, ic.getNumAttributes());
        assertEquals(100, ic.getIndexValue("I1"));
        assertEquals(300, ic.getIndexValue("I2"));
        testIntValuesAllowedOnly(ic);
        testScalarValuesAllowedOnly(ic);
    }

    public void testIntValuesAllowedOnly(SampleCoding sampleCoding) {
        final int numAttributes = sampleCoding.getNumAttributes();
        sampleCoding.addAttribute(new MetadataAttribute("A", ProductData.TYPE_INT32));
        sampleCoding.addAttribute(new MetadataAttribute("B", ProductData.TYPE_INT32));
        assertEquals(numAttributes + 2, sampleCoding.getNumAttributes());

        try {
            sampleCoding.addAttribute(new MetadataAttribute("C", ProductData.TYPE_FLOAT32));
            fail("IllegalArgumentException?");
        } catch (IllegalArgumentException e) {
            // OK
        }

        try {
            sampleCoding.addAttribute(new MetadataAttribute("C", ProductData.TYPE_FLOAT64));
            fail("IllegalArgumentException?");
        } catch (IllegalArgumentException e) {
            // OK
        }

        assertEquals(numAttributes + 2, sampleCoding.getNumAttributes());
    }

    public void testScalarValuesAllowedOnly(SampleCoding sampleCoding) {
        final int numAttributes = sampleCoding.getNumAttributes();
        sampleCoding.addAttribute(new MetadataAttribute("C", ProductData.TYPE_INT32, 1));
        sampleCoding.addAttribute(new MetadataAttribute("D", ProductData.TYPE_INT32, 1));
        assertEquals(numAttributes + 2, sampleCoding.getNumAttributes());

        try {
            sampleCoding.addAttribute(new MetadataAttribute("C", ProductData.TYPE_INT32, 2));
            fail("IllegalArgumentException?");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }
}