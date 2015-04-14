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

package org.esa.snap.csv.dataio;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;
import java.util.Map;

/**
 * Interface providing access methods on a csv product.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public interface CsvSource {

    /**
     * @return The number of data records.
     * @throws java.io.IOException if sth. goes wrong
     */
    int getRecordCount() throws IOException;

    /**
     * @return A header representation on the csv file.
     */
    SimpleFeatureType getFeatureType();

    /**
     * @return An unmodifiable map of properties.
     */
    Map<String, String> getProperties();

    /**
     * @return all SimpleFeatures currently parsed
     */
    SimpleFeature[] getSimpleFeatures();
}
