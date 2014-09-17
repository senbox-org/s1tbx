/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.gpf;

import java.awt.image.BufferedImage;
import org.esa.beam.framework.datamodel.ProductData;
/*
 * This source code has been placed into the public domain.
 */

/*
 * Note: This class only supports the non-standard construction of Haar wavelet
 * basis functions. There is currently no provision for standard basis functions.
 * For a good explanation see http://grail.cs.washington.edu/pub/stoll/wavelet1.pdf
 */
/**
 * <p>
 * Instances of this class provide methods for applying a Haar wavelet filter to
 * two dimensional values stored in a byte array. It is primarily intended to be
 * used for image processing. It also provides methods for reversing the filter
 * and converting filters and data into images.</p>
 *
 * <p>
 * Applying a filter to a byte array containing values in the range [0-255]
 * results in a same length integer array that contains the filter values; as
 * per the parameters set on the HaarFilter instance. A fractional integer
 * encoding is used for reasons of performance; it also ensures that filters
 * constructed with a sufficient number of fractional bits can be inverted
 * perfectly. As a consequence of the current implementation of this encoding by
 * this class, there is an upper limit to the size of value array that can be
 * filtered - presently 4096x4096.</p>
 *
 * <p>
 * <strong>Filtering can only be performed on data arrays that have a size which
 * is a square of a power of two. At present, the class only supports
 * non-standard basis functions. NOTE: This implementation has not been heavily
 * tested and may contain any number of bugs or inadequacies, see NOTES in the
 * source for further information.</strong></p>
 *
 * @author emmab
 */
public class HaarWaveletTransform {

    /**
     * The length of the side of the data to be filtered
     */
    private final int size;

    /**
     * The square of the size.
     */
    private final int sizeSqr;

    /**
     * A buffer that is allocated for performing filtering.
     */
    private int[] buffer = null;

    /**
     * The number of filtering iterations performed.
     */
    private int iterations;

    /**
     * The number of bits in the filter arrays processed.
     */
    private int fractionalBits;

    /**
     * Constructs a new object for performing filtering. Individual filtering
     * instances are constructed to operate on data of a predetermined size. All
     * arrays passed to a filter must have a length which corresponds to the
     * size specified at construction (specifically size x size).
     *
     * @param size the length of the side of all squares of data/filter values
     */
    public HaarWaveletTransform(int size) {
        this(size, -1, -1);
    }

    /**
     * Constructs a new object for performing filtering. Individual filtering
     * instances are constructed to operate on data of a predetermined size. All
     * arrays passed to a filter must have a length which corresponds to the
     * size specified at construction (specifically size x size). See
     *
     * @see #setIterations(int) for information about the iterations parameter
     * @see #setFractionalBits(int) for information about the fractionalBits
     * parameter
     *
     * @param size the length of the side of all squares of data/filter values
     * @param iterations the number of iterations performed during filtering, or
     * -1 to use the default value
     * @param fractionalBits the number of binary fractional bits in the number
     * representation used in filters, or -1 to use the default value
     */
    public HaarWaveletTransform(int size, int iterations, int fractionalBits) {

        if (size <= 0) {
            throw new IllegalArgumentException("size not strictly positive");
        }
//        if (size != Integer.highestOneBit(size)) {
//            throw new IllegalArgumentException("size not a power of two"+String.valueOf(size));
//        }
        if (size > 4096) {
            throw new IllegalArgumentException("size too large to support int size arithmetic");
        }

        this.size = size;
        this.sizeSqr = size * size;

        setIterations(iterations);
        setFractionalBits(fractionalBits);
    }

    /**
     * The number of iterations that will be applied during a call to the
     * <code>filter</code> method.
     *
     * @see #setIterations(int) for more information
     *
     * @return the number of iterations performed during filtering, never
     * negative
     */
    public int getIterations() {
        return iterations;
    }

    /**
     * The number of fractional bits that will be used in the filtered values
     * returned by the <code>filter</code> method.
     *
     * @see #setFractionalBits(int) for more information
     *
     * @return the number of binary fractional bits in the number representation
     * used in filters, never negative
     */
    public int getFractionalBits() {
        return fractionalBits;
    }

