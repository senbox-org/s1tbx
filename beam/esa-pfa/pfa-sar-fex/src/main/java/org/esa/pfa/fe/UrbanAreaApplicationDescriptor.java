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

import java.awt.*;
import java.io.File;
import java.net.URL;

public class UrbanAreaApplicationDescriptor extends AbstractApplicationDescriptor {

    private static final String NAME = "Urban Area Detection";
    private static Dimension patchDimension = new Dimension(200, 200);

    private static final URL graphURL = UrbanAreaApplicationDescriptor.class.getClassLoader().getResource("graphs/UrbanDetectionFeatureWriter.xml");

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
    public File getGraphFile() {
        return new File(graphURL.getPath());
    }

    @Override
    public String getAllQueryExpr() {
        return "product:ENVI*";
    }
}
