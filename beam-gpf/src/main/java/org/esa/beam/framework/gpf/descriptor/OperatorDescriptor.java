package org.esa.beam.framework.gpf.descriptor;

import org.esa.beam.framework.gpf.Operator;

/**
 * Operator metadata.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public interface OperatorDescriptor extends ElementDescriptor {
    /**
     * @return The version of the operator, or {@code null} if not declared.
     */
    String getVersion();

    /**
     * @return The author(s) of the operator, or {@code null} if not declared.
     */
    String getAuthors();

    /**
     * @return The copyright notice for the operator code, or {@code null} if not declared.
     */
    String getCopyright();

    /**
     * @return If {@code true}, this operator is considered for internal use only and thus
     * may not be exposed in user interfaces.
     */
    boolean isInternal();

    /**
     * @return A concrete, non-abstract operator class.
     */
    Class<? extends Operator> getOperatorClass();

    /**
     * @return The source product descriptors.
     * The array will be empty if the operator does not have any source products.
     */
    SourceProductDescriptor[] getSourceProductDescriptors();

    /**
     * @return The source products descriptor, or {@code null} if none is declared.
     */
    SourceProductsDescriptor getSourceProductsDescriptor();

    /**
     * @return The parameter descriptors.
     * The array will be empty if the operator does not have any parameters.
     */
    ParameterDescriptor[] getParameterDescriptors();

    /**
     * @return The target product descriptor, or {@code null} if none is declared.
     */
    TargetProductDescriptor getTargetProductDescriptor();

    /**
     * @return The target property descriptors.
     * The array will be empty if the operator does not produce any target properties.
     */
    TargetPropertyDescriptor[] getTargetPropertyDescriptors();

}
