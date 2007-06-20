/*
 * $Id: SmacRequestEditorTest.java,v 1.2 2007/04/12 17:06:25 marcop Exp $
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

package org.esa.beam.processor.smac.ui;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.processor.smac.SmacConstants;

public class SmacRequestEditorTest extends TestCase {

    SmacRequestEditor _reqGui;

    public SmacRequestEditorTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SmacRequestEditorTest.class);
    }

    @Override
    protected void setUp() {
        _reqGui = new SmacRequestEditor();
    }


    /*
     * testGetRequestReturnsRequestWithAllElements
     *
     * If getRequest was called after setRequest is called the method returnes
     * a valid Request Object with all required elements inside
     */
    public void testGetRequestReturnsRequestWithAllElements() throws ProcessorException {
        Vector<Request> testReq = new Vector<Request>();
        testReq.add(new Request());
        _reqGui.setRequests(testReq);
        Vector testRet = _reqGui.getRequests();
        Request request = (Request) testRet.elementAt(0);
        assertEquals(1, request.getNumInputProducts());
        assertEquals(1, request.getNumOutputProducts());
        assertEquals(0, request.getNumLogFileLocations());
        List<String> expPaNa = getExpectedParamNames();
        assertEquals(12, expPaNa.size());
        assertEquals(expPaNa.size(), request.getNumParameters());
        for (Iterator<String> iterator = expPaNa.iterator(); iterator.hasNext();) {
            String name = iterator.next();
            assertNotNull(request.getParameter(name));
        }
    }

    private static List<String> getExpectedParamNames() {
        List<String> paramNameList = new LinkedList<String>();
        paramNameList.add(SmacConstants.PRODUCT_TYPE_PARAM_NAME);
        paramNameList.add(SmacConstants.BANDS_PARAM_NAME);
        paramNameList.add(SmacConstants.AEROSOL_TYPE_PARAM_NAME);
        paramNameList.add(SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME);
        //paramNameList.add(SmacConstants.USE_LAT_LONG_CORRECT_PARAM_NAME);
        paramNameList.add(SmacConstants.USE_MERIS_ADS_PARAM_NAME);
        paramNameList.add(SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME);
        paramNameList.add(SmacConstants.OZONE_CONTENT_PARAM_NAME);
        paramNameList.add(SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME);
        paramNameList.add(SmacConstants.DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME);
        paramNameList.add(SmacConstants.BITMASK_PARAM_NAME);
        paramNameList.add(ProcessorConstants.LOG_PREFIX_PARAM_NAME);
        paramNameList.add(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);
        return paramNameList;
    }
}
