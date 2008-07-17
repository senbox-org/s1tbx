package com.bc.jai;

import com.bc.jai.tools.Utils;

import javax.media.jai.RenderedOp;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

public class MosaicTest {

    public static void main(String[] args) {
        Utils.configureJAI();
        RenderedOp image = Utils.loadImage(args[0]);
        image = Utils.createMosaic(new RenderedImage[]{
                Utils.transformImage(image, 0f, 0.0f, (float) Math.toRadians(0), 0.2f),
                Utils.transformImage(image, 300f, 300f, (float) Math.toRadians(10), 0.5f),
                Utils.transformImage(image, 600f, 6000f, (float) Math.toRadians(20), 1.0f),
                Utils.transformImage(image, 1000f, 1000f, (float) Math.toRadians(30), 1.5f),
        });
        Utils.displayImage("MosaicTest", image, new AffineTransform(), 0, true);
    }

}
