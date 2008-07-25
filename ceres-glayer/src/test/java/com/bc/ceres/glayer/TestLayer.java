package com.bc.ceres.glayer;

import com.bc.ceres.grender.Rendering;

import java.awt.geom.Rectangle2D;

public class TestLayer extends Layer {
    int renderCount;

    public Rectangle2D getBounds() {
        return null;
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        renderCount++;
    }
}
