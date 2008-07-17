package com.bc.ceres.glayer;

import com.bc.ceres.glayer.AbstractGraphicalLayer;
import com.bc.ceres.glayer.Viewport;

import java.awt.Graphics2D;

public class TestLayer extends AbstractGraphicalLayer {
    int paintCount;

    @Override
    protected void paintLayer(Graphics2D g, Viewport vp) {
        paintCount++;
    }
}
