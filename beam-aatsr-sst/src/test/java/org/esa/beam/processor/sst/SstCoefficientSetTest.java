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
package org.esa.beam.processor.sst;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SstCoefficientSetTest extends TestCase {

    private SstCoefficientSet _set;

    public SstCoefficientSetTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SstCoefficientSetTest.class);
    }

    @Override
    protected void setUp() {
        _set = new SstCoefficientSet();
        assertNotNull(_set);
    }

    /**
     * Tests for the correct functionality of the description setter and getter
     */
    public void testSetGetDescription() {
        String desc1 = "A coefficient description";
        String desc2 = "Another coefficient description";

        // initially the description shall be an emty string
        assertEquals("", _set.getDescription());

        // it shall not be possible to add null as description
        try {
            _set.setDescription(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // when setting a description we expect to get it back
        _set.setDescription(desc1);
        assertEquals(desc1, _set.getDescription());

        _set.setDescription(desc2);
        assertEquals(desc2, _set.getDescription());
    }

    /**
     * Tests the correct functionality of the coefficient accessor function
     */
    public void testAddGetCoefficients() {
        SstCoefficients coeffs1 = new SstCoefficients(1, 2);
        SstCoefficients coeffs2 = new SstCoefficients(3, 5);
        SstCoefficients coeffs3 = new SstCoefficients(6, 11);

        // initially there shall be no ranges contained
        assertEquals(0, _set.getNumCoefficients());

        // it shall not be possible to add null as coefficients
        try {
            _set.addCoefficients(null);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
        }

        // when we added one - the number must increase and we must be able to
        // get it back
        _set.addCoefficients(coeffs1);
        assertEquals(1, _set.getNumCoefficients());
        assertEquals(coeffs1, _set.getCoefficientsAt(0));

        // when we added more - the number must increase and we must be able to
        // get them back
        _set.addCoefficients(coeffs2);
        assertEquals(2, _set.getNumCoefficients());
        assertEquals(coeffs2, _set.getCoefficientsAt(1));

        _set.addCoefficients(coeffs3);
        assertEquals(3, _set.getNumCoefficients());
        assertEquals(coeffs3, _set.getCoefficientsAt(2));

        // it shall not be possible to retrieve coefficients out of index
        try {
            _set.getCoefficientsAt(-3);
            fail("exception expected");
        } catch (Exception e) {
        }

        try {
            _set.getCoefficientsAt(12);
            fail("exception expected");
        } catch (Exception e) {
        }
    }
}
