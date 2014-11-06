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

package com.bc.ceres.glayer.tools;

import java.awt.geom.AffineTransform;
import java.io.File;

public class DisplayImagePyramid {
    public static void main(String[] args) {
        File location = new File(args[0]);
        String extension = args[1];
        int levelCount = Integer.parseInt(args[2]);
        boolean concurrent = Boolean.parseBoolean(args[3]);

        Tools.configureJAI();
        Tools.displayImage(location, extension, new AffineTransform(), levelCount);
    }

}