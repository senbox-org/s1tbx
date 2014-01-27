/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.framework.gpf;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.internal.ModuleReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;

import java.awt.RenderingHints;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

/**
 * <p>The <code>OperatorSpi</code> class is the service provider interface (SPI) for {@link Operator}s.
 * Therefore this abstract class is intended to be derived by clients.</p>
 * <p>The SPI is both a descriptor for the operator type and a factory for new {@link Operator} instances.
 * <p>An SPI is required for your operator if you want to make it accessible via an alias name in
 * the various {@link GPF}{@code .create} methods or within GPF Graph XML code.</p>
 * <p>SPI are registered either programmatically using the
 * {@link org.esa.beam.framework.gpf.GPF#getOperatorSpiRegistry() OperatorSpiRegistry} or
 * automatically via standard Java services lookup mechanism. For the services approach, place a
 * file {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}
 * in the JAR file containing your operators and associated SPIs.
 * For each SPI to be automatically registered, place a text line in the file containing the SPI's
 * fully qualified class name.</p>
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @since 4.1
 */
public abstract class OperatorSpi {

    private final Class<? extends Operator> operatorClass;
    private final String operatorAlias;
    private Module module;
    //private final OperatorDescriptor operatorDescriptor;

    /**
     * Constructs an operator SPI for the given operator class. The alias name
     * and other metadata will be taken from the operator annotation
     * {@link OperatorMetadata}. If no such exists,
     * the alias name will be the operator's class name without the package path.
     * All other metadata will be set to the empty string.
     *
     * @param operatorClass The operator class.
     */
    protected OperatorSpi(Class<? extends Operator> operatorClass) {
        this(operatorClass, getOperatorAlias(operatorClass));
    }

    /**
     * Constructs an operator SPI for the given class name and alias name.
     *
     * @param operatorClass The operator class.
     * @param operatorAlias The alias name for the operator.
     */
    protected OperatorSpi(Class<? extends Operator> operatorClass, String operatorAlias) {
        this.operatorClass = operatorClass;
        this.operatorAlias = operatorAlias;
       /*
        OperatorMetadata annotation = operatorClass.getAnnotation(OperatorMetadata.class);
        if (annotation != null) {
            operatorDescriptor = new AnnotationOperatorDescriptor(annotation);
        } else {
            operatorDescriptor = new AnnotationOperatorDescriptor(new OperatorMetadata());
        }
        */
    }

    /**
     * <p>Creates an operator instance with no arguments. The default implementation calls
     * the default constructor. If no such is defined in the operator, an exception is thrown.</p>
     * <p>This method may be overridden by clients in order to provide a no-argument instance of their operator.
     * Implementors should call {@link Operator#setSpi(OperatorSpi) operator.setSpi(this)}
     * in order to set the operator's SPI.</p>
     *
     * @return the operator instance
     * @throws OperatorException if the instance could not be created
     */
    public Operator createOperator() throws OperatorException {
        try {
            final Operator operator = getOperatorClass().newInstance();
            operator.setSpi(this);
            return operator;
        } catch (InstantiationException e) {
            throw new OperatorException(e);
        } catch (IllegalAccessException e) {
            throw new OperatorException(e);
        }
    }

    /**
     * <p>Creates an operator instance for the given source products and processing parameters.</p>
     * <p>This method may be overridden by clients in order to process the passed parameters and
     * source products and optionally construct the operator in a specific way.
     * Implementors should call {@link Operator#setSpi(OperatorSpi) operator.setSpi(this)}
     * in order to set the operator's SPI.</p>
     *
     * @param parameters     the processing parameters.
     * @param sourceProducts the source products.
     * @return the operator instance.
     * @throws OperatorException if the operator could not be created.
     */
    public Operator createOperator(Map<String, Object> parameters,
                                   Map<String, Product> sourceProducts) throws OperatorException {
        return createOperator(parameters, sourceProducts, null);
    }

