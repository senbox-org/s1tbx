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
