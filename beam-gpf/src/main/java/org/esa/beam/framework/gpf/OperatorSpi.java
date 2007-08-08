package org.esa.beam.framework.gpf;

/**
 * <p>The <code>OperatorSpi</code> is the service provider interface (SPI) for {@link Operator}s.
 * It is a factory for {@link Operator}s and provides operator metadata.</p>
 * <p/>
 * <p>Clients shall not implement or extend the interface <code>OperatorSpi</code> directly. Instead
 * they should derive from {@link org.esa.beam.framework.gpf.AbstractOperatorSpi}.</p>
 *
 * @since 4.1
 */
public interface OperatorSpi {

    /**
     * Gets the operator class.
     * The operator class must be public and provide a public zero-argument constructor.
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
