package org.esa.snap.core.dataop.barithm;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.jexp.WritableNamespace;

/**
 * Extends product {@link WritableNamespace namespaces} which are used for expression evaluation.
 * Clients are asked to register symbols or functions derived from a product into the given namespace.
 * <p>
 * New extenders can be provided by the JAR Service Provider Interface via the file
 * {@code META-INF/services/ProductNamespaceExtender}.
 *
 * @author Norman Fomferra
 * @since SNAP 2
 */
public interface ProductNamespaceExtender {

    /**
     * Extend the namespace by extra product symbols or functions.
     * All new symbol or function names must be prefixed using the given prefix string.
     *
     * @param product    The product.
     * @param namePrefix The prefix for new symbol or function names.
     * @param namespace  The namespace to extend.
     */
    void extendNamespace(Product product, String namePrefix, WritableNamespace namespace);
}
