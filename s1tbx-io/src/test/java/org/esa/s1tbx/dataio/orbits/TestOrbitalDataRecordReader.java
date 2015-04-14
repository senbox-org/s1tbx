/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.dataio.orbits;

import org.esa.snap.util.TestUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * OrbitalDataRecordReader Tester.
 *
 * @author lveci
 */
public class TestOrbitalDataRecordReader {

    private final static String envisatOrbitFilePath = "org/esa/s1tbx/dataio/orbits/envisat_ODR.051";
    private final static String ers1OrbitFilePath = "org/esa/s1tbx/dataio/orbits/ers1_ODR.079";
    private final static String ers2OrbitFilePath = "org/esa/s1tbx/dataio/orbits/ers2_ODR.015";

    @Test
    public void testOpenFile() {

        final OrbitalDataRecordReader reader = new OrbitalDataRecordReader();

        Assert.assertTrue(reader.OpenOrbitFile(envisatOrbitFilePath));
    }

    @Test
    public void testReadHeader() {

        final OrbitalDataRecordReader reader = new OrbitalDataRecordReader();

        if (reader.OpenOrbitFile(envisatOrbitFilePath)) {

            reader.parseHeader1();
            reader.parseHeader2();
        } else {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testReadERS1OrbitFiles() throws Exception {
        readOrbitFile("ERS1 ORD", ers1OrbitFilePath);
    }

    @Test
    public void testReadERS2OrbitFile() throws Exception {
        readOrbitFile("ERS2 ORD", ers2OrbitFilePath);
    }

    @Test
    public void testReadEnvisatOrbitFile() throws Exception {
        readOrbitFile("Envisat ORD", envisatOrbitFilePath);
    }

    private static void readOrbitFile(final String name, final String path) throws Exception {
        final OrbitalDataRecordReader reader = new OrbitalDataRecordReader();
        final boolean res = reader.readOrbitFile(path);
        assert(res);

        final OrbitalDataRecordReader.OrbitDataRecord[] orbits = reader.getDataRecords();
        final StringBuilder str = new StringBuilder(name+ " Num Orbits " + orbits.length);
        for (int i = 0; i < 2; ++i) {
            str.append(" Orbit time " + orbits[i].time);
            str.append(" lat " + orbits[i].latitude);
            str.append(" lng " + orbits[i].longitude);
            str.append(" hgt " + orbits[i].heightOfCenterOfMass);
        }
        TestUtils.log.info(str.toString());
    }

}
