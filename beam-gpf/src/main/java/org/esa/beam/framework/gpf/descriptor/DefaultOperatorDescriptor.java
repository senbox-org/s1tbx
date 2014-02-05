package org.esa.beam.framework.gpf.descriptor;

import com.thoughtworks.xstream.XStream;
import org.esa.beam.framework.gpf.Operator;

import java.io.Reader;
import java.net.URL;

/**
 * Default implementation of the {@link OperatorDescriptor} interface.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class DefaultOperatorDescriptor implements OperatorDescriptor {

    String name;
    Class<? extends Operator> operatorClass;
    String alias;
    String label;
    String version;
    String description;
    String authors;
    String copyright;
    Boolean internal;

    DefaultSourceProductDescriptor[] sourceProductDescriptors;
    DefaultSourceProductsDescriptor sourceProductsDescriptor;
    DefaultParameterDescriptor[] parameterDescriptors;
    DefaultTargetProductDescriptor targetProductDescriptor;
    DefaultTargetPropertyDescriptor[] targetPropertyDescriptors;

    public DefaultOperatorDescriptor() {
    }

    public DefaultOperatorDescriptor(String name, Class<? extends Operator> operatorClass) {
        this.name = name;
        this.operatorClass = operatorClass;
        sourceProductDescriptors = new DefaultSourceProductDescriptor[0];
        parameterDescriptors = new DefaultParameterDescriptor[0];
        targetPropertyDescriptors = new DefaultTargetPropertyDescriptor[0];
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getAuthors() {
        return authors;
    }

    @Override
    public String getCopyright() {
        return copyright;
    }

    @Override
    public boolean isInternal() {
        return internal != null ? internal : false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Class<? extends Operator> getOperatorClass() {
        return operatorClass;
    }

    @Override
    public SourceProductDescriptor[] getSourceProductDescriptors() {
        return sourceProductDescriptors;
    }

    @Override
    public SourceProductsDescriptor getSourceProductsDescriptor() {
        return sourceProductsDescriptor;
    }

    @Override
    public ParameterDescriptor[] getParameterDescriptors() {
        return parameterDescriptors;
    }

    @Override
    public TargetPropertyDescriptor[] getTargetPropertyDescriptors() {
        return targetPropertyDescriptors;
    }

    @Override
    public TargetProductDescriptor getTargetProductDescriptor() {
        return targetProductDescriptor;
    }


    /**
     * Loads an operator descriptor from an XML document.
     *
     * @param url The URL pointing to a valid operator descriptor XML document.
     * @return A new operator descriptor.
     */
    public static DefaultOperatorDescriptor fromXml(URL url) {
        DefaultOperatorDescriptor descriptor = new DefaultOperatorDescriptor();
        createXStream().fromXML(url, descriptor);
        return descriptor;
    }

    /**
     * Loads an operator descriptor from an XML document.
     *
     * @param reader The reader providing a valid operator descriptor XML document.
     * @return A new operator descriptor.
     */
    public static DefaultOperatorDescriptor fromXml(Reader reader) {
        DefaultOperatorDescriptor descriptor = new DefaultOperatorDescriptor();
        createXStream().fromXML(reader, descriptor);
        return descriptor;
    }

    /**
     * Loads an operator descriptor from an XML document.
     *
     * @param xml A valid operator descriptor XML.
     * @return A new operator descriptor.
     */
    public static DefaultOperatorDescriptor fromXml(String xml) {
        DefaultOperatorDescriptor descriptor = new DefaultOperatorDescriptor();
        createXStream().fromXML(xml, descriptor);
        return descriptor;
    }

    /**
     * Converts an operator descriptor to XML.
     *
     * @return A string containing valid operator descriptor XML.
     */
    public String toXml() {
        return createXStream().toXML(this);
    }


    static XStream createXStream() {
        XStream xStream = new XStream();
        xStream.alias("operator", DefaultOperatorDescriptor.class);

        xStream.alias("sourceProduct", DefaultSourceProductDescriptor.class);
        xStream.aliasField("namedSourceProducts", DefaultOperatorDescriptor.class, "sourceProductDescriptors");

        xStream.alias("sourceProducts", DefaultSourceProductsDescriptor.class);
        xStream.aliasField("sourceProducts", DefaultOperatorDescriptor.class, "sourceProductsDescriptor");

        xStream.alias("parameter", DefaultParameterDescriptor.class);
        xStream.aliasField("parameters", DefaultOperatorDescriptor.class, "parameterDescriptors");

        xStream.alias("targetProduct", DefaultTargetProductDescriptor.class);
        xStream.aliasField("targetProduct", DefaultOperatorDescriptor.class, "targetProductDescriptor");

        xStream.alias("targetProperty", DefaultTargetPropertyDescriptor.class);
        xStream.aliasField("targetProperties", DefaultOperatorDescriptor.class, "targetPropertyDescriptors");

        return xStream;
    }

}
