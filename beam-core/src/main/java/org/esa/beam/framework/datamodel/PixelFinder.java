package org.esa.beam.framework.datamodel;

/**
 * Represents a strategy for finding a pixel position for a given geographic position.
 * <p>
 * This class does not belong to the public API.
 *
 * @author Ralf Quast
 */
public interface PixelFinder {

    /**
     * Returns the pixel position for a given geographic position.
     *
     * @param geoPos   the geographic position.
     * @param pixelPos the pixel position.
     */
    void findPixelPos(GeoPos geoPos, PixelPos pixelPos);
}
