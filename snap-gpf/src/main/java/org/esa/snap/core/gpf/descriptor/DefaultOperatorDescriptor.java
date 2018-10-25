package org.esa.snap.core.gpf.descriptor;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.converters.ClassConverter;
import com.bc.ceres.core.Assert;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.converters.JavaTypeConverter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    Boolean autoWriteSuppressed;

    DefaultSourceProductDescriptor[] sourceProductDescriptors;
    DefaultSourceProductsDescriptor sourceProductsDescriptor;
    DefaultParameterDescriptor[] parameterDescriptors;
    DefaultTargetProductDescriptor targetProductDescriptor;
    DefaultTargetPropertyDescriptor[] targetPropertyDescriptors;

    DefaultOperatorDescriptor() {
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
    public boolean isAutoWriteDisabled() {
        return autoWriteSuppressed != null ? autoWriteSuppressed : false;
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
        return operatorClass != null ? operatorClass : Operator.class;
    }

    @Override
    public SourceProductDescriptor[] getSourceProductDescriptors() {
        return sourceProductDescriptors != null ? sourceProductDescriptors : new SourceProductDescriptor[0];
    }

    @Override
    public SourceProductsDescriptor getSourceProductsDescriptor() {
        return sourceProductsDescriptor;
    }

    @Override
    public ParameterDescriptor[] getParameterDescriptors() {
        return parameterDescriptors != null ? parameterDescriptors : new ParameterDescriptor[0];
    }

    @Override
    public TargetPropertyDescriptor[] getTargetPropertyDescriptors() {
        return targetPropertyDescriptors != null ? targetPropertyDescriptors : new TargetPropertyDescriptor[0];
    }

    @Override
    public TargetProductDescriptor getTargetProductDescriptor() {
        return targetProductDescriptor;
    }


    /**
     * Loads an operator descriptor from an XML document.
     *
     * @param url         The URL pointing to a valid operator descriptor XML document.
     * @param classLoader The class loader is used to load classed specified in the xml. For example the
     *                    class defined by the {@code operatorClass} tag.
     * @return A new operator descriptor.
     */
    public static DefaultOperatorDescriptor fromXml(URL url, ClassLoader classLoader) {
        String resourceName = url.toExternalForm();
        try {
            try (InputStreamReader streamReader = new InputStreamReader(url.openStream())) {
                DefaultOperatorDescriptor operatorDescriptor;
                operatorDescriptor = fromXml(streamReader, resourceName, classLoader);
                return operatorDescriptor;
            }
        } catch (IOException e) {
            throw new OperatorException(formatReadExceptionText(resourceName, e), e);
        }
    }

    /**
     * Loads an operator descriptor from an XML document.
     *
     * @param file        The file containing a valid operator descriptor XML document.
     * @param classLoader The class loader is used to load classed specified in the xml. For example the
     *                    class defined by the {@code operatorClass} tag.
     * @return A new operator descriptor.
     */
    public static DefaultOperatorDescriptor fromXml(File file, ClassLoader classLoader) throws OperatorException {
        String resourceName = file.getPath();
        try {
            try (FileReader reader = new FileReader(file)) {
                return DefaultOperatorDescriptor.fromXml(reader, resourceName, classLoader);
            }
        } catch (IOException e) {
            throw new OperatorException(formatReadExceptionText(resourceName, e), e);
        }
    }

    /**
     * Loads an operator descriptor from an XML document.
     *
     * @param reader       The reader providing a valid operator descriptor XML document.
     * @param resourceName Used in error messages
     * @param classLoader  The class loader is used to load classed specified in the xml. For example the
     *                     class defined by the {@code operatorClass} tag.
     * @return A new operator descriptor.
     */
    public static DefaultOperatorDescriptor fromXml(Reader reader, String resourceName, ClassLoader classLoader) throws OperatorException {
        Assert.notNull(reader, "reader");
        Assert.notNull(resourceName, "resourceName");
        DefaultOperatorDescriptor descriptor = new DefaultOperatorDescriptor();
        try {
            createXStream(classLoader).fromXML(reader, descriptor);
            if (StringUtils.isNullOrEmpty(descriptor.getName())) {
                throw new OperatorException(formatInvalidExceptionMessage(resourceName, "missing 'name' element"));
            }
            if (StringUtils.isNullOrEmpty(descriptor.getAlias())) {
                throw new OperatorException(formatInvalidExceptionMessage(resourceName, "missing 'alias' element"));
            }
        } catch (StreamException e) {
            throw new OperatorException(formatReadExceptionText(resourceName, e), e);
        }
        return descriptor;
    }

    /**
     * Converts an operator descriptor to XML.
     *
     * @param classLoader The class loader is used to load classed specified in the xml. For example the
     *                    class defined by the {@code operatorClass} tag.
     * @return A string containing valid operator descriptor XML.
     */
    public String toXml(ClassLoader classLoader) {
        return createXStream(classLoader).toXML(this);
    }


    private static XStream createXStream(ClassLoader classLoader) {
        XStream xStream = new XStream();

        xStream.setClassLoader(classLoader);

        xStream.alias("operator", DefaultOperatorDescriptor.class);

        xStream.alias("sourceProduct", DefaultSourceProductDescriptor.class);
        xStream.aliasField("namedSourceProducts", DefaultOperatorDescriptor.class, "sourceProductDescriptors");

        xStream.alias("sourceProducts", DefaultSourceProductsDescriptor.class);
        xStream.aliasField("sourceProducts", DefaultOperatorDescriptor.class, "sourceProductsDescriptor");

        xStream.alias("parameter", DefaultParameterDescriptor.class);
        xStream.aliasField("parameters", DefaultOperatorDescriptor.class, "parameterDescriptors");
        xStream.registerLocalConverter(DefaultParameterDescriptor.class, "dataType", new ParameterTypeConverter());
        xStream.registerLocalConverter(DefaultParameterDescriptor.class, "valueSet", new ValueSetConverter());

        xStream.alias("targetProduct", DefaultTargetProductDescriptor.class);
        xStream.aliasField("targetProduct", DefaultOperatorDescriptor.class, "targetProductDescriptor");

        xStream.alias("targetProperty", DefaultTargetPropertyDescriptor.class);
        xStream.aliasField("targetProperties", DefaultOperatorDescriptor.class, "targetPropertyDescriptors");

        return xStream;
    }

    private static String formatReadExceptionText(String resourceName, Exception e) {
        return String.format("Failed to read operator descriptor from '%s':\nError: %s", resourceName, e.getMessage());
    }

    private static String formatInvalidExceptionMessage(String resourceName, String message) {
        return String.format("Invalid operator descriptor in '%s': %s", resourceName, message);
    }

    private static class ParameterTypeConverter extends AbstractSingleValueConverter {

        private ClassConverter converter;

        ParameterTypeConverter() {
            converter = new JavaTypeConverter();
        }

        @Override
        public boolean canConvert(Class aClass) {
            return Class.class.equals(aClass);
        }

        @Override
        public Object fromString(String s) {
            try {
                return converter.parse(s);
            } catch (ConversionException e) {
                return null;
            }
        }

        @Override
        public String toString(Object obj) {
            return converter.format((Class) obj);
        }
    }

    private static class ValueSetConverter implements com.thoughtworks.xstream.converters.Converter {

        public boolean canConvert(Class aClass) {
            return String[].class.equals(aClass);
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            DefaultParameterDescriptor headerParameter = (DefaultParameterDescriptor) source;
            writer.addAttribute("valueSet", StringUtils.arrayToString(headerParameter.getValueSet(), ","));
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

            final String valueSetString = reader.getValue();
            if (valueSetString != null) {
                return StringUtils.toStringArray(valueSetString, ",");
            }
            return null;
        }
    }
}