    /**
     * Specifies the number of times that the Haar wavelet is successively
     * applied during a call to the <code>filter</code> method. This cannot
     * exceed ln2(size). This maximum forms the sensible default value and can
     * be specified by passing a negative number to this method.
     *
     * @param iterations the number of iterations performed during filtering, or
     * -1 for the default value
     */
    public void setIterations(int iterations) {
        if (iterations < 0) {
            iterations = Integer.numberOfTrailingZeros(size);
        }
        if (1 << iterations > size) {
            throw new IllegalArgumentException("too many iterations for size");
        }
        this.iterations = iterations;
    }

    /**
     * Specifies the number of bits in the integers returned by the
     * <code>filter</code> method that represent fractions of a whole number. If
     * fractionalBits is not zero, these bits will form the least significant
     * bits of the integers. Due to the size constraints that arise from
     * arithmetic on <code>int</code>s the number of fractional bits cannot
     * exceed 23. The sensible default value for the number of fractional bits
     * is twice the number of iterations; this default can be specified by
     * passing a negative number to this method.
     *
     * @param fractionalBits the number of binary fractional bits in the number
     * representation used in filters, or -1 for the default value
     */
    public void setFractionalBits(int fractionalBits) {
        if (fractionalBits < 0) {
            fractionalBits = iterations << 1;
        }
        if (fractionalBits > 23) {
            throw new IllegalArgumentException("fractionalBits exceeds 23, the largest storable in an int");
        }
        this.fractionalBits = fractionalBits;
    }

