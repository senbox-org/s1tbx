package com.bc.jai.tools;

import java.awt.image.RenderedImage;

public class StoreTiledTiff {
    public static void main(String[] args) {
        final String sourceImageName = args[0];
        final String targetImageName = args[1];

        Utils.configureJAI();
        RenderedImage sourceImage = Utils.loadImage(sourceImageName);
        sourceImage = Utils.createTiledImage(sourceImage, 512, 512);
        Utils.storeTiledTiff(sourceImage, targetImageName);
    }
}
