package org.esa.snap.framework.gpf.pointop;

import org.esa.snap.framework.datamodel.GeneralFilterBand;
import org.esa.snap.framework.datamodel.Kernel;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.RasterDataNode;

/**
 * A {@code SourceSampleConfigurer} is used to define the source samples processed by a {@link PointOperator}.
 * <p>
 * The definition of a sample is given by its index within the pixel (a pixel comprises one or more samples) and
 * the name of a {@link org.esa.snap.framework.datamodel.RasterDataNode RasterDataNode} in one of the source products.
 * <p>
 * Sample can also be computed on the fly, either by band maths expressions or by filters. See the various
 * {@code #defineComputedSample(...)} methods.
 * <p>
 * This interface is not intended to be implemented by clients.
 *
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public interface SourceSampleConfigurer {
    /**
     * Defines a sample for a {@link org.esa.snap.framework.datamodel.RasterDataNode RasterDataNode}.
     *
     * @param index The index of the sample within the sample arrays passed to
     *              {@link SampleOperator#computeSample(int, int, Sample[], WritableSample) computeSample()} or
     *              {@link PixelOperator#computePixel(int, int, Sample[], WritableSample[]) computePixel()} methods.
     * @param name  The name of a {@link org.esa.snap.framework.datamodel.RasterDataNode RasterDataNode} to
     *              which the sample belongs.
     */
    void defineSample(int index, String name);

    /**
     * Defines a sample for a {@link org.esa.snap.framework.datamodel.RasterDataNode RasterDataNode} in the given product.
     *
     * @param index   The index of the sample within the sample arrays passed to
     *                {@link SampleOperator#computeSample(int, int, Sample[], WritableSample) computeSample()} or
     *                {@link PixelOperator#computePixel(int, int, Sample[], WritableSample[]) computePixel()} methods.
     * @param name    The name of a {@code RasterDataNode} to
     *                which the sample belongs.
     * @param product The product in which to find the raster data node's name.
     */
    void defineSample(int index, String name, Product product);

    void defineComputedSample(int index, int dataType, String expression, Product ...sourceProducts);

    void defineComputedSample(int index, int sourceIndex, Kernel kernel);

    void defineComputedSample(int index, int sourceIndex, GeneralFilterBand.OpType opType, Kernel structuringElement);

    void defineComputedSample(int index, RasterDataNode node);

    void defineValidPixelMask(String maskExpression);
}
