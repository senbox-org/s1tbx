/*
 * $Id: SensorCoefficientManagerTest.java,v 1.4 2006/11/15 09:22:13 norman Exp $
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

package org.esa.beam.processor.smac;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.GlobalTestConfig;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class SensorCoefficientManagerTest extends TestCase {

    private File _auxdataDir;
    private String oldAuxdataPath;


    @Override
     protected void setUp() throws Exception {
         oldAuxdataPath = System.getProperty(SmacConstants.SMAC_AUXDATA_DIR_PROPERTY, "");
         String path = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(), "auxdata/smac").getPath();
         System.setProperty(SmacConstants.SMAC_AUXDATA_DIR_PROPERTY, path);
         SmacProcessor processor = new SmacProcessor();
         processor.installAuxdata(); // just to extract auxdata
         _auxdataDir = processor.getAuxdataInstallDir();
         assertEquals(path, _auxdataDir.getPath());
     }

    @Override
    protected void tearDown() throws Exception {
        System.setProperty(SmacConstants.SMAC_AUXDATA_DIR_PROPERTY, oldAuxdataPath);
    }
    public static Test suite() {
        return new TestSuite(SensorCoefficientManagerTest.class);
    }

    /**
     * Tests the functionality of the constructors.
     */
    public void testSensorCoefficientManager() {
        SensorCoefficientManager mgr = new SensorCoefficientManager();

        // when using default constructor, no file shall be returned upon request
        assertNull(mgr.getCoefficientFile("MERIS", "radiance_1", "CONT"));

        // the URL constructor shall not accept a null argument
        try {
            mgr = new SensorCoefficientManager(null);
            fail("exception expected here");
        } catch (IllegalArgumentException e) {
        } catch (IOException e) {
            fail("wrong exception type");
        }

        // when insering a valid url, we shall retrieve a coefficient file
        URL url = null;

        try {
//            url = new URL("file", "", _location);
            url = _auxdataDir.toURL();
        } catch (MalformedURLException e) {
        }

        try {
            mgr = new SensorCoefficientManager(url);
        } catch (IOException e) {
        }
        assertNotNull(mgr.getCoefficientFile("MERIS", "radiance_2", "DES"));

    }

    /**
     * Tests the functionality of getCoefficientFIle
     */
    public void testGetCoefficientFile() {
        URL url = null;
        SensorCoefficientManager mgr;

        try {
//            url = new URL("file", "", _location);
            url = _auxdataDir.toURL();
        } catch (MalformedURLException e) {
        }

        try {
            mgr = new SensorCoefficientManager(url);
            assertNotNull(mgr.getCoefficientFile("MERIS", "radiance_2", "DES"));
        } catch (IOException e) {
        }
    }

    /**
     * Tests the functionality for setUrl
     */
    public void testSetURL() {
        // it must not be possible to set a null argument
        SensorCoefficientManager mgr = new SensorCoefficientManager();

        try {
            mgr.setURL(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        } catch (IOException e) {
        }

        // if we set a valid url - return something when we ask for it :-)
        URL url;
        try {
//            url = new URL("file", "", _location);
            url = _auxdataDir.toURL();
            mgr.setURL(url);
            assertNotNull(mgr.getCoefficientFile("MERIS", "radiance_3", "CONT"));
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
    }

}