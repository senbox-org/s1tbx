package com.bc.jai.tools;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

public class DisplayImage {
    public static void main(String[] args) {
        String imageFilePath = args[0];
        int levelCount = Integer.parseInt(args[1]);
        boolean concurrent = Boolean.parseBoolean(args[2]);

        Utils.configureJAI();
        RenderedImage image = Utils.loadImage(imageFilePath);
        Utils.dumpImageInfo(image);
        Utils.displayImage(imageFilePath, image, new AffineTransform(), levelCount, concurrent);
    }

}
