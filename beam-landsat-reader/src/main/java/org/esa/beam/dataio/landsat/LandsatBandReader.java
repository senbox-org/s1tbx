package org.esa.beam.dataio.landsat;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * The abstract class <code>LandsatBandReader</code> is used as an interface for LandsatBandReader implementations
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public abstract class LandsatBandReader {

    private final String bandName;
    private final long startOffset;
    final float multiplier;
    final ImageInputStream stream;
    final int width;

    /**
     * Creates the object with given band name, file offset, conversion multiplier and file stream.
     *
     * @param width
     * @param bandName
     * @param offset
     * @param mult
     * @param stream
     */
    LandsatBandReader(final int width, final String bandName, final int offset, final float mult,
                      final ImageInputStream stream) {
        this.bandName = bandName;
        startOffset = offset;
        multiplier = mult;
        this.stream = stream;
        this.width = width;
    }

    LandsatBandReader(final int width, final String bandName, final int offset,
                      final ImageInputStream stream) {
        this(width, bandName, offset, (float) 1, stream);
    }

    LandsatBandReader(final int width, final String bandName, final ImageInputStream stream) {
        this(width, bandName, 0, (float) 1, stream);
    }

    /**
     * @return the name of the band
     */
    public final String getBandName() {
        return bandName;
    }

    /**
     * Reads the band data from file.
     *
     * @param sourceOffsetX
     * @param sourceOffsetY
     * @param sourceWidth
     * @param sourceHeight
     * @param sourceStepX
     * @param sourceStepY
     * @param destOffsetX
     * @param destOffsetY
     * @param destWidth
     * @param destHeight
     * @param destBuffer
     *
     * @throws IOException
     */
    abstract void readBandData(int sourceOffsetX, int sourceOffsetY,
                               int sourceWidth, int sourceHeight,
                               int sourceStepX, int sourceStepY,
                               int destOffsetX, int destOffsetY,
                               int destWidth, int destHeight,
                               ProductData destBuffer,
                               ProgressMonitor pm) throws IOException;


    /**
     * Sets the file stream to the start position of the reading process
     *
     * @param sourceOffsetX
     * @param sourceOffsetY
     * @param pixelSize
     *
     * @throws IOException
     */
    final void setStreamPos(final int sourceOffsetX, final int sourceOffsetY, final int pixelSize) throws
                                                                                                   IOException {
        // calculate offset in stream and wind to position
        long currentOffset = (sourceOffsetY * width + sourceOffsetX) * pixelSize;
        currentOffset += startOffset;
        stream.seek(currentOffset);
    }

    /**
     * Updates the stream position to the position: <code>_currentOffset += delta * pixelSize</code>
     *
     * @param delta
     * @param pixelSize
     *
     * @throws IOException
     */
    final void updateStreamPos(final int delta, final int pixelSize) throws
                                                                     IOException {
        long currentOffset = (int) stream.getStreamPosition();
        currentOffset += delta * pixelSize;
        stream.seek(currentOffset);
    }

    /**
     * @throws IOException
     */
    public final void close() throws
                              IOException {
        stream.close();
    }

}
