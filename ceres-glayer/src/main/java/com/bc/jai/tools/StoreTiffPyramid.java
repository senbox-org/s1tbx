package com.bc.jai.tools;

import java.awt.image.RenderedImage;

public class StoreTiffPyramid {
    public static void main(String[] args) {
        final String sourceImageName = args[0];
        final String targetBaseName = args[1];
        final int maxLevel = Integer.valueOf(args[2]);

        Utils.configureJAI();
        RenderedImage sourceImage = Utils.loadImage(sourceImageName);
        sourceImage = Utils.createTiledImage(sourceImage, 512, 512);
        Utils.storeTiffPyramid(sourceImage, targetBaseName, maxLevel);
    }
}