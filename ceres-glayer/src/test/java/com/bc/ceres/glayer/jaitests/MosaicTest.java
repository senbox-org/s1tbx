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

package com.bc.ceres.glayer.jaitests;

import com.bc.ceres.glayer.tools.Tools;

import javax.media.jai.RenderedOp;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

import org.junit.Ignore;

@Ignore
public class MosaicTest {

    public static void main(String[] args) {
        Tools.configureJAI();
        RenderedOp image = Tools.loadImage(args[0]);
        image = Tools.createMosaic(new RenderedImage[]{
                Tools.transformImage(image, 0f, 0.0f, (float) Math.toRadians(0), 0.2f),
                Tools.transformImage(image, 300f, 300f, (float) Math.toRadians(10), 0.5f),
                Tools.transformImage(image, 600f, 6000f, (float) Math.toRadians(20), 1.0f),
                Tools.transformImage(image, 1000f, 1000f, (float) Math.toRadians(30), 1.5f),
        });
        Tools.displayImage("MosaicTest", image, new AffineTransform(), 0);
    }

}
