package com.bc.ceres.glayer;

import com.bc.ceres.glayer.AbstractGraphicalLayer;

public class TestLayer extends AbstractGraphicalLayer {
    int paintCount;

    @Override
    protected void renderLayer(Rendering rendering) {
        paintCount++;
    }
}
