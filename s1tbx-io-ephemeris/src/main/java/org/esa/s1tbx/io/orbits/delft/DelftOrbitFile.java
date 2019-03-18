/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.orbits.delft;

import org.esa.s1tbx.io.orbits.BaseOrbitFile;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Orbits;
import org.esa.snap.engine_utilities.util.Settings;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * DORIS Orbit File
 */
public class DelftOrbitFile extends BaseOrbitFile {

    public static final String DELFT_PRECISE = "DELFT Precise";

    private OrbitalDataRecordReader delftReader = null;
    private final Product sourceProduct;

    public DelftOrbitFile(final MetadataElement absRoot,
                          final Product sourceProduct) throws Exception {
        super(absRoot);
        this.sourceProduct = sourceProduct;        
    }

    public String[] getAvailableOrbitTypes() {
        return new String[] { DELFT_PRECISE };
    }

    public File retrieveOrbitFile(final String orbitType) throws Exception {
        delftReader = OrbitalDataRecordReader.getInstance();
        // get product start time
        final Date startDate = sourceProduct.getStartTime().getAsDate();

        // find orbit file in the folder
        orbitFile = FindDelftOrbitFile(delftReader, startDate);

        if (orbitFile == null) {
            throw new IOException("Unable to find suitable orbit file.\n" +
                    "Please refer to http://www.deos.tudelft.nl/ers/precorbs/orbits/ \n" +
                    "ERS1 orbits are available until 1996\n" +
                    "ERS2 orbits are available until 2003\n" +
                    "ENVISAT orbits are available until 2008");
        }

        return orbitFile;
    }

    /**
     * Get orbit information for given time.
     *
     * @param utc The UTC in days.
     * @return The orbit information.
     * @throws Exception The exceptions.
     */
    public Orbits.OrbitVector getOrbitData(final double utc) throws Exception {

        final OrbitalDataRecordReader.OrbitVector orb = delftReader.getOrbitVector(utc);

        return new Orbits.OrbitVector(utc,
                orb.xPos, orb.yPos, orb.zPos,
                orb.xVel, orb.yVel, orb.zVel);
    }

    /**
     * Find DELFT orbit file.
     *
     * @param delftReader The DELFT oribit reader.
     * @param productDate The start date of the product.
     * @return The orbit file.
     * @throws Exception The exceptions.
     */
    private File FindDelftOrbitFile(final OrbitalDataRecordReader delftReader, final Date productDate)
            throws Exception {

        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);

        // construct path to the orbit file folder
        String orbitPathStr = "";
        String delftFTPPath = "";
        switch (mission) {
            case "ENVISAT":
                orbitPathStr = Settings.getPath("OrbitFiles.delftEnvisatOrbitPath");
                delftFTPPath = Settings.getPath("OrbitFiles.delftFTP_ENVISAT_precise_remotePath");
                break;
            case "ERS1":
                orbitPathStr = Settings.getPath("OrbitFiles.delftERS1OrbitPath");
                delftFTPPath = Settings.getPath("OrbitFiles.delftFTP_ERS1_precise_remotePath");
                break;
            case "ERS2":
                orbitPathStr = Settings.getPath("OrbitFiles.delftERS2OrbitPath");
                delftFTPPath = Settings.getPath("OrbitFiles.delftFTP_ERS2_precise_remotePath");
                break;
        }
        final File orbitPath = new File(orbitPathStr);
        final String delftFTP = Settings.instance().get("OrbitFiles.delftFTP");

        if (!orbitPath.exists()) {
            if(!orbitPath.mkdirs()) {
                throw new IOException("Failed to create directory '" + orbitPath + "'.");
            }
        }

        // find arclist file, then get the arc# of the orbit file
        final File arclistFile = new File(orbitPath, "arclist");
        if (!arclistFile.exists()) {
            if (!getRemoteFile(delftFTP, delftFTPPath, arclistFile))
                return null;
        }

        int arcNum = OrbitalDataRecordReader.getArcNumber(arclistFile, productDate);
        if (arcNum == OrbitalDataRecordReader.invalidArcNumber) {
            // force refresh of arclist file
            if (!getRemoteFile(delftFTP, delftFTPPath, arclistFile))
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
            if (!getRemoteFile(delftFTP, delftFTPPath, orbitFile))
                return null;
        }

        // read content of the orbit file
        delftReader.readOrbitFile(orbitFile.toPath());

        return orbitFile;
    }
}