    /**
     * Filters the byte values supplied by applying the Haar wavelet for a
     * number of iterations and stores the result in the specified integer
     * array. If no filter array is supplied then a new array is created. The
     * values returned may have been scaled to accomodate fractional bits.
     *
     * @param values a byte array of length containing the values to be
     * filtered, not null
     * @param filter an array which is to contain the filter values, or null
     * @return the array which contains the filter values
     */
    public int[] filter(ProductData[] values, int[] filter) {
        //validate arguments
        if (values == null) {
            throw new IllegalArgumentException("null values");
        }
//        if (values.length != sizeSqr) {
//            throw new IllegalArgumentException("values array incorrect length");
//        }
        if (filter == null) {
            filter = new int[sizeSqr];
        } else {
            if (filter.length != sizeSqr) {
                throw new IllegalArgumentException("filter array incorrect length");
            }
        }

        //lazily allocate a buffer
        if (buffer == null) {
            buffer = new int[sizeSqr];
        }

        //convert values into integers
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < size; j++) {
                filter[i] = values[i].getElemIntAt(j) & 0xff;
            }
        }

        //filter the values
        for (int i = 0; i < iterations; i++) {
            int[] s;
            int[] t;

            int length = size >> i;
            //horizontal processing
            s = filter;
//            t = buffer;
            int hOffset = length >> 1;
            for (int y = 0; y < length; y++) {
                int sIndex, tIndex;
                tIndex = sIndex = y * size;
                for (int x = 0; x < length; x += 2) {
                    int a = s[sIndex];
                    int b = s[sIndex + 1];
//                    t[tIndex] = a + b;
//                    t[tIndex + hOffset] = a - b;
                    sIndex += 2;
                    tIndex++;
                }
            }
            //vertical processing
            s = buffer;
//            t = filter;
            int vOffset = (length >> 1) * size;
            for (int x = 0; x < length; x++) {
                int sIndex, tIndex;
                sIndex = tIndex = x;
                for (int y = 0; y < length; y += 2) {
                    int a = s[sIndex];
                    int b = s[sIndex + size];
//                    t[tIndex] = a + b;
//                    t[tIndex + vOffset] = a - b;
                    sIndex += size << 1;
                    tIndex += size;
                }
            }

            length <<= 1;
        }

        //normalize the number of fractional bits before returning
        normalize(filter);
        return filter;
    }

    /**
     * Converts a filter created by this object back into the supplied byte
     * array. If no byte array is supplied a new one is created.
     *
     * If the <code>iterations</code> and <code>fractionalBits</code> parameters
     * of this object have been modified since the filter was created then the
     * data will not be faithfully reconstructed. If the number of fractional
     * bits is less than its default value then the inversion may be imperfect
     * due to accumulated rounding errors.
     *
     * <strong>This method destroys the supplied filter array - this is done
     * intentionally for performance reasons. If you want to preserve your
     * filter clone it before passing it into this method.</strong>
     *
     * @param filter an array containing the filter values, not null
     * @param values an array which is to contain the recovered data, or null
     * @return the array which contains the recovered data
     */
    /*
     * NOTE: As indicated, this implementation has not been thoroughly tested.
     * In particular, it might be possible that this method could overflow when
     * inverting large filters. I have not yet had the time to prove otherwise.
     */
    public byte[] invert(int[] filter, byte[] values) {
        //validate arguments
        if (filter == null) {
            throw new IllegalArgumentException("null filter");
        }
        if (filter.length != sizeSqr) {
            throw new IllegalArgumentException("filter array incorrect length");
        }
        if (values == null) {
            values = new byte[sizeSqr];
        } else {
            if (values.length != sizeSqr) {
                throw new IllegalArgumentException("values array incorrect length");
            }
        }

        //lazily allocate a buffer
        if (buffer == null) {
            buffer = new int[sizeSqr];
        }

        //reverse the filtering process
        for (int i = iterations - 1; i >= 0; i--) {
            int[] s;
            int[] t;

            int length = size >> i;
            //vertical processing
            s = filter;
            t = buffer;
            int vOffset = (length >> 1) * size;
            for (int x = 0; x < length; x++) {
                int sIndex, tIndex;
                sIndex = tIndex = x;
                for (int y = 0; y < length; y += 2) {
                    int a = s[sIndex];
                    int b = s[sIndex + vOffset];
                    t[tIndex] = a + b;
                    t[tIndex + size] = a - b;
                    sIndex += size;
                    tIndex += size << 1;
                }
            }
            //horizontal processing
            s = buffer;
            t = filter;
            int hOffset = length >> 1;
            for (int y = 0; y < length; y++) {
                int sIndex, tIndex;
                tIndex = sIndex = y * size;
                for (int x = 0; x < length; x += 2) {
                    int a = s[sIndex];
                    int b = s[sIndex + hOffset];
                    t[tIndex] = a + b;
                    t[tIndex + 1] = a - b;
                    sIndex++;
                    tIndex += 2;
                }
            }

            length >>= 1;
        }

        //remove the fractional bits and convert to bytes
        for (int i = 0; i < values.length; i++) {
            int value = filter[i] >> fractionalBits;
            if (value > 255) {
                value = 255;
            } else if (value < 0) {
                value = 0;
            }
            values[i] = (byte) value;
        }

        return values;
    }

    /**
     * A utility method for converting filter values generated by this object
     * into an <code>TYPE_BYTE_GRAY</code> <code>BufferedImage</code>. If the
     * <code>fractionalBits</code> parameter of this object has been modified
     * since the filter was created then the image may not be accurately
     * generated.
     *
     * @param filter an array containing the filter values, not null
     * @return the generated image
     */
    public BufferedImage filterToImage(int[] filter) {
        if (filter == null) {
            throw new IllegalArgumentException("null filter");
        }
        if (filter.length != sizeSqr) {
            throw new IllegalArgumentException("filter array incorrect length");
        }
        byte[] data = new byte[filter.length];

        int shift = fractionalBits + 1;
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (128 + (filter[i] >> shift));
        }
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        image.getWritableTile(0, 0).setDataElements(0, 0, size, size, data);
        image.releaseWritableTile(0, 0);
        return image;
    }

    /**
     * A utility method for converting data values into an
     * <code>TYPE_BYTE_GRAY</code> <code>BufferedImage</code>.
     *
     * @param values an array containing the data values, not null
     * @return the generated image
     */
    public BufferedImage valuesToImage(byte[] values) {
        if (values == null) {
            throw new IllegalArgumentException("null values");
        }
        if (values.length != sizeSqr) {
            throw new IllegalArgumentException("values array incorrect length");
        }

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        image.getWritableTile(0, 0).setDataElements(0, 0, size, size, values);
        image.releaseWritableTile(0, 0);
        return image;
    }

    /*
     * Adjusts for accumulated fractional bits in the raw filter array.
     */
    private void normalize(int[] filter) {
        int shiftD = -2;
        int shiftY = (iterations << 1) - fractionalBits;

        int nextY = size >> (iterations - 1);
        for (int y = 0; y < size; y++) {
            if (y == nextY) {
                shiftY += shiftD;
                nextY <<= 1;
            }
            int i = y * size;
            int nextX = nextY;
            int shiftX = shiftY;
            for (int x = 0; x < size; x++) {
                if (x == nextX) {
                    shiftX += shiftD;
                    nextX <<= 1;
                }
                if (shiftX > 0) {
                    filter[i] >>= shiftX;
                } else if (shiftX < 0) {
                    filter[i] <<= -shiftX;
                }
                i++;
            }
        }
    }

}
