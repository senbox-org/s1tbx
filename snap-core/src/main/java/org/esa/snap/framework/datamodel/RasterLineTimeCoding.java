package org.esa.snap.framework.datamodel;

import java.io.IOException;

public class RasterLineTimeCoding implements org.esa.snap.framework.datamodel.TimeCoding {

    private final double[] mjDs;

    public RasterLineTimeCoding(double[] mjDs) throws IOException {
        this.mjDs = mjDs;
    }


    @Override
    public double getMJD(PixelPos pixelPos) {
        int index = (int) pixelPos.y;
        if (index >= mjDs.length) {
            return Double.NaN;
        }
        return mjDs[index];
    }
}
