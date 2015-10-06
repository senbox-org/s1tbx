package org.esa.snap.binning.reader;

import org.esa.snap.binning.support.SEAGrid;
import org.esa.snap.core.datamodel.Band;
import ucar.ma2.Array;

import java.io.IOException;

abstract class AbstractGridAccessor {

    protected SEAGrid planetaryGrid;
    protected double pixelSizeX;

    abstract void dispose();

    abstract Array getLineValues(Band destBand, VariableReader variableReader, int lineIndex) throws IOException;

    abstract int getBinIndexInGrid(int binIndex, int lineIndex);

    abstract int getStartBinIndex(int sourceOffsetX, int lineIndex);

    abstract int getEndBinIndex(int sourceOffsetX, int sourceWidth, int lineIndex);

    void setPlanetaryGrid(SEAGrid planetaryGrid) {
        this.planetaryGrid = planetaryGrid;
    }

    void setPixelSizeX(double pixelSizeX) {
        this.pixelSizeX = pixelSizeX;
    }
}
