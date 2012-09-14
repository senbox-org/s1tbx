/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.netcdf;

import junit.framework.TestCase;

import java.io.IOException;


/**
 *
 * @author lveci
 */
public class TestNetCDFReader extends TestCase {

    private final String geoTiffFile = "F:\\data\\GIS\\Tiff\\Osaka Japan\\tc_osaka_geo.tif";
    private final String netCDFFile = "C:\\Data\\netcdf_data\\tos_O1_2001-2002.nc";
    private final String hdfFile = "C:\\Data\\netcdf_data\\PALAPR01.h5";

    public TestNetCDFReader(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testOpenNetCDF() throws IOException {
/*        NetcdfDataset ds = NetcdfDataset.openDataset(netCDFFile);

        List<Variable> varList = ds.getVariables();

        List<Group> groupList = ds.getRootGroup().getGroups();
         */

    }

    public void testOpenHDF() throws IOException {
  /*      NetcdfDataset ds = NetcdfDataset.openDataset(hdfFile);

        List<Variable> varList = ds.getVariables();

        List<Group> groupList = ds.getRootGroup().getGroups();

        */
    }

    public void testWriteHDF() throws IOException {

       //NetcdfFileWriteable netCDFWriteable = NetcdfFileWriteable.createNew("ha");




    }
}