package org.esa.beam.gpf.common.reproject.ui;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class GridDefinitionFormModel {

    public static final String GRID_WIDTH_NAME = "gridWidth";
    public static final String GRID_HEIGHT_NAME = "gridHeight";
    public static final String PIXEL_SIZE_X_NAME = "pixelSizeX";
    public static final String PIXEL_SIZE_Y_NAME = "pixelSizeY";
    public static final String ADJUST_SIZE_TO_SOURCE_REGION_NAME = "adjustSizeToSourceRegion";


    private final Rectangle sourceDimension;
    private final CoordinateReferenceSystem sourceCrs;
    private final CoordinateReferenceSystem targetCrs;
    private int gridWidth;
    private int gridHeight;

    private double pixelSizeY;
    private double pixelSizeX;
    private String unit;
    private boolean adjustSizeToSourceRegion;

    GridDefinitionFormModel(Rectangle sourceDimension, CoordinateReferenceSystem sourceCrs,
                            CoordinateReferenceSystem targetCrs,
                            String unit) {
        this.sourceDimension = sourceDimension;
        this.sourceCrs = sourceCrs;
        this.targetCrs = targetCrs;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.pixelSizeX = pixelSizeX;
        this.pixelSizeY = pixelSizeY;
        this.unit = unit;
        adjustSizeToSourceRegion = true;
    }

    public Rectangle getSourceDimension() {
        return sourceDimension;
    }

    public CoordinateReferenceSystem getSourceCrs() {
        return sourceCrs;
    }

    public CoordinateReferenceSystem getTargetCrs() {
        return targetCrs;
    }

    public int getGridWidth() {
        return gridWidth;
    }

    public void setGridWidth(int gridWidth) {
        this.gridWidth = gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public void setGridHeight(int gridHeight) {
        this.gridHeight = gridHeight;
    }

    public double getPixelSizeY() {
        return pixelSizeY;
    }

    public void setPixelSizeY(double pixelSizeY) {
        this.pixelSizeY = pixelSizeY;
    }

    public double getPixelSizeX() {
        return pixelSizeX;
    }

    public void setPixelSizeX(double pixelSizeX) {
        this.pixelSizeX = pixelSizeX;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isAdjustSizeToSourceRegion() {
        return adjustSizeToSourceRegion;
    }

    public void setAdjustSizeToSourceRegion(boolean adjustSizeToSourceRegion) {
        this.adjustSizeToSourceRegion = adjustSizeToSourceRegion;
    }
}
