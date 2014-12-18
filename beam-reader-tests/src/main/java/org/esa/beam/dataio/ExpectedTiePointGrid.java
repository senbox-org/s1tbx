package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.StringUtils;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;

class ExpectedTiePointGrid {

    @JsonProperty(required = true)
    private String name;
    @JsonProperty
    private String description;
    @JsonProperty
    private String offsetX;
    @JsonProperty
    private String offsetY;
    @JsonProperty
    private String subSamplingX;
    @JsonProperty
    private String subSamplingY;
    @JsonProperty
    private ExpectedPixel[] expectedPixels;

    ExpectedTiePointGrid() {
        expectedPixels = new ExpectedPixel[0];
    }

    ExpectedTiePointGrid(TiePointGrid tiePointGrid, Random random) {
        this();
        this.name = tiePointGrid.getName();
        this.description = tiePointGrid.getDescription();
        this.offsetX = String.valueOf(tiePointGrid.getOffsetX());
        this.offsetY = String.valueOf(tiePointGrid.getOffsetY());
        this.subSamplingX = String.valueOf(tiePointGrid.getSubSamplingX());
        this.subSamplingY = String.valueOf(tiePointGrid.getSubSamplingY());
        expectedPixels = createExpectedPixels(tiePointGrid, random);
    }

    String getName() {
        return name;
    }

    public boolean isDescriptionSet() {
        return StringUtils.isNotNullAndNotEmpty(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    String getDescription() {
        return description;
    }

    public boolean isOffsetXSet() {
        return StringUtils.isNotNullAndNotEmpty(offsetX);
    }

    public void setOffsetX(String offsetX) {
        this.offsetX = offsetX;
    }

    double getOffsetX() {
        return Double.parseDouble(offsetX);
    }

    public boolean isOffsetYSet() {
        return StringUtils.isNotNullAndNotEmpty(offsetY);
    }

    public void setOffsetY(String offsetY) {
        this.offsetY = offsetY;
    }

    double getOffsetY() {
        return Double.parseDouble(offsetY);
    }

    public boolean isSubSamplingXSet() {
        return StringUtils.isNotNullAndNotEmpty(subSamplingX);
    }

    public void setSubSamplingX(String subSamplingX) {
        this.subSamplingX = subSamplingX;
    }

    double getSubSamplingX() {
        return Double.parseDouble(subSamplingX);
    }

    public boolean isSubSamplingYSet() {
        return StringUtils.isNotNullAndNotEmpty(subSamplingY);
    }

    public void setSubSamplingY(String subSamplingY) {
        this.subSamplingY = subSamplingY;
    }

    double getSubSamplingY() {
        return Double.parseDouble(subSamplingY);
    }

    ExpectedPixel[] getExpectedPixels() {
        return expectedPixels;
    }

    private ExpectedPixel[] createExpectedPixels(TiePointGrid tiePointGrid, Random random) {
        final ArrayList<Point2D> pointList = ExpectedPixel.createPointList(tiePointGrid.getProduct(), random);
        final ExpectedPixel[] expectedPixels = new ExpectedPixel[pointList.size()];
        for (int i = 0; i < expectedPixels.length; i++) {
            final Point2D point = pointList.get(i);
            final int x = (int) point.getX();
            final int y = (int) point.getY();
            final float value = tiePointGrid.isPixelValid(x, y) ? tiePointGrid.getSampleFloat(x, y) : Float.NaN;
            expectedPixels[i] = new ExpectedPixel(x, y, value);
        }
        return expectedPixels;
    }
}
