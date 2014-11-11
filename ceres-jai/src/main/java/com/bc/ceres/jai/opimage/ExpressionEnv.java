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

package com.bc.ceres.jai.opimage;

import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;

public class ExpressionEnv {
    Map<String, Number> constants;
    Map<String, RenderedImage> sourceImages;
    Map<String, Map<String, Integer>> masks;

    public ExpressionEnv() {
        constants = new HashMap<String, Number>();
        sourceImages = new HashMap<String, RenderedImage>();
        masks = new HashMap<String, Map<String, Integer>>();
    }

    public void addConstant(String name, Number number) {
        constants.put(name, number);
    }

    public Number getConstant(String name) {
        return constants.get(name);
    }

    public void addSourceImage(String name, RenderedImage image) {
        sourceImages.put(name, image);
    }

    public RenderedImage getSourceImage(String name) {
        return sourceImages.get(name);
    }

    public void addMask(String imageName, String maskName, int mask) {
        Map<String, Integer> map = masks.get(imageName);
        if (map == null) {
            map = new HashMap<String, Integer>();
            masks.put(imageName, map);
        }
        map.put(maskName, mask);
    }

    public int getMask(String imageName, String maskName) {
        Map<String, Integer> stringIntegerMap = masks.get(imageName);
        if (stringIntegerMap != null) {
            Integer integer = stringIntegerMap.get(maskName);
            if (integer != null) {
                return integer;
            }
        }
        return 0;
    }
}
