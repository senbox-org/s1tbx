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