package com.bc.ceres.glayer.tools;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

public class DisplayImage {
    public static void main(String[] args) {
        String imageFilePath = args[0];
        int levelCount = Integer.parseInt(args[1]);
        boolean concurrent = Boolean.parseBoolean(args[2]);

        Tools.configureJAI();
        RenderedImage image = Tools.loadImage(imageFilePath);
//        Tools.dumpImageInfo(image);
        Tools.displayImage(imageFilePath, image, new AffineTransform(), levelCount, concurrent);
    }

}
