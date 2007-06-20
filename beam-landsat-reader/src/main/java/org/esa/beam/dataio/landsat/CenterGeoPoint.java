package org.esa.beam.dataio.landsat;

import org.esa.beam.dataio.landsat.LandsatConstants.Points;

import java.io.IOException;


/**
 * The class <code>CenterGeoPoint</code> is used to store the geo and offset data of the
 * center point
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public class CenterGeoPoint extends GeoPoint {

    private int centerY;
    private int centerX;

    /**
     * creates the center Geopoint (todo with factory... center point exists only one time in a landsat product)
     *
     * @param point
     * @param input
     */

    public CenterGeoPoint(Points point, LandsatImageInputStream input) {
        super(point);
    }

    /**
     * y Value of the center pixel
     *
     * @return the y value of the center pixel
     */
    public int getCenterY() {
        return centerY;
    }

    /**
     * gets the y center point from the file
     *
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws NumberFormatException
     * @throws IOException
     */
    public void setCenterY(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                  NumberFormatException,
                                                                                                  IOException {
        centerY = Integer.parseInt(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }

    /**
     * @return the x value of the center pixel
     */
    public int getCenterX() {
        return centerX;
    }

    /**
     * gets the x central point from the file
     *
     * @param offset
     * @param size
     * @param inputStream
     *
     * @throws NumberFormatException
     * @throws IOException
     */
    public void setCenterX(final int offset, final int size, LandsatImageInputStream inputStream) throws
                                                                                                  NumberFormatException,
                                                                                                  IOException {
        centerX = Integer.parseInt(LandsatUtils.getValueFromLandsatFile(inputStream, offset, size).trim());
    }
}
