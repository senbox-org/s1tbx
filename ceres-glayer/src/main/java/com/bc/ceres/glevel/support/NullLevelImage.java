package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LevelImage;

import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
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

    @Override
    public int getLevelCount() {
        return 1;
    }

    @Override
    public int computeLevel(double scale) {
        return 0;
    }

    @Override
    public RenderedImage getLRImage(int level) {
        return IMAGE;
    }

    @Override
    public void reset() {
    }

    @Override
    public AffineTransform getImageToModelTransform(int level) {
        return new AffineTransform();
    }

    @Override
    public AffineTransform getModelToImageTransform(int level) {
        return new AffineTransform();
     }

    @Override
    public Rectangle2D getModelBounds() {
        return new Rectangle();
    }
}
