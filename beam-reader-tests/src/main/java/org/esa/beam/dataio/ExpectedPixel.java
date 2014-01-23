package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.framework.datamodel.Product;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;

class ExpectedPixel {
    @JsonProperty(required = true)
    private int x;
    @JsonProperty(required = true)
    private int y;
    @JsonProperty(required = true)
    private float value;

    ExpectedPixel() {
    }

    ExpectedPixel(int x, int y, float value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    float getValue() {
        return value;
    }

    static ArrayList<Point2D> createPointList(Product product, Random random) {
        ArrayList<Point2D> pointList = new ArrayList<Point2D>();
        if (product.getPinGroup().getNodeCount() > 0) {
            final PlacemarkGroup pinGroup = product.getPinGroup();
            for (int i = 0; i < pinGroup.getNodeCount(); i++) {
                pointList.add(pinGroup.get(i).getPixelPos());
            }
        } else {
            for (int i = 0; i < 2; i++) {
                final int x = (int) (random.nextFloat() * product.getSceneRasterWidth());
                final int y = (int) (random.nextFloat() * product.getSceneRasterHeight());
                pointList.add(new Point(x, y));
            }
        }
        return pointList;
    }
}
