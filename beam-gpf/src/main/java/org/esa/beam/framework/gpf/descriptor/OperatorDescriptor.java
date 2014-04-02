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
     * may not be exposed in user interfaces. The default is {@code false}.
     */
    boolean isInternal();

    // todo - rename before beam 5 rel
    /**
     * @return If {@code true}, the framework will not automatically write the target product of this
     * operator. Usually, the framework writes the target products of single operators or processing graphs
     * when executed from the GPT commandline operator's GUI. The default is {@code false}.
     * <p/>
     * Setting this property may be useful if your operator does not generate a new target
     * {@link org.esa.beam.framework.datamodel.Product Product} and/or if it
     * does its own writing of non-{@link org.esa.beam.framework.datamodel.Product Product}
     * targets to external files in any format.
     */
    boolean isSuppressWrite();

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
