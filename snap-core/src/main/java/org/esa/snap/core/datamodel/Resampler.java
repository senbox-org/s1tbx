package org.esa.snap.core.datamodel;

/**
 * An interface for classes that resample a multi-size product to a single-size product.
 *
 * @author Tonio Fincke
 */
public interface Resampler {

    /**
     * Returns the name of the resampler. The Resampler will be visible in the user interface by this name.
     *
     * @return The name of the Resampler.
     */
    String getName();

    /**
     * Returns a description of the resampler.
     *
     * @return A description of the resampler.
     */
    String getDescription();

    /**
     * Performs a check whether the product can be resampled to a single-size product.
     *
     * @param multiSizeProduct A product with multiple sizes and/or resolutions.
     * @return true, if the product can be resampled.
     */
    boolean canResample(Product multiSizeProduct);

    /**
     * Resamples a multi-size product to a single-size product. This method is responsible for either
     * saving the created product to disk or adding it to the product manager.
     *
     * @param multiSizeProduct  A product with multiple sizes and/or resolutions.
     */
    Product resample(Product multiSizeProduct);

}
