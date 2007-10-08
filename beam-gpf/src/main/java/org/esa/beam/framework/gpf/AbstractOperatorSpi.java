package org.esa.beam.framework.gpf;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Product;

import java.util.Map;
import java.util.Set;

/**
 * Base class for operator SPIs.
 */
public abstract class AbstractOperatorSpi implements OperatorSpi {

    private String name;
    private String version;
    private String description;
    private String author;
    private String copyright;
    private Class<? extends Operator> operatorClass;

    protected AbstractOperatorSpi(Class<? extends Operator> operatorClass) {
        this(operatorClass, operatorClass.getName());
    }

    protected AbstractOperatorSpi(Class<? extends Operator> operatorClass, String name) {
        Assert.notNull(name, "name");
        Assert.notNull(operatorClass, "operatorClass");
        this.name = name;
        this.operatorClass = operatorClass;
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
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
