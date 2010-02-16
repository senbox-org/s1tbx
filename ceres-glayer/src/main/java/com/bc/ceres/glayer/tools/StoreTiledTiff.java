package com.bc.ceres.glayer.tools;

import java.awt.image.RenderedImage;

public class StoreTiledTiff {
    public static void main(String[] args) {
        final String sourceImageName = args[0];
        final String targetImageName = args[1];

        Tools.configureJAI();
        RenderedImage sourceImage = Tools.loadImage(sourceImageName);
        sourceImage = Tools.createTiledImage(sourceImage, 512, 512);
        Tools.storeTiledTiff(sourceImage, targetImageName);
    }
}
