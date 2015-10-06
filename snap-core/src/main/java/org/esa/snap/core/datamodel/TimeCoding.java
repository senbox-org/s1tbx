package org.esa.snap.core.datamodel;

/**
 * The {@code TimeCoding} interface allows to convert pixel coordinates to time values and vice versa (if possible).
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @since SNAP 2.0
 */
public interface TimeCoding {
    /**
     * Gets the time for a given pixel position as Modified Julian Day 2000 (MJD2000).
     *
     * @param pixelPos The pixel position in units of a given raster data node
     * @return The time as Modified Julian Day 2000 (MJD2000).
     * @see ProductData.UTC#getMJD()
     * @see #getPixelPos(double, PixelPos)
     */
    double getMJD(PixelPos pixelPos);

    /**
     * Determines whether this {@code TimeCoding} is capable of converting time values into pixel positions.
     * @return {@code true}, if so.
     * @see #getPixelPos(double, PixelPos)
     */
    default boolean canGetPixelPos() {
        return false;
    }

    /**
     * Gets a pixel position associated with a time value given as Modified Julian Day 2000 (MJD2000).
     *
     * @param mjd The time as Modified Julian Day 2000 (MJD2000).
     * @param pixelPos The pixel position to be modified and returned. If {@code null} a new instance will be created and returned.
     * @return The pixel position, or {@code null} if this {@code TimeCoding} cannot compute pixel positions from time values.
     * @see #canGetPixelPos()
     * @see #getMJD(PixelPos)
     */
    default PixelPos getPixelPos(double mjd, PixelPos pixelPos) {
        return null;
    }
}
