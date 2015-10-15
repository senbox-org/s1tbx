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

package org.esa.snap.dataio.envisat;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.core.util.Debug;

public class FieldInfoTest extends TestCase {

    public FieldInfoTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(FieldInfoTest.class);
    }

    @Override
    protected void setUp() {
    }

    @Override
    protected void tearDown() {
    }

    public void testFieldInfo() {
        Debug.traceMethodNotImplemented(this.getClass(), "testFieldInfo");
    }
    /*
    public void testCreateField() {
        assertTrue(false);
    }
    public void testGetNumDataElems() {
        assertTrue(false);
    }
    public void testGetSizeInBytes() {
        assertTrue(false);
    }
    public void testToString() {
        assertTrue(false);
    }
 */
}
