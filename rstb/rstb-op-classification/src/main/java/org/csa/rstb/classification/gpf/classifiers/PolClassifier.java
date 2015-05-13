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
package org.csa.rstb.classification.gpf.classifiers;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.IndexCoding;
import org.esa.snap.framework.gpf.Tile;

/**
 * Polarimetric Classifiers
 */
public interface PolClassifier {

    /**
     * Return the band name for the target product
     *
     * @return band name
     */
    String getTargetBandName();

    /**
     * returns the number of classes
     *
     * @return num classes
     */
    int getNumClasses();

    IndexCoding createIndexCoding();

    boolean canProcessStacks();

    /**
     * Perform decomposition for given tile.
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs during computation of the filtered value.
     */
    void computeTile(final Band targetBand, final Tile targetTile);
}