    /**
     * <p>Creates an operator instance for the given source products and processing parameters.</p>
     * <p>This method may be overridden by clients in order to process the passed parameters and
     * source products and optionally construct the operator in a specific way.
     * Implementors should call {@link Operator#setSpi(OperatorSpi) operator.setSpi(this)}
     * in order to set the operator's SPI.</p>
     *
     * @param parameters     the processing parameters.
     * @param sourceProducts the source products.
     * @param renderingHints the rendering hints, may be {@code null}.
     * @return the operator instance.
     * @throws OperatorException if the operator could not be created.
     */
    public Operator createOperator(Map<String, Object> parameters,
                                   Map<String, Product> sourceProducts,
                                   RenderingHints renderingHints) throws OperatorException {
        final Operator operator = createOperator();
        operator.context.setSourceProducts(sourceProducts);
        operator.context.setParameters(parameters);
        if (renderingHints != null) {
            operator.context.addRenderingHints(renderingHints);
        }
        return operator;
    }

    /**
     * Gets the operator class.
     * The operator class must be public and provide a public zero-argument constructor.
     *
     * @return the operator class
     */
    public final Class<? extends Operator> getOperatorClass() {
        return operatorClass;
    }

    /**
     * The alias name under which the operator can be accessed.
     *
     * @return The alias name of the (@link Operator).
     */
    public final String getOperatorAlias() {
        return operatorAlias;
    }

    /**
     * The module containing the operator.
     *
     * @return The {@link Module module} containing the operator or {@code null} if no module is defined.
     */
    public Module getModule() {
        if (module == null) {
            this.module = loadModule();
        }
        return module;
    }

    public static String getOperatorAlias(Class<? extends Operator> operatorClass) {
        OperatorMetadata annotation = operatorClass.getAnnotation(OperatorMetadata.class);
        if (annotation != null && !annotation.alias().isEmpty()) {
            return annotation.alias();
        }
        return operatorClass.getSimpleName();
    }

    private Module loadModule() {
        ModuleReader moduleReader = new ModuleReader(Logger.getAnonymousLogger());
        URL moduleLocation = operatorClass.getProtectionDomain().getCodeSource().getLocation();
        try {
            return moduleReader.readFromLocation(moduleLocation);
        } catch (CoreException e) {
            Logger.getAnonymousLogger().warning("Could not read " + moduleLocation.toString());
        }
        return null;
    }

/*
    public OperatorDescriptor getOperatorDescriptor() {
        return operatorDescriptor;
    }

    public SourceProductDescriptor[] getSourceProductDescriptors() {

    }

    public TargetProductDescriptor getTargetProductDescriptor() {

    }

    public ParameterDescriptor[] getParameterDescriptors() {

    }
  */
    public static interface ItemDescriptor {
        /**
         * @return The name of the operator.
         */
        String getName();

        /**
         * @return The item description.
         */
        String getDescription();
    }

    public static interface OperatorDescriptor extends ItemDescriptor {

        /**
         * @return The version of the operator.
         *         Defaults to the empty string (= not set).
         */
        String getVersion();

        /**
         * @return The author(s) of the operator.
         *         Defaults to the empty string (= not set).
         */
        String getAuthors();

        /**
         * @return The copyright notice for the operator code.
         *         Defaults to the empty string (= not set).
         */
        String getCopyright() ;

        /**
         * @return If {@code true}, this operator is considered for internal use only and thus
         *         may not be exposed in user interfaces.
         */
        boolean isInternal();
    }

    public static interface SourceProductDescriptor {
        /**
         * @return {@code true} if the source product is optional.
         *         In this case the field value thus may be {@code null}.
         *         Defaults to {@code false}.
         */
        boolean isOptional();

        /**
         * @return The product type or a regular expression identifying the allowed product types.
         *         Defaults to the empty string (= not set).
         * @see java.util.regex.Pattern
         */
        String getType();

        /**
         * @return The names of the bands which need to be present in the source product.
         *         Defaults to an empty array (= not set).
         */
        String[] getBands();

