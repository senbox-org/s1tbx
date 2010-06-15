package org.esa.beam.framework.datamodel;

/**
 * Abstract class representing a time-coding. A time-coding is defined by a start and an end time and thus represents
 * a time span. It maps time information to pixel-positions.
 */
public abstract class TimeCoding {

    private ProductData.UTC startTime;

    private ProductData.UTC endTime;

    /**
     * Constructor creates a new TimeCoding-instance with a given start and end time.
     *
     * @param startTime the start time of the time span represented by the time-coding
     * @param endTime   the end time of the time span represented by the time-coding
     *
     * @throws IllegalArgumentException if startTime or endTime is <code>null</code> or the start time is after the end
     *                                  time
     */
    protected TimeCoding(ProductData.UTC startTime, ProductData.UTC endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("No start or end time 'null' allowed.");
        }
        if (startTime.getAsDate().after(endTime.getAsDate())) {
            throw new IllegalArgumentException("Start time after end time.");
        }

        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Allows to retrieve time information for a given pixel.
     *
     * @param pos the pixel position to retrieve time information for
     *
     * @return the time at the given pixel position
     */
    public abstract ProductData.UTC getTimeAtPixel(final PixelPos pos);

    /**
     * Getter for the end time
     *
     * @return the end time
     */
    public ProductData.UTC getEndTime() {
        return endTime;
    }

    /**
     * Getter for the start time
     *
     * @return the start time
     */
    public ProductData.UTC getStartTime() {
        return startTime;
    }

    /**
     * Setter for the end time
     *
     * @param endTime the end time to set
     *
     * @throws IllegalArgumentException if end time is <code>null</code> or before start time
     */
    public void setEndTime(ProductData.UTC endTime) {
        if (endTime == null) {
            throw new IllegalArgumentException("No end time 'null' allowed.");
        }
        if (endTime.getAsDate().before(startTime.getAsDate())) {
            throw new IllegalArgumentException("Start time after end time.");
        }
        this.endTime = endTime;
    }

    /**
     * Setter for the start time
     *
     * @param startTime the start time to set
     *
     * @throws IllegalArgumentException if start time is <code>null</code> or after end time
     */
    public void setStartTime(ProductData.UTC startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("No start time 'null' allowed.");
        }
        if (startTime.getAsDate().after(endTime.getAsDate())) {
            throw new IllegalArgumentException("Start time after end time.");
        }
        this.startTime = startTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TimeCoding that = (TimeCoding) o;

        return endTime.getAsDate().getTime() == that.endTime.getAsDate().getTime()
               && startTime.getAsDate().getTime() == that.startTime.getAsDate().getTime();

    }

    @Override
    public int hashCode() {
        int result = startTime.hashCode();
        result = 31 * result + endTime.hashCode();
        return result;
    }
}