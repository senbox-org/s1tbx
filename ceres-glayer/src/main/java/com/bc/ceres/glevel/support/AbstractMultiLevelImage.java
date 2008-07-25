package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LevelImage;

import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileCache;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

public abstract class AbstractMultiLevelImage implements LevelImage {

    private final int levelCount;
    private final PlanarImage[] imageCache;
    private final AffineTransform[] imageToModelTransforms;
    private final AffineTransform[] modelToImageTransforms;

    public AbstractMultiLevelImage(AffineTransform imageToModelTransform, int levelCount) {
        final AffineTransform modelToImageTransform;
        try {
            modelToImageTransform = imageToModelTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("imageToModelTransform", e);
        }
        this.levelCount = levelCount;
        this.imageCache = new PlanarImage[levelCount];
        this.imageToModelTransforms = new AffineTransform[levelCount];
        this.modelToImageTransforms = new AffineTransform[levelCount];
        this.imageToModelTransforms[0] = new AffineTransform(imageToModelTransform);
        this.modelToImageTransforms[0] = new AffineTransform(modelToImageTransform);
    }

    public int getLevelCount() {
        return levelCount;
    }

    public int computeLevel(double scale) {
        int level = (int) Math.round(log2(scale));
        if (level < 0) {
            level = 0;
        } else if (level >= levelCount) {
            level = levelCount - 1;
        }
        return level;
    }

    public void regenerateLevels(boolean removeCachedTiles) {
        final PlanarImage[] planarImages;
        synchronized (imageCache) {
            planarImages = imageCache.clone();
            for (int i = 0; i < imageCache.length; i++) {
                imageCache[i] = null;
            }
        }
        for (PlanarImage planarImage : planarImages) {
            TileCache cache = JAI.getDefaultInstance().getTileCache();
            if (planarImage instanceof OpImage) {
                OpImage opImage = (OpImage) planarImage;
                if (opImage.getTileCache() != null) {
                    cache = opImage.getTileCache();
                }
            }
            cache.removeTiles(planarImage);
        }
    }

    public final PlanarImage getPlanarImage(int level) {
        checkLevel(level);
        synchronized (imageCache) {
            PlanarImage image = imageCache[level];
            if (image == null) {
                image = createPlanarImage(level);
                imageCache[level] = image;
            }
            return image;
        }
    }

    protected abstract PlanarImage createPlanarImage(int level);

    public final AffineTransform getImageToModelTransform(int level) {
        checkLevel(level);
        AffineTransform transform = imageToModelTransforms[level];
        if (transform == null) {
            transform = new AffineTransform(imageToModelTransforms[0]);
            final double s = pow2(level);
            transform.scale(s, s);
            imageToModelTransforms[level] = transform;
        }
        return new AffineTransform(transform);
    }

    public final AffineTransform getModelToImageTransform(int level) {
        checkLevel(level);
        AffineTransform transform = modelToImageTransforms[level];
        if (transform == null) {
            try {
                transform = getImageToModelTransform(level).createInverse();
                modelToImageTransforms[level] = transform;
            } catch (NoninvertibleTransformException e) {
                throw new IllegalStateException(e);
            }
        }
        return new AffineTransform(transform);
    }

    protected static double pow2(double v) {
        return Math.pow(2.0, v);
    }

    protected static double log2(double v) {
        return Math.log(v) / Math.log(2.0);
    }

    protected void checkLevel(int level) {
        if (level < 0 || level >= levelCount) {
            throw new IllegalArgumentException("level");
        }
    }
}