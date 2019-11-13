/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.csa.rstb.soilmoisture.gpf.support;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;

/**
 * Creates a dielectric model for soil moisture inversion.
 */
public class DielectricModelFactory {

    public static final String HALLIKAINEN = "Hallikainen";
    public static final String MIRONOV = "Mironov";

    public static DielectricModel createDielectricModel(final Operator op, final Product srcProduct,
                                                        final Product tgtProduct, final double invalidSMValue,
                                                        final double minSM, final double maxSM,
                                                        final Band smBand, final Band qualityIndexBand,
                                                        final String rdcBandName, final String modelName)
            throws OperatorException, IllegalArgumentException {

        switch (modelName) {

            case HALLIKAINEN:
                return new HallikainenDielectricModel(op, srcProduct, tgtProduct, invalidSMValue, minSM, maxSM, smBand, qualityIndexBand, rdcBandName);

            case MIRONOV:
                return new MironovDielectricModel(op, srcProduct, tgtProduct, invalidSMValue, minSM, maxSM, smBand, qualityIndexBand, rdcBandName);

            default:
                break;
        }

        return null;
    }
}


