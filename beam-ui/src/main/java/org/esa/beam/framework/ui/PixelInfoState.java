package org.esa.beam.framework.ui;

import org.esa.beam.framework.ui.product.ProductSceneView;

class PixelInfoState {
    static final PixelInfoState INVALID = new PixelInfoState(null, -1, -1, -1, false);

    final ProductSceneView view;
    final int pixelX;
    final int pixelY;
    final int level;
    final boolean pixelPosValid;

    PixelInfoState(ProductSceneView view, int pixelX, int pixelY, int level, boolean pixelPosValid) {
        this.view = view;
        this.pixelX = pixelX;
        this.pixelY = pixelY;
        this.level = level;
        this.pixelPosValid = pixelPosValid;
    }

    boolean equals(ProductSceneView view, int pixelX, int pixelY, int level, boolean pixelPosValid) {
        return this.view == view
                && this.pixelX == pixelX
                && this.pixelY == pixelY
                && this.level == level
                && this.pixelPosValid == pixelPosValid;
    }
}
