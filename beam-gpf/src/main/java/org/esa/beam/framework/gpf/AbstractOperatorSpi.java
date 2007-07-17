package org.esa.beam.framework.gpf;

import com.bc.ceres.core.Assert;

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
        this(operatorClass, operatorClass.getClass().getName());
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
    public Class<? extends Operator> getOperatorClass() {
        return operatorClass;
    }

    public void setOperatorClass(Class<? extends Operator> operatorClass) {
        this.operatorClass = operatorClass;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * {@inheritDoc}
     */
    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * {@inheritDoc}
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
