/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.datamodel;

/**
 * The scaling method used for geophysical value transformation in a {@link Band}.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public interface Scaling {

    /**
     * The identity scaling in=out.
     */
    Scaling IDENTITY = new Scaling() {
        @Override
        public double scale(final double value) {
            return value;
        }

        @Override
        public double scaleInverse(final double value) {
            return value;
        }
    };

    /**
     * The forward scaling method.
     * @param value the value to be scaled
     * @return the transformed value
     */
    double scale(double value);

    /**
     * The inverse scaling method.
     * @param value the value to be inverse-scaled
     * @return the transformed value
     */
    double scaleInverse(double value);
}
