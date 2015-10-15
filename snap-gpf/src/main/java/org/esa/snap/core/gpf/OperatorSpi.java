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

package org.esa.snap.core.gpf;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.descriptor.AnnotationOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.AnnotationOperatorDescriptorBody;
import org.esa.snap.core.gpf.descriptor.DefaultOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.SourceProductDescriptor;
import org.esa.snap.core.gpf.descriptor.SourceProductsDescriptor;
import org.esa.snap.core.gpf.descriptor.TargetProductDescriptor;
import org.esa.snap.core.gpf.descriptor.TargetPropertyDescriptor;
import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.SystemUtils;

import java.awt.RenderingHints;
import java.net.URL;
import java.util.Map;
import java.util.jar.Manifest;

/**
 * <p>The {@code OperatorSpi} class is the service provider interface (SPI) for {@link Operator}s.
 * Therefore this abstract class is intended to be derived by clients.
 * <p>The SPI is both a descriptor for the operator type and a factory for new {@link Operator} instances.
 * <p>An SPI is required for your operator if you want to make it accessible via an alias name in
 * the various {@link GPF}{@code .create} methods or within GPF Graph XML code.
 * <p>SPI are registered either pragmatically using the
 * {@link GPF#getOperatorSpiRegistry() OperatorSpiRegistry} or
 * automatically via standard Java services lookup mechanism. For the services approach, place a
 * file {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}
 * in the JAR file containing your operators and associated SPIs.
 * For each SPI to be automatically registered, place a text line in the file containing the SPI's
 * fully qualified class name.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @since 4.1
 */
public abstract class OperatorSpi {

    private final OperatorDescriptor operatorDescriptor;
    // Note: We need this only for backward compatibility with BEAM 4.11.
    private final String operatorAlias;

    // lazily loaded
    private Manifest manifest;

    /**
     * Constructs an operator SPI for the given operator descriptor.
     *
     * @param operatorDescriptor The operator descriptor.
     * @since BEAM 5
     */
    protected OperatorSpi(OperatorDescriptor operatorDescriptor) {
        Assert.notNull(operatorDescriptor, "operatorDescriptor");
        this.operatorDescriptor = operatorDescriptor;
        this.operatorAlias = operatorDescriptor.getAlias();
        Assert.notNull(operatorAlias, "operatorAlias");
    }

    /**
     * Constructs an operator SPI for the given URL pointing to a valid operator descriptor XML document.
     *
     * @param operatorDescriptorUrl The operator descriptor URL.
     * @since BEAM 5
     */
    protected OperatorSpi(URL operatorDescriptorUrl) {
        this(DefaultOperatorDescriptor.fromXml(operatorDescriptorUrl, DefaultOperatorDescriptor.class.getClassLoader()));
    }

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
     * @deprecated since BEAM 5, no replacement.
     */
    @Deprecated
    protected OperatorSpi(Class<? extends Operator> operatorClass, String operatorAlias) {
        Assert.notNull(operatorClass, "operatorClass");
        Assert.notNull(operatorAlias, "operatorAlias");
        OperatorMetadata annotation = operatorClass.getAnnotation(OperatorMetadata.class);
        if (annotation != null) {
            operatorDescriptor = new AnnotationOperatorDescriptor(operatorClass, annotation);
        } else {
            operatorDescriptor = new NoMetadataOperatorDescriptor(operatorClass, operatorAlias);
        }
        this.operatorAlias = operatorAlias;
    }

    /**
     * <p>Creates an operator instance with no arguments. The default implementation calls
     * the default constructor. If no such is defined in the operator, an exception is thrown.
     * <p>This method may be overridden by clients in order to provide a no-argument instance of their operator.
     * Implementors should call {@link Operator#setSpi(OperatorSpi) operator.setSpi(this)}
     * in order to set the operator's SPI.
     *
     * @return the operator instance
     * @throws OperatorException if the instance could not be created
     */
    public Operator createOperator() throws OperatorException {
        try {
            final Operator operator = getOperatorClass().newInstance();
            operator.setSpi(this);
            operator.setParameterDefaultValues();
            return operator;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new OperatorException(e);
        }
    }

