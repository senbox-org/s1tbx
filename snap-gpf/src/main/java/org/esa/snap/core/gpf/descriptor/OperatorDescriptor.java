package org.esa.snap.core.gpf.descriptor;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;

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

    /**
     * The GPF framework usually writes the target product of either single operators or processing graphs to the file
     * system when executed from the GPT command-line interface or the operator GUI.
     * <p>
     * If the {@code autoWriteDisabled} property is set, this default behaviour is switched off and hence,
     * the operator or graph is responsible for outputting any computed results.
     * <p>
     * Setting this property on an operator will only be useful, if it either does not generate a new target
     * {@link Product Product} and/or if it
     * does its own outputting of non-{@code Product} targets to external files.
     *
     * @return If {@code true}, the framework will prevent automatic writing of the target product to the file system.
     * @since BEAM 5.0
     */
    boolean isAutoWriteDisabled();

    /**
     * @return The operator implementation class.
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