        String getLabel();
    }

    public static interface TargetProductDescriptor {
    }

    public static interface ParameterDescriptor {
       /**
        * @return An alias name for the elements of a parameter array.
        *         Forces element-wise array conversion from and to DOM representation.
        *         Defaults to the empty string (= not set).
        * @see #areItemsInlined()
        */
       String getItemName();

       /**
        * @return If {@code true} items of parameter array values are inlined (not
        *         enclosed by the parameter name) in the DOM representation of the
        *         array. In this case also the ({@code itemName} must be given.
        *         Defaults to {@code false}.
        * @see #getItemName()
        */
       boolean areItemsInlined();

       /**
        * Gets the parameter's default value.
        * The default value set is given as a textual representations of the actual value.
        * The framework creates the actual value set by converting the text value to
        * an object using the associated {@link com.bc.ceres.binding.Converter}.
        *
        * @return The default value.
        *         Defaults to the empty string (= not set).
        * @see #getConverter()
        */
       String getDefaultValue();

       /**
        * @return A parameter label.
        *         Defaults to the empty string (= not set).
        */
       String getLabel();

       /**
        * @return The parameter physical unit.
        *         Defaults to the empty string (= not set).
        */
       String getUnit();

       /**
        * @return The parameter description.
        *         Defaults to the empty string (= not set).
        */
       String getDescription();

       /**
        * Gets the set of values which can be assigned to a parameter field.
        * The value set is given as textual representations of the actual values.
        * The framework creates the actual value set by converting each text value to
        * an object value using the associated {@link com.bc.ceres.binding.Converter}.
        *
        * @return The value set.Defaults to empty array (= not set).
        * @see #getConverter()
        */
       String[] getValueSet();

       /**
        * Gets the valid interval for numeric parameters, e.g. {@code "[10,20)"}: in the range 10 (inclusive) to 20 (exclusive).
        *
        * @return The valid interval. Defaults to empty string (= not set).
        */
       String getInterval();

       /**
        * Gets a conditional expression which must return {@code true} in order to indicate
        * that the parameter value is valid, e.g. {@code "value > 2.5"}.
        *
        * @return A conditional expression. Defaults to empty string (= not set).
        */
       String getCondition();

       /**
        * Gets a regular expression pattern to which a textual parameter value must match in order to indicate
        * a valid value, e.g. {@code "a*"}.
        *
        * @return A regular expression pattern. Defaults to empty string (= not set).
        * @see java.util.regex.Pattern
        */
       String getPattern();

       /**
        * Gets a format string to which a textual parameter value must match in order to indicate
        * a valid value, e.g. {@code "yyyy-MM-dd HH:mm:ss.Z"}.
        *
        * @return A format string. Defaults to empty string (= not set).
        * @see java.text.Format
        */
       String getFormat();

       /**
        * Parameter value must not be {@code null}?
        *
        * @return {@code true}, if so. Defaults to {@code false}.
        */
       boolean isNotNull();

       /**
        * Parameter value must not be an empty string?
        *
        * @return {@code true}, if so. Defaults to {@code false}.
        */
       boolean isNotEmpty();

       /**
        * A validator to be used to validate a parameter value.
        *
        * @return The validator class.
        */
       Class<? extends Validator> getValidator();

       /**
        * A converter to be used to convert a text to the parameter value and vice versa.
        *
        * @return The converter class.
        */
       Class<? extends Converter> getConverter();

       /**
        * A converter to be used to convert an (XML) DOM to the parameter value and vice versa.
        *
        * @return The DOM converter class.
        */
       Class<? extends DomConverter> getDomConverter();

