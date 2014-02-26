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
package org.esa.pfa.fe;

import java.awt.*;
import java.io.File;

/**
 * Describe the feature extraction application .
 *
 * @author Luis Veci
 * @author Norman Fomferra
 */
public interface PFAApplicationDescriptor {

    /**
     * The name
     *
     * @return a name
     */
    String getName();

    /**
     * Gets the width and height of the patch segmentation.
     *
     * @return the  dimension
     */
    Dimension getPatchDimension();

    /**
     * Gets the graph file with which to apply the feature extraction
     *
     * @return the graph file
     */
    File getGraphFile();

    /**
     * @return A Lucene query expression that matches all entries.
     */
    String getAllQueryExpr();

    /**
     * @return The name of the default quicklook file to be used. May be {@code null}, and if so, an arbitrary quicklook file will be used.
     */
    String getDefaultQuicklookFileName();

    /**
     * @return The name of the numeric features to be used by the classifier. May be {@code null} or an empty array, and if so, all numeric features will be used.
     */
    String[] getDefaultFeatureSet();
}
