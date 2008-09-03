package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LevelImageFactory;

import javax.media.jai.PlanarImage;
import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class MultiLevelImageSupport {
    private final RenderedImage frImage;
    private final LevelImageFactory levelImageFactory;
    private final Map<Integer, RenderedImage> lrImages;

    public MultiLevelImageSupport(RenderedImage frImage, LevelImageFactory levelImageFactory) {
        this.levelImageFactory = levelImageFactory;
        this.frImage = frImage;
        this.lrImages = new HashMap<Integer, RenderedImage>(16);
    }

    public RenderedImage getFRImage() {
        return frImage;
    }

    public LevelImageFactory getLevelImageFactory() {
        return levelImageFactory;
    }

    public RenderedImage getLevelImage(int level) {
        if (level <= 0) {
            return frImage;
        }
        synchronized (lrImages) {
            RenderedImage lrImage = lrImages.get(level);
            if (lrImage == null) {
                lrImage = levelImageFactory.createLevelImage(level);
                lrImages.put(level, lrImage);
            }
            return lrImage;
        }
    }

    public synchronized void dispose() {
        for (RenderedImage lrImage : lrImages.values()) {
            if (lrImage instanceof PlanarImage) {
                PlanarImage planarImage = (PlanarImage) lrImage;
                planarImage.dispose();
            }
        }
        lrImages.clear();
        if (frImage instanceof PlanarImage) {
            PlanarImage planarImage = (PlanarImage) frImage;
            planarImage.dispose();
        }
    }
}