        /**
        * Specifies which {@code RasterDataNode} subclass of the source products is used
        * to fill the {@link #getValueSet()} for this parameter.
        *
        * @return The raster data node type.
        */
       Class<? extends RasterDataNode> getRasterDataNodeType();
    }
/*
    private static class AnnotationOperatorDescriptor implements OperatorDescriptor {
        private final OperatorMetadata annotation;

        public AnnotationOperatorDescriptor(OperatorMetadata annotation) {
            this.annotation = annotation;
        }

        @Override
        public String getAlias() {
            return annotation.alias();
        }

        @Override
        public String getVersion() {
            return annotation.version();
        }

        @Override
        public String getAuthors() {
            return annotation.authors();
        }

        @Override
        public String getCopyright() {
            return annotation.copyright();
        }

        @Override
        public String getDescription() {
            return annotation.description();
        }

        @Override
        public boolean isInternal() {
            return annotation.internal();
        }
    }

    private static class DefaultOperatorDescriptor implements OperatorDescriptor {

        public AnnotationOperatorDescriptor(OperatorMetadata annotation) {
            this.annotation = annotation;
        }

        @Override
        public String getAlias() {
            return annotation.alias();
        }

        @Override
        public String getVersion() {
            return annotation.version();
        }

        @Override
        public String getAuthors() {
            return annotation.authors();
        }

        @Override
        public String getCopyright() {
            return annotation.copyright();
        }

        @Override
        public String getDescription() {
            return annotation.description();
        }

        @Override
        public boolean isInternal() {
            return annotation.internal();
        }
    }

    private static class AnnotationSourceProductDescriptor implements SourceProductDescriptor {
        private final SourceProduct annotation;

        public AnnotationSourceProductDescriptor(SourceProduct annotation) {
            this.annotation = annotation;
        }

        @Override
        public boolean isOptional() {
            return annotation.optional();
        }

        @Override
        public String getType() {
            return annotation.type();
        }

        @Override
        public String[] getBands() {
            return annotation.bands();
        }

        @Override
        public String getAlias() {
            return annotation.alias();
        }

        @Override
        public String getDescription() {
            return annotation.description();
        }

        @Override
        public String getLabel() {
            return annotation.label();
        }
    }

    private static class AnnotationTargetProductDescriptor implements TargetProductDescriptor {

        private final TargetProduct annotation;

        public AnnotationTargetProductDescriptor(TargetProduct annotation) {
            this.annotation = annotation;
        }

        @Override
        public String getDescription() {
            return annotation.description();
        }
    }

    private static class AnnotationParameterDescriptor implements ParameterDescriptor {
        private final Parameter annotation;

        public AnnotationParameterDescriptor(Parameter annotation) {
            this.annotation = annotation;
        }

        @Override
        public String getAlias() {
            return annotation.alias();
        }

        @Override
        public String getItemAlias() {
            return annotation.itemAlias();
        }

        @Override
        public boolean areItemsInlined() {
            return annotation.itemsInlined();
        }

        @Override
        public String getDefaultValue() {
            return annotation.defaultValue();
        }

        @Override
        public String getLabel() {
            return annotation.label();
        }

        @Override
        public String getUnit() {
            return annotation.unit();
        }

        @Override
        public String getDescription() {
            return annotation.description();
        }

        @Override
        public String[] getValueSet() {
            return annotation.valueSet();
        }

        @Override
        public String getInterval() {
            return annotation.interval();
        }

        @Override
        public String getCondition() {
            return annotation.condition();
        }

        @Override
        public String getPattern() {
            return annotation.pattern();
        }

        @Override
        public String getFormat() {
            return annotation.format();
        }

        @Override
        public boolean isNotNull() {
            return annotation.notNull();
        }

        @Override
        public boolean isNotEmpty() {
            return annotation.notEmpty();
        }

        @Override
        public Class<? extends Validator> getValidator() {
            return annotation.validator();
        }

        @Override
        public Class<? extends Converter> getConverter() {
            return annotation.converter();
        }

        @Override
        public Class<? extends DomConverter> getDomConverter() {
            return annotation.domConverter();
        }

        @Override
        public Class<? extends RasterDataNode> getRasterDataNodeType() {
            return annotation.rasterDataNodeType();
        }
    }

*/
}
