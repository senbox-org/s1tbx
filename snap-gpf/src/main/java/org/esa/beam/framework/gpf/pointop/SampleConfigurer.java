package org.esa.beam.framework.gpf.pointop;

import org.esa.beam.framework.datamodel.Product;

/**
 * A {@code SampleConfigurer} is used to define the samples processed by a {@link PointOperator}. These can be both,
 * source or target samples.
 * The definition of a sample is given by its index within the pixel (a pixel comprises one or more samples) and
 * the name of a {@link org.esa.beam.framework.datamodel.RasterDataNode RasterDataNode}.
 * <p/>
 * This interface is not intended to be implemented by clients.
 *
 * @author Norman Fomferra
 * @since BEAM 4.9
 */
public interface SampleConfigurer {

    /**
     * Defines a sample for a {@link org.esa.beam.framework.datamodel.RasterDataNode RasterDataNode}.
     *
     * @param index The index of the sample within the sample arrays passed to
     *              {@link SampleOperator#computeSample(int, int, Sample[], WritableSample) computeSample()} or
     *              {@link PixelOperator#computePixel(int, int, Sample[], WritableSample[]) computePixel()} methods.
     * @param name  The name of a {@link org.esa.beam.framework.datamodel.RasterDataNode RasterDataNode} to
     *              which the sample belongs.
     */
    void defineSample(int index, String name);

    /**
     * Defines a sample for a {@link org.esa.beam.framework.datamodel.RasterDataNode RasterDataNode} in the given product.
     *
     * @param index   The index of the sample within the sample arrays passed to
     *                {@link SampleOperator#computeSample(int, int, Sample[], WritableSample) computeSample()} or
     *                {@link PixelOperator#computePixel(int, int, Sample[], WritableSample[]) computePixel()} methods.
     * @param name    The name of a {@code RasterDataNode} to
     *                which the sample belongs.
     * @param product The product in which to find the raster data node's name.
     */
    void defineSample(int index, String name, Product product);
}
