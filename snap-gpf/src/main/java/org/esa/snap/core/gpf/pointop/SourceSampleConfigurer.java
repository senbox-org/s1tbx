package org.esa.snap.core.gpf.pointop;

import org.esa.snap.core.datamodel.ConvolutionFilterBand;
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VirtualBand;

/**
 * A {@code SourceSampleConfigurer} is used to define the source samples processed by a {@link PointOperator}.
 * <p>
 * The definition of a sample is given by its index within the pixel (a pixel comprises one or more samples) and
 * the name of a {@link RasterDataNode RasterDataNode} in one of the source products.
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
     * Defines a mask that identifies valid pixels (region of interest) using a Boolean expression.
     * The framework evaluates the expression for each target pixel. If the mask evaluates to zero, all the
     * target samples are set to their no-data values. Otherwise the respective
     * {@link PixelOperator#computePixel(int, int, Sample[], WritableSample[]) computePixel} or
     * {@link SampleOperator#computeSample(int, int, Sample[], WritableSample) computeSample} are called.
     *
     * @param maskExpression The Boolean valid-pixel mask expression.
     * @see RasterDataNode#setNoDataValueUsed(boolean)
     * @see RasterDataNode#setGeophysicalNoDataValue(double)
     */
    void setValidPixelMask(String maskExpression);

    /**
     * Defines a sample for a {@link RasterDataNode RasterDataNode}.
     *
     * @param index The index of the sample within the sample arrays passed to
     *              {@link SampleOperator#computeSample(int, int, Sample[], WritableSample) computeSample()} or
     *              {@link PixelOperator#computePixel(int, int, Sample[], WritableSample[]) computePixel()} methods.
     * @param name  The name of a {@link RasterDataNode RasterDataNode} to
     *              which the sample belongs.
     */
    void defineSample(int index, String name);

    /**
     * Defines a sample for a {@link RasterDataNode RasterDataNode} in the given product.
     *
     * @param index   The index of the sample within the sample arrays passed to
     *                {@link SampleOperator#computeSample(int, int, Sample[], WritableSample) computeSample()} or
     *                {@link PixelOperator#computePixel(int, int, Sample[], WritableSample[]) computePixel()} methods.
     * @param name    The name of a {@code RasterDataNode} to
     *                which the sample belongs.
     * @param product The product in which to find the raster data node's name.
     */
    void defineSample(int index, String name, Product product);

    /**
     * Defines an intermediate source sample computed from a band math expression.
     * <p>
     * The method effectively creates a {@link VirtualBand VirtualBand}
     * from which it computes the source samples.
     * <p>
     * If multiple source products are used a reference number {@link Product#setRefNo(int)} has to be assigned.
     *
     * @param index          The index of the sample within the sample arrays passed to
     *                       {@link SampleOperator#computeSample(int, int, Sample[], WritableSample) computeSample()} or
     *                       {@link PixelOperator#computePixel(int, int, Sample[], WritableSample[]) computePixel()} methods.
     * @param dataType       The data type of the computed sample. See {@code TYPE_X} constants in {@link ProductData}.
     * @param expression     The band maths expression.
     * @param sourceProducts Source products that are referenced in the expression.
     */
    void defineComputedSample(int index, int dataType, String expression, Product... sourceProducts);

    /**
     * Defines an intermediate source sample computed from a linear image convolution.
     * <p>
     * The method effectively creates a {@link ConvolutionFilterBand ConvolutionFilterBand}
     * from which it computes the source samples.
     *
     * @param index       The index of the sample within the sample arrays passed to
     *                    {@link SampleOperator#computeSample(int, int, Sample[], WritableSample) computeSample()} or
     *                    {@link PixelOperator#computePixel(int, int, Sample[], WritableSample[]) computePixel()} methods.
     * @param sourceIndex The index of the source sample that will be filtered.
     * @param kernel      The image convolution kernel.
     */
    void defineComputedSample(int index, int sourceIndex, Kernel kernel);

    /**
     * Defines an intermediate source sample computed from a non-linear image filter.
     * <p>
     * The method effectively creates a {@link GeneralFilterBand GeneralFilterBand}
     * from which it computes the source samples.
     *
     * @param index              The index of the sample within the sample arrays passed to
     *                           {@link SampleOperator#computeSample(int, int, Sample[], WritableSample) computeSample()} or
     *                           {@link PixelOperator#computePixel(int, int, Sample[], WritableSample[]) computePixel()} methods.
     * @param sourceIndex        The index of the source sample that will be filtered.
     * @param opType             The filter operation to be applied to pixels identified by the structuring element.
     * @param structuringElement The structuring element is a Boolean kernel identifying the pixel positions to be filtered.
     */
    void defineComputedSample(int index, int sourceIndex, GeneralFilterBand.OpType opType, Kernel structuringElement);

    /**
     * Defines an intermediate source sample computed from the given raster.
     * <p>
     * The raster is usually either a component of the source products or not attached to any product at all.
     * However, it must not be a component of the target product.
     *
     * @param index  The index of the sample within the sample arrays passed to
     *               {@link SampleOperator#computeSample(int, int, Sample[], WritableSample) computeSample()} or
     *               {@link PixelOperator#computePixel(int, int, Sample[], WritableSample[]) computePixel()} methods.
     * @param raster The index of the source sample that will be filtered.
     */
    void defineComputedSample(int index, RasterDataNode raster);
}
