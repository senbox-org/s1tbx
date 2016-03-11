package org.esa.snap.core.gpf.descriptor;

import org.esa.snap.core.datamodel.Product;

/**
 * Source product element metadata.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public interface SourceProductDescriptor extends DataElementDescriptor {

    /**
     * @return {@code true} if the source product is optional.
     * Defaults to {@code false}.
     */
    boolean isOptional();

    /**
     * @return The product type or a regular expression identifying the allowed product types, or {@code null} if not declared.
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
     * Defaults to {@link Product}.
     */
    Class<? extends Product> getDataType();
}
