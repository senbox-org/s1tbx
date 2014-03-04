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
package org.esa.pfa.fe;

import org.esa.beam.util.SystemUtils;

import java.awt.Dimension;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class UrbanAreaApplicationDescriptor extends AbstractApplicationDescriptor {

    private static final String NAME = "Urban Area Detection";
    private static final String DEFAULT_FEATURE_SET =   "speckle_divergence.mean," +
                                                        "speckle_divergence.stdev," +
                                                        "speckle_divergence.cvar," +
                                                        "speckle_divergence.min," +
                                                        "speckle_divergence.maxspeckle_divergence.percentOverPnt4" +
                                                        "speckle_divergence.largestConnectedBlob";
    private static final String DEFAULT_QL_NAME = "sigma0_ql.png";
    private static final String DEFAULT_ALL_QUERY = "product:ENVI*";
    private static Dimension patchDimension = new Dimension(200, 200);
    private static Set<String> defaultFeatureSet;

    private static Properties properties = new Properties(System.getProperties());

    static {
        File file = new File(SystemUtils.getApplicationDataDir(), "pfa.urbanarea.properties");
        try {
            try (FileReader reader = new FileReader(file)) {
                properties.load(reader);
            }
        } catch (IOException e) {
            // ok
        }
    }

    public UrbanAreaApplicationDescriptor() {
        super(NAME);
    }

    /**
     * Gets the width and height of the patch segmentation.
     *
     * @return the  dimension
     */
    @Override
    public Dimension getPatchDimension() {
        return patchDimension;
    }

    @Override
    public InputStream getGraphFileAsStream() {
        return UrbanAreaApplicationDescriptor.class.getClassLoader().getResourceAsStream("graphs/UrbanDetectionFeatureWriter.xml");
    }

    @Override
    public String getAllQueryExpr() {
        return properties.getProperty("pfa.urbanarea.allQuery", DEFAULT_ALL_QUERY);
    }

    @Override
    public String getDefaultQuicklookFileName() {
        return properties.getProperty("pfa.urbanarea.qlName", DEFAULT_QL_NAME);
    }

    @Override
    public Set<String> getDefaultFeatureSet() {
        if (defaultFeatureSet == null) {
            String property = properties.getProperty("pfa.urbanarea.featureSet", DEFAULT_FEATURE_SET);
            defaultFeatureSet = getStringSet(property);
        }
        return defaultFeatureSet;
    }

    private static Set<String> getStringSet(String csv) {
        String[] values = csv.split(",");
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
        }
        return new HashSet<>(Arrays.asList(values));
    }

    @Override
    public File getLocalProductDir() {
        return null;
    }
}
