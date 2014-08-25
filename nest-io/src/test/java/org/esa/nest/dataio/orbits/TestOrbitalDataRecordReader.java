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
package org.esa.nest.dataio.orbits;

import org.junit.Test;
import org.junit.Assert;

/**
 * OrbitalDataRecordReader Tester.
 *
 * @author lveci
 */
public class TestOrbitalDataRecordReader {

    String envisatOrbitFilePath = "org/esa/nest/data/envisat_ODR.051";
    String ers1OrbitFilePath = "org/esa/nest/data/ers1_ODR.079";
    String ers2OrbitFilePath = "org/esa/nest/data/ers2_ODR.015";

    @Test
    public void testOpenFile() {

        OrbitalDataRecordReader reader = new OrbitalDataRecordReader();

        Assert.assertTrue(reader.OpenOrbitFile(envisatOrbitFilePath));
    }

    @Test
    public void testReadHeader() {

        OrbitalDataRecordReader reader = new OrbitalDataRecordReader();

        if (reader.OpenOrbitFile(envisatOrbitFilePath)) {

            reader.parseHeader1();
            reader.parseHeader2();
        } else {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testReadERS1OrbitFiles() throws Exception {
        System.out.print("ERS1 ORD ");
        readOrbitFile(ers1OrbitFilePath);
    }

    @Test
    public void testReadERS2OrbitFile() throws Exception {
        System.out.print("ERS2 ORD ");
        readOrbitFile(ers2OrbitFilePath);
    }

    @Test
    public void testReadEnvisatOrbitFile() throws Exception {
        System.out.print("Envisat ORD ");
        readOrbitFile(envisatOrbitFilePath);
    }

    private static void readOrbitFile(String path) throws Exception {
        final OrbitalDataRecordReader reader = new OrbitalDataRecordReader();
        boolean res = reader.readOrbitFile(path);
        assert(res);

        OrbitalDataRecordReader.OrbitDataRecord[] orbits = reader.getDataRecords();
        System.out.print("Num Orbits " + orbits.length);
        for (int i = 0; i < 2; ++i) {
            System.out.print(" Orbit time " + orbits[i].time);
            System.out.print(" lat " + orbits[i].latitude);
            System.out.print(" lng " + orbits[i].longitude);
            System.out.print(" hgt " + orbits[i].heightOfCenterOfMass);
            System.out.println();
        }
    }

}
