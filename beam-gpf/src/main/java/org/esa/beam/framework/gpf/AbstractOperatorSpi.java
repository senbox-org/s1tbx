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
    public Class<? extends Operator> getOperatorClass() {
        return operatorClass;
    }

    /**
     * Sets the class of the {@link Operator}.
     *
     * @param operatorClass the class of the {@link Operator}
     */
    public void setOperatorClass(Class<? extends Operator> operatorClass) {
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
    public void setName(String name) {
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
    public void setAuthor(String author) {
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
    public void setCopyright(String copyright) {
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
    public void setDescription(String description) {
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
    public void setVersion(String version) {
        this.version = version;
    }
}
