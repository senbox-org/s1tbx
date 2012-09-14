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
package org.esa.nest.gpf;

import org.esa.beam.framework.gpf.OperatorSpi;

/**
 * The operator performs the following operations for all master/slave pairs that user selected:
 *
 * 1. For both PCA images read in the min values computed in the previous step by PCAMinOp;
 * 2. Read also the eigendevector matrix saved in the temporary metadata;
 * 3. Compute the PCA images again and substract the min value from each;
 * 4. Output the final PCA images to target product.
 */

public class PCAOpSpi extends OperatorSpi {

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    //public static class Spi extends OperatorSpi {
        public PCAOpSpi() {
            super(PCAOp.class);
            super.setOperatorUI(PCAStatisticsOpUI.class);
        }
    //}
}