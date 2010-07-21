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

import java.awt.image.RenderedImage;

public class StoreTiffPyramid {
    public static void main(String[] args) {
        final String sourceImageName = args[0];
        final String targetBaseName = args[1];
        final int maxLevel = Integer.valueOf(args[2]);

        Tools.configureJAI();
        RenderedImage sourceImage = Tools.loadImage(sourceImageName);
        sourceImage = Tools.createTiledImage(sourceImage, 512, 512);
        Tools.storeTiffPyramid(sourceImage, targetBaseName, maxLevel);
    }
}