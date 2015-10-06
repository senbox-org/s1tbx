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

public class RecordTest extends TestCase {

    public RecordTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(RecordTest.class);
    }

    @Override
    protected void setUp() {
    }

    @Override
    protected void tearDown() {
    }

    public void testCreate() {
        Debug.traceMethodNotImplemented(this.getClass(), "testCreate");
    }
    /**
     public void testGetField() {
     assertTrue(false);
     }
     public void testGetFieldAt() {
     assertTrue(false);
     }
     public void testGetFieldIndex() {
     assertTrue(false);
     }
     public void testGetInfo() {
     assertTrue(false);
     }
     public void testGetName() {
     assertTrue(false);
     }
     public void testGetNumFields() {
     assertTrue(false);
     }
     public void testGetSizeInBytes() {
     assertTrue(false);
     }
     public void testReadFrom() {
     assertTrue(false);
     }
     public void testToString() {
     assertTrue(false);
     }
     */
}
