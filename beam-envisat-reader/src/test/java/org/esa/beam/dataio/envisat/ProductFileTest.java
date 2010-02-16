/*
 * $Id: ProductFileTest.java,v 1.1 2006/09/18 06:34:40 marcop Exp $
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

public class ProductFileTest extends TestCase {

    public ProductFileTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ProductFileTest.class);
    }

    @Override
    protected void setUp() {
    }

    @Override
    protected void tearDown() {
    }

    public void testProductFile() {
        Debug.traceMethodNotImplemented(this.getClass(), "testProductFile");
    }
    /*
    public void testClose() {
        Debug.traceMethodNotImplemented(this.getClass(), "testClose");
    }
    public void testGetDSD() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetDSD");
    }
    public void testGetDSDAt() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetDSDAt");
    }
    public void testGetDSDIndex() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetDSDIndex");
    }
    public void testGetDSDs() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetDSDs");
    }
    public void testGetDataInputStream() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetDataInputStream");
    }
    public void testGetDatasetNames() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetDatasetNames");
    }
    public void testGetLineLength() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetLineLength");
    }
    public void testGetLogSink() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetLogSink");
    }
    public void testGetMPH() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetMPH");
    }
    public void testGetNumDSDs() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetNumDSDs");
    }
    public void testGetProductId() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetProductId");
    }
    public void testGetProductType() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetProductType");
    }
    public void testGetRecordReader() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetRecordReader");
    }
    public void testGetSPH() {
        Debug.traceMethodNotImplemented(this.getClass(), "testGetSPH");
    }
    public void testIsEnvisatFile() {
        Debug.traceMethodNotImplemented(this.getClass(), "testIsEnvisatFile");
    }
    public void testLogDebug() {
        Debug.traceMethodNotImplemented(this.getClass(), "testLogDebug");
    }
    public void testLogError() {
        Debug.traceMethodNotImplemented(this.getClass(), "testSetLogSink");
    }
    public void testLogInfo() {
        Debug.traceMethodNotImplemented(this.getClass(), "testLogInfo");
    }
    public void testLogWarning() {
        Debug.traceMethodNotImplemented(this.getClass(), "testLogWarning");
    }
    public void testOpen() {
        Debug.traceMethodNotImplemented(this.getClass(), "testOpen");
    }
    public void testPostProcessMPH() {
        Debug.traceMethodNotImplemented(this.getClass(), "testPostProcessMPH");
    }
    public void testPostProcessSPH() {
        Debug.traceMethodNotImplemented(this.getClass(), "testPostProcessSPH");
    }
    public void testSetLogSink() {
        Debug.traceMethodNotImplemented(this.getClass(), "testSetLogSink");
    }
 */
}