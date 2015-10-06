package org.esa.snap.core.gpf.pointop;

import org.esa.snap.core.datamodel.RasterDataNode;

/**
 * A {@code TargetSampleConfigurer} is used to define the target samples processed by a {@link PointOperator}.
 * The definition of a sample is given by its index within the pixel (a pixel comprises one or more samples) and
 * the name of a {@link RasterDataNode RasterDataNode} contained in the target
 * product.
 * <p>
 * This interface is not intended to be implemented by clients.
 *
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public interface TargetSampleConfigurer {
    /**
     * Defines a sample for a {@link RasterDataNode RasterDataNode} contained in the
     * target product.
     *
     * @param index The index of the sample within the sample arrays passed to
     *              {@link SampleOperator#computeSample(int, int, Sample[], WritableSample) computeSample()} or
     *              {@link PixelOperator#computePixel(int, int, Sample[], WritableSample[]) computePixel()} methods.
     * @param name  The name of a {@link RasterDataNode RasterDataNode} to
     *              which the sample belongs.
     */
    void defineSample(int index, String name);
}
