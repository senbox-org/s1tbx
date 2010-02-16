package org.esa.beam.util;

/**
 * A pixel mask provides a boolean value for a given pixel position.
 * It is used to identify valid pixels in a raster.
 * @since 4.1
 * @author Norman
 */
public final class BitRaster {

    private final int width;
    private final int height;
    private final long[] words;

    public BitRaster(int width, int height) {
       this.width = width;
       this.height = height;
       int size = width * height;
       this.words = new long[(size >> 6) + 1];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void set(int x, int y, boolean value) {
        set(y * width + x, value);
    }

    public void set(int pixelIndex, boolean value) {
        if (value) {
            set(pixelIndex);
        } else {
            clear(pixelIndex);
        }
    }

    public boolean isSet(int x, int y) {
       return isSet(y * width + x);
    }

    public boolean isSet(int pixelIndex) {
        final int wordIndex = pixelIndex >> 6;
        return (words[wordIndex] & (1L << pixelIndex)) != 0L;
    }

    public void set(int pixelIndex) {
        final int wordIndex = pixelIndex >> 6;
        words[wordIndex] |= (1L << pixelIndex);
    }

    public void clear(int pixelIndex) {
        final int wordIndex = pixelIndex >> 6;
        words[wordIndex] &= ~(1L << pixelIndex);
    }

    /**
     * Creates a byte-packed bitmask as array of bytes.
     * <p>
     * This method is used to provide backward API compatibility with BEAM versions prior 4.1.
     * Its use is discouraged.</p>
     *
     * @return an array of bytes of size {@link #getBytePackedBitmaskRasterWidth()}<code> * </code>{@link #getHeight()}
     * @see #getBytePackedBitmaskRasterWidth()
     */
    public byte[] createBytePackedBitmaskRasterData() {
        int packedWidth = getBytePackedBitmaskRasterWidth();
        byte[] bytes = new byte[packedWidth * getHeight()];
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                bytes[(y * packedWidth + x)] |= (1 << (x % 8));
            }
        }
        return bytes;
    }

    /**
     * Gets the width of this raster in byte-packed form
     * <p>
     * This method is used to provide backward API compatibility with BEAM versions prior 4.1.
     * Its use is discouraged.</p>
     *
     * @return the width of this raster in byte-packed form
     * @see #createBytePackedBitmaskRasterData()
     */
    public final int getBytePackedBitmaskRasterWidth() {
        int bytePackedBitmaskRasterWidth = getWidth() / 8;
        if (getWidth() % 8 != 0) {
            bytePackedBitmaskRasterWidth++;
        }
        return bytePackedBitmaskRasterWidth;
    }
}
