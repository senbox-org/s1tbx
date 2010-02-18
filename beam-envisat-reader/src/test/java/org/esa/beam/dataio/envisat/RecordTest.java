/*
 * $Id: RecordTest.java,v 1.1 2006/09/18 06:34:40 marcop Exp $
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

package org.esa.beam.dataio.envisat;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.util.Debug;

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