    /**
     * <p>Creates an operator instance for the given source products and processing parameters.
     * <p>This method may be overridden by clients in order to process the passed parameters and
     * source products and optionally construct the operator in a specific way.
     * Implementors should call {@link Operator#setSpi(OperatorSpi) operator.setSpi(this)}
     * in order to set the operator's SPI.
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
     * <p>Creates an operator instance for the given source products and processing parameters.
     * <p>This method may be overridden by clients in order to process the passed parameters and
     * source products and optionally construct the operator in a specific way.
     * Implementors should call {@link Operator#setSpi(OperatorSpi) operator.setSpi(this)}
     * in order to set the operator's SPI.
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
        operator.context.setParameterMap(parameters);
        if (renderingHints != null) {
            operator.context.addRenderingHints(renderingHints);
        }
        return operator;
    }

    /**
     * Gets the operator implementation class.
     * The operator class must be public and provide a public zero-argument constructor.
     * <p>
     * Shorthand for {@link OperatorDescriptor#getOperatorClass() getOperatorDescriptor().getOperatorClass()}.
     *
     * @return The operator implementation class.
     */
    public final Class<? extends Operator> getOperatorClass() {
        return operatorDescriptor.getOperatorClass();
    }

    /**
     * The alias name under which the operator can be accessed.
     * <p>
     * Shorthand for {@code getOperatorDescriptor().getAlias()}.
     *
     * @return The alias name of the (@link Operator), or {@code null} if not declared.
     */
    public final String getOperatorAlias() {
        if (operatorAlias != null && !operatorAlias.isEmpty()) {
            return operatorAlias;
        }
        return operatorDescriptor.getAlias();
    }

    /**
     * The metadata information of the module which holds the operator provided by this SPI.
     *
     * @return the module metadata
     */
    public ModuleMetadata getModuleMetadata() {
        return SystemUtils.loadModuleMetadata(getOperatorDescriptor().getOperatorClass());
    }

    /**
     * @return The operator descriptor.
     * @since BEAM 5
     */
    public OperatorDescriptor getOperatorDescriptor() {
        return operatorDescriptor;
    }

    /**
     * Gets the alias name of the operator given by it's class.
     * The method returns the 'alias' element of the operator's {@link OperatorMetadata}, if any.
     * Otherwise it returns the class' simple name (without package path).
     *
     * @param operatorClass The operator class.
     * @return An operator alias name.
     */
    public static String getOperatorAlias(Class<? extends Operator> operatorClass) {
        OperatorMetadata annotation = operatorClass.getAnnotation(OperatorMetadata.class);
        if (annotation != null && !annotation.alias().isEmpty()) {
            return annotation.alias();
        }
        return operatorClass.getSimpleName();
    }

    private static class NoMetadataOperatorDescriptor implements OperatorDescriptor {

        private final AnnotationOperatorDescriptorBody body;
        private final String operatorAlias;

        public NoMetadataOperatorDescriptor(Class<? extends Operator> operatorClass, String operatorAlias) {
            this.body = new AnnotationOperatorDescriptorBody(operatorClass);
            this.operatorAlias = operatorAlias;
        }

        @Override
        public String getName() {
            return body.getOperatorClass().getName();
        }

        @Override
        public String getAlias() {
            return operatorAlias;
        }

        @Override
        public Class<? extends Operator> getOperatorClass() {
            return body.getOperatorClass();
        }

        @Override
        public String getVersion() {
            return null;
        }

        @Override
        public String getAuthors() {
            return null;
        }

        @Override
        public String getCopyright() {
            return null;
        }

        @Override
        public boolean isInternal() {
            return false;
        }

        @Override
        public boolean isAutoWriteDisabled() {
            return false;
        }

        @Override
        public String getLabel() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public SourceProductDescriptor[] getSourceProductDescriptors() {
            return body.getSourceProductDescriptors();
        }

        @Override
        public SourceProductsDescriptor getSourceProductsDescriptor() {
            return body.getSourceProductsDescriptor();
        }

        @Override
        public TargetProductDescriptor getTargetProductDescriptor() {
            return body.getTargetProductDescriptor();
        }

        @Override
        public TargetPropertyDescriptor[] getTargetPropertyDescriptors() {
            return body.getTargetPropertyDescriptors();
        }

        @Override
        public ParameterDescriptor[] getParameterDescriptors() {
            return body.getParameterDescriptors();
        }
    }
}
