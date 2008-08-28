package org.esa.beam.jai;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.DownscalableImage;

import java.util.HashMap;
import java.util.Map;


public abstract class DownscalableImageSupport {

    private final DownscalableImage image;

    protected DownscalableImageSupport(DownscalableImage image) {
        Assert.notNull(image, "image");
        this.image = image;
    }


    public final DownscalableImage getImage() {
        return image;
    }

    public abstract DownscalableImageSupport getLevel0();

    public abstract int getSourceWidth();

    public abstract int getSourceHeight();

    public abstract int getLevel();

    public abstract double getScale();

    public int getSourceX(int tx) {
        return getSourceCoord(tx, 0, getSourceWidth() - 1);
    }

    public int getSourceY(int ty) {
        return getSourceCoord(ty, 0, getSourceHeight() - 1);
    }

    // TODO - wrong impl, replace by getSourceRect(destRect)
    public int getSourceWidth(int destWidth) {
        return getSourceCoord(destWidth, 1, getSourceWidth() - 1);
    }

    public int getSourceCoord(double destCoord, int min, int max) {
        return double2int(destCoord / getScale(), min, max);
    }

    public int getDestCoord(double sourceCoord, int min, int max) {
        return double2int(getScale() * sourceCoord, min, max);
    }

    public DownscalableImage getDownscaledImage(int level) {
        if (level == getLevel()) {
            return getImage();
        } else {
            return getDownscaledImageImpl(level);
        }
    }

    protected abstract DownscalableImage getDownscaledImageImpl(int level);

    public abstract void dispose();

    private static int double2int(double v, int min, int max) {
        int sc = (int) Math.floor(v);
        if (sc < min) {
            sc = min;
        }
        if (sc > max) {
            sc = max;
        }
        return sc;
    }

    public final static class Level0 extends DownscalableImageSupport {
        private final DownscalableImageFactory levelNImageFactory;
        private final Map<Integer, DownscalableImage> levelNImages;
        private final int sourceWidth;
        private final int sourceHeight;

        public Level0(DownscalableImage level0Image, DownscalableImageFactory levelNImageFactory) {
            super(level0Image);
            Assert.notNull(levelNImageFactory, "levelNImageFactory");
            this.levelNImageFactory = levelNImageFactory;
            this.levelNImages = new HashMap<Integer, DownscalableImage>(16);
            this.sourceWidth = level0Image.getWidth();
            this.sourceHeight = level0Image.getHeight();
        }

        @Override
        public DownscalableImageSupport getLevel0() {
            return this;
        }

        @Override
        public int getSourceWidth() {
            return sourceWidth;
        }

        @Override
        public int getSourceHeight() {
            return sourceHeight;
        }

        @Override
        public int getLevel() {
            return 0;
        }

        @Override
        public double getScale() {
            return 1;
        }

        @Override
        protected DownscalableImage getDownscaledImageImpl(int level) {
            DownscalableImage downscalableImage = levelNImages.get(level);
            if (downscalableImage == null) {
                downscalableImage = levelNImageFactory.createDownscalableImage(level);
                levelNImages.put(level, downscalableImage);
            }
            return downscalableImage;
        }

        @Override
        public synchronized void dispose() {
            if (levelNImages != null) {
                levelNImages.clear();
            }
        }
    }

    public final static class LevelN extends DownscalableImageSupport {
        private final DownscalableImageSupport level0;
        private final int level;
        private final double scale;

        public LevelN(DownscalableImageSupport level0, DownscalableImage levelNImage, int level) {
            super(levelNImage);
            Assert.argument(level0.getLevel() == 0, "level0.getLevel() == 0");
            Assert.argument(level > 0, "level > 0");
            this.level0 = level0;
            this.level = level;
            this.scale = ImageManager.computeScale(level);
        }

        @Override
        public DownscalableImageSupport getLevel0() {
            return level0;
        }

        @Override
        public int getSourceWidth() {
            return level0.getSourceWidth();
        }

        @Override
        public int getSourceHeight() {
            return level0.getSourceHeight();
        }

        @Override
        public int getLevel() {
            return level;
        }

        @Override
        public double getScale() {
            return scale;
        }

        @Override
        protected DownscalableImage getDownscaledImageImpl(int level) {
            return level0.getDownscaledImage(level);
        }

        @Override
        public synchronized void dispose() {
        }

    }

}