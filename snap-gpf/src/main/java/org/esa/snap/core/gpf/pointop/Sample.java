package org.esa.snap.core.gpf.pointop;

import org.esa.snap.core.datamodel.RasterDataNode;

/**
 * A sample represents the (geophysical) value of a {@link RasterDataNode} at a certain a certain pixel position.
 * <p>
 * This interface is not intended to be implemented by clients.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9
 */
public interface Sample {

    /**
     * @return The raster data node to which this sample belongs.
     */
    RasterDataNode getNode();

    /**
     * @return The index of the sample defined by the various
     *         {@link SourceSampleConfigurer} or {@link TargetSampleConfigurer}
     *         method and used within the sample arrays passed to
     *         {@link SampleOperator#computeSample(int, int, Sample[], WritableSample) computeSample()} or
     *         {@link PixelOperator#computePixel(int, int, Sample[], WritableSample[]) computePixel()} methods.
     */
    int getIndex();

    /**
     * @return The geophysical data type of this sample.
     * @see RasterDataNode#getGeophysicalDataType()
     */
    int getDataType();

    /**
     * Interprets an integer sample as a sequence of bits (flags).
     *
     * @param bitIndex The bit index. Valid range is zero to n-1, where n is 8, 16, 32, 64,
     *                 depending on the actual raster data type.
     * @return The sample value as {@code bit} at given bit index.
     */
    boolean getBit(int bitIndex);

    /**
     * @return The sample value as {@code boolean}.
     */
    boolean getBoolean();

    /**
     * @return The sample value as {@code int}.
     */
    int getInt();

    /**
     * @return The sample value as {@code float}.
     */
    float getFloat();

    /**
     * @return The sample value as {@code double}.
     */
    double getDouble();
}
