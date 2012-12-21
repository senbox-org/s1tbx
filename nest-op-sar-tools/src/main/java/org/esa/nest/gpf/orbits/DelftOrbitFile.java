/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf.orbits;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.dataio.OrbitalDataRecordReader;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Orbits;
import org.esa.nest.util.Settings;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * DORIS Orbit File
 */
public class DelftOrbitFile extends BaseOrbitFile {

    private OrbitalDataRecordReader delftReader = null;
    
    public DelftOrbitFile(final String orbitType, final MetadataElement absRoot,
                          final Product sourceProduct) throws Exception {
        super(orbitType, absRoot);

        init(sourceProduct);
    }

    /**
     * Get orbit information for given time.
     * @param utc The UTC in days.
     * @return The orbit information.
     * @throws Exception The exceptions.
     */
    public Orbits.OrbitData getOrbitData(final double utc) throws Exception {

        final Orbits.OrbitData orbitData = new Orbits.OrbitData();

        final OrbitalDataRecordReader.OrbitVector orb = delftReader.getOrbitVector(utc);
        orbitData.xPos = orb.xPos;
        orbitData.yPos = orb.yPos;
        orbitData.zPos = orb.zPos;
        orbitData.xVel = orb.xVel;
        orbitData.yVel = orb.yVel;
        orbitData.zVel = orb.zVel;

        return orbitData;
    }

    /**
     * Get DELFT orbit file.
     * @param sourceProduct the input product
     * @throws Exception The exceptions.
     */
    private void init(final Product sourceProduct) throws Exception {

        delftReader = OrbitalDataRecordReader.getInstance();
        // get product start time
        final Date startDate = sourceProduct.getStartTime().getAsDate();

        // find orbit file in the folder
        orbitFile = FindDelftOrbitFile(delftReader, startDate);

        if(orbitFile == null) {
            throw new IOException("Unable to find suitable orbit file.\n" +
                    "Please refer to http://www.deos.tudelft.nl/ers/precorbs/orbits/ \n" +
                    "ERS1 orbits are available until 1996\n" +
                    "ERS2 orbits are available until 2003\n" +
                    "ENVISAT orbits are available until 2008");
        }
    }

    /**
     * Find DELFT orbit file.
     * @param delftReader The DELFT oribit reader.
     * @param productDate The start date of the product.
     * @return The orbit file.
     * @throws Exception The exceptions.
     */
    private File FindDelftOrbitFile(final OrbitalDataRecordReader delftReader, final Date productDate)
            throws Exception  {

        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);

        // construct path to the orbit file folder
        String orbitPathStr = "";
        String delftFTPPath = "";
        if(mission.equals("ENVISAT")) {
            orbitPathStr = Settings.instance().get("OrbitFiles/delftEnvisatOrbitPath");
            delftFTPPath = Settings.instance().get("OrbitFiles/delftFTP_ENVISAT_precise_remotePath");
        } else if(mission.equals("ERS1")) {
            orbitPathStr = Settings.instance().get("OrbitFiles/delftERS1OrbitPath");
            delftFTPPath = Settings.instance().get("OrbitFiles/delftFTP_ERS1_precise_remotePath");
        } else if(mission.equals("ERS2")) {
            orbitPathStr = Settings.instance().get("OrbitFiles/delftERS2OrbitPath");
            delftFTPPath = Settings.instance().get("OrbitFiles/delftFTP_ERS2_precise_remotePath");
        }
        final File orbitPath = new File(orbitPathStr);
        final String delftFTP = Settings.instance().get("OrbitFiles/delftFTP");

        if(!orbitPath.exists())
            orbitPath.mkdirs();

        // find arclist file, then get the arc# of the orbit file
        final File arclistFile = new File(orbitPath, "arclist");
        if (!arclistFile.exists()) {
            if(!getRemoteFile(delftFTP, delftFTPPath, arclistFile))
                return null;
        }

        int arcNum = OrbitalDataRecordReader.getArcNumber(arclistFile, productDate);
        if (arcNum == OrbitalDataRecordReader.invalidArcNumber) {
            // force refresh of arclist file
            if(!getRemoteFile(delftFTP, delftFTPPath, arclistFile))
                return null;
            arcNum = OrbitalDataRecordReader.getArcNumber(arclistFile, productDate);
            if (arcNum == OrbitalDataRecordReader.invalidArcNumber)
                return null;
        }

        String orbitFileName = orbitPath.getAbsolutePath() + File.separator + "ODR.";
        if (arcNum < 10) {
            orbitFileName += "00" + arcNum;
        } else if (arcNum < 100) {
            orbitFileName += "0" + arcNum;
        } else {
            orbitFileName += arcNum;
        }

        final File orbitFile = new File(orbitFileName);
        if (!orbitFile.exists()) {
            if(!getRemoteFile(delftFTP, delftFTPPath, orbitFile))
                return null;
        }

        // read content of the orbit file
        delftReader.readOrbitFile(orbitFile.getAbsolutePath());

        return orbitFile;
    }
}