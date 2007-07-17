package org.esa.beam.framework.gpf.internal;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XppDomReader;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.ParameterConverter;

public class DefaultParameterConverter implements ParameterConverter {

    public void getParameterValues(Operator operator, Xpp3Dom configuration) throws OperatorException {
        // todo - implement
    }

    public void setParameterValues(Operator operator, Xpp3Dom configuration) throws OperatorException {
        Class configurationObjectClass = operator.getClass();
        XStream xStream = new XStream();
        xStream.setClassLoader(operator.getClass().getClassLoader());
        xStream.alias(configuration.getName(), configurationObjectClass);
        xStream.unmarshal(new XppDomReader(configuration), operator);
    }
}
