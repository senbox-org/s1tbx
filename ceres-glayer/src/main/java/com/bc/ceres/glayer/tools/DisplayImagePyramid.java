package com.bc.ceres.glayer.tools;

import java.awt.geom.AffineTransform;
import java.io.File;

public class DisplayImagePyramid {
    public static void main(String[] args) {
        File location = new File(args[0]);
        String extension = args[1];
        int levelCount = Integer.parseInt(args[2]);
        boolean concurrent = Boolean.parseBoolean(args[3]);

        Tools.configureJAI();
        Tools.displayImage(location, extension, new AffineTransform(), levelCount, concurrent);
    }

}