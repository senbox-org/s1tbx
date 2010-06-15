package org.esa.beam.framework.datamodel;

/**
 * Default implementation of {@link org.esa.beam.framework.datamodel.TimeCoding}. It simply interpolates line-wise
 * between start and end time.
 */
public class DefaultTimeCoding extends TimeCoding {

    private int width;
    private int height;

    /**
     * Constructor for a DefaultTimeCoding.
     *
     * @param startTime the start time
     * @param endTime   the end time
     * @param width     the width
     * @param height    the height
     */
    public DefaultTimeCoding(ProductData.UTC startTime, ProductData.UTC endTime,
                             int width, int height) {
        super(startTime, endTime);
        this.height = height;
        this.width = width;
    }

    /**
     * Interpolates time line-wise
     *
     * @param pos the pixel position to retrieve time information for
     *
     * @return the interpolated time at the given pixel position
     *
     * @throws IllegalArgumentException if pixel is out of bounds
     */
    @Override
    public ProductData.UTC getTimeAtPixel(PixelPos pos) {
        final ProductData.UTC utcStartTime = getStartTime();
        final ProductData.UTC utcEndTime = getEndTime();

        final double dStart = utcStartTime.getMJD();
        final double dStop = utcEndTime.getMJD();
        if (dStart == 0 || dStop == 0 || !isInBounds(pos)) {
            throw new IllegalArgumentException("Cannot determine time for pixel " + pos.toString() + ".");
        } else {
            final double vPerLine = (dStop - dStart) / (height - 1);
            final double currentLine = vPerLine * pos.y + dStart;
            return new ProductData.UTC(currentLine);
        }
    }

    private boolean isInBounds(PixelPos pos) {
        return pos.x < width &&
               pos.y < height &&
               pos.x >= 0 &&
               pos.y >= 0;
    }


}

