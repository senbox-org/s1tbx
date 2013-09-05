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

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;

import java.awt.RenderingHints;
import java.util.Map;

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
    }

    /**
     * <p>Creates an operator instance with no arguments. The default implementation calls
     * the default constructor. If no such is defined in the operator, an exception is thrown.</p>
     * <p>This method may be overridden by clients in order to provide a no-argument instance of their operator.
     * Implementors should call {@link Operator#setSpi(OperatorSpi) operator.setSpi(this)}
     * in order to set the operator's SPI.</p>
     *
     * @return the operator instance
     *
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
     *
     * @return the operator instance.
     *
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
     *
     * @return the operator instance.
     *
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

    public static String getOperatorAlias(Class<? extends Operator> operatorClass) {
        OperatorMetadata annotation = operatorClass.getAnnotation(OperatorMetadata.class);
        if (annotation != null && !annotation.alias().isEmpty()) {
            return annotation.alias();
        }
        return operatorClass.getSimpleName();
    }
}
