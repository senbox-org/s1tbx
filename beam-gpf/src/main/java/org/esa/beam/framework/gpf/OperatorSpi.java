package org.esa.beam.framework.gpf;

import org.esa.beam.framework.datamodel.Product;

import java.util.Map;

/**
 * <p>The <code>OperatorSpi</code> class is the service provider interface (SPI) for {@link Operator}s.
 * The SPI is both a descriptor for the operator type and a factory for new {@link Operator} instances.</p>
 * <p/>
 * <p>Clients shall not implement or extend the interface <code>OperatorSpi</code> directly. Instead
 * they should derive from {@link org.esa.beam.framework.gpf.AbstractOperatorSpi}.</p>
 *
 * @since 4.1
 */
public interface OperatorSpi {
    /**
     * Creates an operator instance with no arguments. The default implemrentation calls
     * the default constructor. If no such is defined in the operator, an exception is thrown.
     * Override in order to provide a no-argument instance of your operator.
     * Implementors should call {@link Operator#setSpi(OperatorSpi) operator.setSpi(this)}
     * in order to set the operator's SPI.
     *
     * @return the operator instance
     * @throws OperatorException if the instance could not be created
     */
    Operator createOperator() throws OperatorException;

    /**
     * Creates an operator instance for the given source products and processing parameters.
     *
     * @param parameters     the processing parameters
     * @param sourceProducts the source products
     * @return the operator instance
     * @throws OperatorException if the operator could not be created
     */
    Operator createOperator(Map<String, Object> parameters, Map<String, Product> sourceProducts) throws OperatorException;

    /**
     * Gets the operator class.
     * The operator class must be public and provide a public zero-argument constructor.
     *
     * @return the operator class
     */
    Class<? extends Operator> getOperatorClass();

    /**
     * The unique name under which the operator can be accessed.
     *
     * @return the name of the (@link Operator)
     */
    String getName();

    /**
     * The name of the author of the {@link Operator} this SPI is responsible for.
     *
     * @return the authors name
     */
    String getAuthor();

    /**
     * Gets a copyright note for the {@link Operator}.
     *
     * @return a copyright note
     */
    String getCopyright();

    /**
     * Gets a short description of the {@link Operator}.
     *
     * @return description of the operator
     */
    String getDescription();

    /**
     * Gets the version number of the {@link Operator}.
     *
     * @return the version number
     */
    String getVersion();
}
