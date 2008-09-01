package com.bc.ceres.glevel.support;

import com.bc.ceres.glevel.LRImageFactory;

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
public class MRImageSupport {
    private final RenderedImage frImage;
    private final LRImageFactory lrImageFactory;
    private final Map<Integer, RenderedImage> lrImages;

    public MRImageSupport(RenderedImage frImage, LRImageFactory lrImageFactory) {
        this.lrImageFactory = lrImageFactory;
        this.frImage = frImage;
        this.lrImages = new HashMap<Integer, RenderedImage>(16);
    }

    public RenderedImage getFRImage() {
        return frImage;
    }

    public LRImageFactory getLRImageFactory() {
        return lrImageFactory;
    }

    public RenderedImage getLRImage(int level) {
        if (level <= 0) {
            return frImage;
        }
        synchronized (lrImages) {
            RenderedImage lrImage = lrImages.get(level);
            if (lrImage == null) {
                lrImage = lrImageFactory.createLRImage(level);
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
