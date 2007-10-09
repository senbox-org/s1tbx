package org.esa.beam.framework.gpf;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Product;

import java.util.Map;
import java.util.Set;

/**
 * <p>The <code>OperatorSpi</code> class is the service provider interface (SPI) for {@link Operator}s.
 * The SPI is both a descriptor for the operator type and a factory for new {@link Operator} instances.</p>
 * <p>This class is intended to be implemented by clients.</p>
 * todo - write about META-INF/services  (nf - 09.10.2007)
 *
 * @since 4.1
 */
public class OperatorSpi {

    private Class<? extends Operator> operatorClass;
    private String name;
    private String version;
    private String description;
    private String author;
    private String copyright;

    /**
     * todo
     *
     * @param operatorClass todo
     */
    protected OperatorSpi(Class<? extends Operator> operatorClass) {
        this(operatorClass, operatorClass.getSimpleName());
    }

    /**
     * todo
     *
     * @param operatorClass todo
     * @param name          todo
     */
    protected OperatorSpi(Class<? extends Operator> operatorClass, String name) {
        this(operatorClass, name, "1.0", "", "", "");
    }

    /**
     * todo
     *
     * @param operatorClass todo
     * @param name          todo
     * @param version       todo
     * @param description   todo
     * @param author        todo
     * @param copyright     todo
     */
    protected OperatorSpi(Class<? extends Operator> operatorClass, String name, String version, String description, String author, String copyright) {
        Assert.notNull(operatorClass, "operatorClass");
        Assert.notNull(name, "name");
        this.operatorClass = operatorClass;
        this.name = name;
        this.version = version;
        this.description = description;
        this.author = author;
        this.copyright = copyright;
    }

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
     * Creates an operator instance for the given source products and processing parameters.
     *
     * @param parameters     the processing parameters
     * @param sourceProducts the source products
     * @return the operator instance
     * @throws OperatorException if the operator could not be created
     */
    public final Operator createOperator(Map<String, Object> parameters, Map<String, Product> sourceProducts) throws OperatorException {
        Operator operator = createOperator();
        Set<Map.Entry<String, Product>> entries = sourceProducts.entrySet();
        for (Map.Entry<String, Product> entry : entries) {
            operator.addSourceProduct(entry.getKey(), entry.getValue());
        }
        operator.setParameters(parameters);
        return operator;
    }

    /**
     * Gets the operator class.
     * The operator class must be public and provide a public zero-argument constructor.
     *
     * @return the operator class
     */
    public Class<? extends Operator> getOperatorClass() {
        return operatorClass;
    }

    /**
     * Sets the class of the {@link Operator}.
     *
     * @param operatorClass the class of the {@link Operator}
     */
    protected void setOperatorClass(Class<? extends Operator> operatorClass) {
        this.operatorClass = operatorClass;
    }

    /**
     * The unique name under which the operator can be accessed.
     *
     * @return the name of the (@link Operator)
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the {@link Operator}
     *
     * @param name the name of the {@link Operator}
     */
    protected void setName(String name) {
        this.name = name;
    }

    /**
     * The name of the author of the {@link Operator} this SPI is responsible for.
     *
     * @return the authors name
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the name of the author
     *
     * @param author the authors name
     */
    protected void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Gets a copyright note for the {@link Operator}.
     *
     * @return a copyright note
     */
    public String getCopyright() {
        return copyright;
    }

    /**
     * Sets the copyright note.
     *
     * @param copyright the copyright note
     */
    protected void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    /**
     * Gets a short description of the {@link Operator}.
     *
     * @return description of the operator
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets a short description of the {@link Operator}.
     *
     * @param description a short description
     */
    protected void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the version number of the {@link Operator}.
     *
     * @return the version number
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version number of the {@link Operator}.
     *
     * @param version the version number
     */
    protected void setVersion(String version) {
        this.version = version;
    }
}
