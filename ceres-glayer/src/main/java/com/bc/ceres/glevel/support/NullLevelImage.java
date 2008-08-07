package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LevelImage;

import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.Rectangle;

/**
 * A level image which can be used instead of {@code null).
*
* @author Ralf Quast
* @version $revision$ $date$
*/
public class NullLevelImage implements LevelImage {

    public static final NullLevelImage INSTANCE = new NullLevelImage();

    private final static PlanarImage IMAGE = PlanarImage.wrapRenderedImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

    private NullLevelImage() {
    }

    public int getLevelCount() {
        return 1;
    }

    public int computeLevel(double scale) {
        return 0;
    }

    public PlanarImage getPlanarImage(int level) {
        return IMAGE;
    }

    public void reset() {
    }

    public AffineTransform getImageToModelTransform(int level) {
        return new AffineTransform();
    }

    public AffineTransform getModelToImageTransform(int level) {
        return new AffineTransform();
     }

    public Rectangle2D getBounds(int level) {
        return new Rectangle();
    }
}
