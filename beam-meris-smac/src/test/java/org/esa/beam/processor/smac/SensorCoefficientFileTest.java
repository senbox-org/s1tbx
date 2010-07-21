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

package org.esa.beam.processor.smac;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.GlobalTestConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SensorCoefficientFileTest extends TestCase {

    private File smacAuxDir;
    private String oldAuxdataPath;


    @Override
    protected void setUp() throws Exception {
        oldAuxdataPath = System.getProperty(SmacConstants.SMAC_AUXDATA_DIR_PROPERTY, "");
        String path = new File(GlobalTestConfig.getBeamTestDataOutputDirectory(), "auxdata/smac").getPath();
        System.setProperty(SmacConstants.SMAC_AUXDATA_DIR_PROPERTY, path);
        SmacProcessor processor = new SmacProcessor();
        processor.installAuxdata(); // just to extract auxdata
        smacAuxDir = processor.getAuxdataInstallDir();
        assertEquals(path, smacAuxDir.getPath());
    }

    @Override
    protected void tearDown() throws Exception {
        System.setProperty(SmacConstants.SMAC_AUXDATA_DIR_PROPERTY, oldAuxdataPath);
    }

    public static Test suite() {
        return new TestSuite(SensorCoefficientFileTest.class);
    }

    public void testReadFile() {
        SensorCoefficientFile file = new SensorCoefficientFile();

        // class should not accept null argument
        try {
            file.readFile(null);
            fail("illegal null argument shall not be accepted");
        } catch (IllegalArgumentException e) {
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        // class shall throw exception when file does not exist
        try {
            file.readFile("gbrmtzewz");
            fail("exception must be thrown when non existent file is passed in");
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

        // now read an existing file
        try {

            File toRead = new File(smacAuxDir, "coef_ASTER1_DES.dat");
            file.readFile(toRead.toString());
        } catch (FileNotFoundException e) {
            fail("must be able to read this file");
        } catch (IOException e) {
        }

        assertEquals(-0.002718, file.getAh2o(), 1e-12);
        assertEquals(0.758879, file.getNh2o(), 1e-12);
        assertEquals(-0.087688, file.getAo3(), 1e-12);
        assertEquals(0.994792, file.getNo3(), 1e-12);
        assertEquals(0.000000, file.getAo2(), 1e-12);
        assertEquals(0.000000, file.getNo2(), 1e-12);
        assertEquals(0.000000, file.getPo2(), 1e-12);
        assertEquals(0.000000, file.getAco2(), 1e-12);
        assertEquals(0.000000, file.getNco2(), 1e-12);
        assertEquals(0.000000, file.getPco2(), 1e-12);
        assertEquals(0.000000, file.getAch4(), 1e-12);
        assertEquals(0.000000, file.getNch4(), 1e-12);
        assertEquals(0.000000, file.getPch4(), 1e-12);
        assertEquals(0.000000, file.getAno2(), 1e-12);
        assertEquals(0.000000, file.getNno2(), 1e-12);
        assertEquals(0.000000, file.getPno2(), 1e-12);
        assertEquals(0.000000, file.getAco(), 1e-12);
        assertEquals(0.000000, file.getNco(), 1e-12);
        assertEquals(0.000000, file.getPco(), 1e-12);
        assertEquals(0.049583, file.getA0s(), 1e-12);
        assertEquals(0.202694, file.getA1s(), 1e-12);
        assertEquals(-0.069883, file.getA2s(), 1e-12);
        assertEquals(0.032950, file.getA3s(), 1e-12);
        assertEquals(1.122924, file.getA0T(), 1e-12);
        assertEquals(-0.171042, file.getA1T(), 1e-12);
        assertEquals(-0.087717, file.getA2T(), 1e-12);
        assertEquals(-0.243279, file.getA3T(), 1e-12);
        assertEquals(0.095735, file.getTaur(), 1e-12);
        assertEquals(0.080458, file.getSr(), 1e-12);
        assertEquals(1.665335e-16, file.getA0taup(), 1e-12);
        assertEquals(0.996574, file.getA1taup(), 1e-12);
        assertEquals(0.931251, file.getWo(), 1e-12);
        assertEquals(0.707801, file.getGc(), 1e-12);
        assertEquals(6.65108385405353e+00, file.getA0P(), 1e-12);
        assertEquals(-2.04145765556867e-01, file.getA1P(), 1e-12);
        assertEquals(2.47532478623969e-03, file.getA2P(), 1e-12);
        assertEquals(-1.37367474080701e-05, file.getA3P(), 1e-12);
        assertEquals(2.93421755343360e-08, file.getA4P(), 1e-12);
        assertEquals(-0.003809, file.getRest1(), 1e-12);
        assertEquals(-0.015628, file.getRest2(), 1e-12);
        assertEquals(-0.014170, file.getRest3(), 1e-12);
        assertEquals(-0.002414, file.getRest4(), 1e-12);
        assertEquals(-0.001115, file.getResr1(), 1e-12);
        assertEquals(-0.003992, file.getResr2(), 1e-12);
        assertEquals(0.025444, file.getResr3(), 1e-12);
        assertEquals(-0.008359, file.getResa1(), 1e-12);
        assertEquals(-0.03399, file.getResa2(), 1e-12);
        assertEquals(-0.042086, file.getResa3(), 1e-12);
        assertEquals(-0.012983, file.getResa4(), 1e-12);
    }

}