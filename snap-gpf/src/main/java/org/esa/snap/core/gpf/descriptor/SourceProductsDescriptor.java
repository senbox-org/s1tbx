package org.esa.snap.core.gpf.descriptor;

import org.esa.snap.core.datamodel.Product;

/**
 * Source products element metadata.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public interface SourceProductsDescriptor extends DataElementDescriptor {

    /**
     * @return the number of source products expected.
     * The value {@code -1} means any number but at least one source product.
     * Defaults to {@code 0} (= not set).
     */
    int getCount();

    /**
     * @return The product type or a regular expression identifying the allowed product types.
     * Defaults to the empty string (= not set).
     * @see java.util.regex.Pattern
     */
    String getProductType();

    /**
     * @return The names of the bands which need to be present in the source product.
     * Defaults to an empty array (= not set).
     */
    String[] getBands();

    /**
     * @return The source product type.
     * Defaults to {@link Product}[].
     */
    Class<? extends Product[]> getDataType();

}
