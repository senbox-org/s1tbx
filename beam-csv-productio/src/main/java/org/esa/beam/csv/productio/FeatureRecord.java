/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.csv.productio;

/**
 * Interface extending a record to feature-specific methods.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public interface FeatureRecord extends Record {

    /**
     * @return {@code true}, if feature record has an ID.
     */
    boolean hasFeatureId();

    /**
     * @return the feature ID
     */
    String getFeatureId();

}
