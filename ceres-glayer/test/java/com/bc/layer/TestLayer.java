package com.bc.layer;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class TestLayer extends AbstractGraphicalLayer {
    int paintCount;

    @Override
    protected void paintLayer(Graphics2D g, Viewport vp) {
        paintCount++;
    }
}
