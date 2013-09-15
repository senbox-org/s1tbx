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
package org.csa.rstb.gpf.classifiers;

import org.csa.rstb.gpf.PolarimetricClassificationOp;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.gpf.Tile;

/**
    Polarimetric Classifiers
 */
public interface PolClassifier {

    /**
        Return the band name for the target product
        @return band name
     */
    public String getTargetBandName();

    /**
     * returns the number of classes
     * @return num classes
     */
    public int getNumClasses();

    public IndexCoding createIndexCoding();

    public boolean canProcessStacks();

    /**
     * Perform decomposition for given tile.
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param op the polarimetric decomposition operator
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    public void computeTile(final Band targetBand, final Tile targetTile, final PolarimetricClassificationOp op);
}
