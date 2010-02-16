package com.bc.ceres.glayer.jaitests;

import com.bc.ceres.glayer.tools.Tools;

import javax.media.jai.RenderedOp;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

import org.junit.Ignore;

@Ignore
public class MosaicTest {

    public static void main(String[] args) {
        Tools.configureJAI();
        RenderedOp image = Tools.loadImage(args[0]);
        image = Tools.createMosaic(new RenderedImage[]{
                Tools.transformImage(image, 0f, 0.0f, (float) Math.toRadians(0), 0.2f),
                Tools.transformImage(image, 300f, 300f, (float) Math.toRadians(10), 0.5f),
                Tools.transformImage(image, 600f, 6000f, (float) Math.toRadians(20), 1.0f),
                Tools.transformImage(image, 1000f, 1000f, (float) Math.toRadians(30), 1.5f),
        });
        Tools.displayImage("MosaicTest", image, new AffineTransform(), 0, true);
    }

}
