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
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class AlgalBloomApplicationDescriptor extends AbstractApplicationDescriptor {

    private static final String NAME = "Algal Bloom Detection";
    public static final String DEFAULT_FEATURE_SET = "flh.mean,mci.mean,flh_hg_pixels";
    public static final String DEFAULT_QL_NAME = "rgb1_ql.png";
    public static final String DEFAULT_ALL_QUERY = "product:MER*";

    private static final URL graphURL = AlgalBloomApplicationDescriptor.class.getResource("AlgalBloomFeatureWriter.xml");
    private static Properties properties = new Properties(System.getProperties());

    private static Dimension patchDimension = new Dimension(200, 200);
    private static Set<String> defaultFeatureSet;
    private static File localProductDir;


    static {
        File file = new File(SystemUtils.getApplicationDataDir(), "pfa-algalblooms.properties");
        try {
            try (FileReader reader = new FileReader(file)) {
                properties.load(reader);
            }
        } catch (IOException e) {
            // ok
        }
    }

    public AlgalBloomApplicationDescriptor() {
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
    public File getGraphFile() {
        return new File(graphURL.getPath());
    }

    @Override
    public String getAllQueryExpr() {
        return properties.getProperty("pfa.algalblooms.allQuery", DEFAULT_ALL_QUERY);
    }

    @Override
    public String getDefaultQuicklookFileName() {
        return properties.getProperty("pfa.algalblooms.qlName", DEFAULT_QL_NAME);
    }

    @Override
    public Set<String> getDefaultFeatureSet() {
        if (defaultFeatureSet == null) {
            String property = properties.getProperty("pfa.algalblooms.featureSet", DEFAULT_FEATURE_SET);
            defaultFeatureSet = getStringSet(property);
        }
        return defaultFeatureSet;
    }

    @Override
    public File getLocalProductDir() {
        if (localProductDir == null) {
            String property = properties.getProperty("pfa.algalblooms.localProductDir");
            if (property != null) {
                localProductDir = new File(property.trim());
            }
        }
        return localProductDir;
    }

    private static Set<String> getStringSet(String csv) {
        String[] values = csv.split(",");
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
        }
        return new HashSet<>(Arrays.asList(values));
    }
}
