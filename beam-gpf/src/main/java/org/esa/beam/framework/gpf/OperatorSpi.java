package org.esa.beam.framework.gpf;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;

import java.util.Map;
import java.util.Set;

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
 * @since 4.1
 */
public abstract class OperatorSpi {

    private Class<? extends Operator> operatorClass;
    private String name;
    private String version;
    private String description;
    private String authors;
    private String copyright;

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
        this(operatorClass,
             getAliasName(operatorClass),
             getVersion(operatorClass),
             getDescription(operatorClass),
             getAuthor(operatorClass),
             getCopyright(operatorClass));
    }

    /**
     * Constructs an operator SPI for the given class name and alias name.
     *
     * @param operatorClass The operator class.
     * @param aliasName     The alias name for the operator.
     * @deprecated either use constructor {@link #OperatorSpi(Class,String,String,String,String,String)}
     *             or use constructor {@link #OperatorSpi(Class)} and operator annotation {@link OperatorMetadata}.
     */
    protected OperatorSpi(Class<? extends Operator> operatorClass, String aliasName) {
        this(operatorClass,
             aliasName,
             getVersion(operatorClass),
             getDescription(operatorClass),
             getAuthor(operatorClass),
             getCopyright(operatorClass));
    }

    /**
     * Constructs an operator SPI for the given class name and alias name and metadata.
     *
     * @param operatorClass The operator class.
     * @param aliasName     The alias name for the operator.
     * @param version       The operator version.
     * @param description   The operator description.
     * @param author        The operator author.
     * @param copyright     The operator copyright.
     */
    protected OperatorSpi(Class<? extends Operator> operatorClass,
                          String aliasName,
                          String version,
                          String description,
                          String author,
                          String copyright) {
        Assert.notNull(operatorClass, "operatorClass");
        Assert.notNull(aliasName, "name");
        this.operatorClass = operatorClass;
        this.name = aliasName;
        this.version = version;
        this.description = description;
        this.authors = author;
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
        operator.context.setParameters(parameters);
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
    public String getAliasName() {
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
    public String getAuthors() {
        return authors;
    }

    /**
     * Sets the name of the author
     *
     * @param authors the authors name
     */
    protected void setAuthors(String authors) {
        this.authors = authors;
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


    private static String getAliasName(Class<? extends Operator> operatorClass) {
        OperatorMetadata annotation = operatorClass.getAnnotation(OperatorMetadata.class);
        if (annotation != null && !annotation.alias().isEmpty()) {
            return annotation.alias();
        }
        return operatorClass.getSimpleName();
    }

    private static String getVersion(Class<? extends Operator> operatorClass) {
        OperatorMetadata annotation = operatorClass.getAnnotation(OperatorMetadata.class);
        if (annotation != null && annotation.version() != null) {
            return annotation.version();
        }
        return "";
    }

    private static String getAuthor(Class<? extends Operator> operatorClass) {
        OperatorMetadata annotation = operatorClass.getAnnotation(OperatorMetadata.class);
        if (annotation != null && annotation.authors() != null) {
            return annotation.authors();
        }
        return "";
    }

    private static String getCopyright(Class<? extends Operator> operatorClass) {
        OperatorMetadata annotation = operatorClass.getAnnotation(OperatorMetadata.class);
        if (annotation != null && annotation.copyright() != null) {
            return annotation.authors();
        }
        return "";
    }

    private static String getDescription(Class<? extends Operator> operatorClass) {
        OperatorMetadata annotation = operatorClass.getAnnotation(OperatorMetadata.class);
        if (annotation != null && annotation.description() != null) {
            return annotation.description();
        }
        return "";
    }
}
