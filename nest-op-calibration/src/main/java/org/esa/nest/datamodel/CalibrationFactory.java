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
package org.esa.nest.datamodel;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.nest.gpf.*;

/**
* The abstract base class for all calibration operators intended to be extended by clients.
 * The following methods are intended to be implemented or overidden:
 */
public class CalibrationFactory {

    public static Calibrator createCalibrator(Product sourceProduct)
                                            throws OperatorException, IllegalArgumentException {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        if(absRoot == null) {
            throw new OperatorException("AbstractMetadata is null");
        }
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);

        if(mission.equals("ENVISAT")) {
            return new ASARCalibrator();
        } else if(mission.contains("ERS1") || mission.contains("ERS2")) {
            return new ERSCalibrator();
        } else if(mission.equals("ALOS")) {
            return new ALOSCalibrator();
        } else if(mission.equals("RS2")) {
            return new Radarsat2Calibrator();
        } else if(mission.contains("TSX") || mission.contains("TDX")) {
            return new TerraSARXCalibrator();
        } else if(mission.contains("CSK")) {
        	return new CosmoSkymedCalibrator();
        } else {
            throw new OperatorException("Mission " + mission + " is currently not supported for calibration.");
        }
    }
}