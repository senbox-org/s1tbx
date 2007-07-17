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

    String getAuthor();

    String getCopyright();

    String getDescription();

    String getVersion();
}
