package org.esa.beam.framework.gpf.internal;

import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.ParameterConverter;

public class Xpp3DomParameterInjector implements ParameterInjector {

    private final Xpp3Dom configuration;

    public Xpp3DomParameterInjector(Xpp3Dom configuration) {
        this.configuration = configuration;
    }

    public void injectParameters(Operator operator) throws OperatorException {
        if (configuration != null) {
            if (operator instanceof ParameterConverter) {
                ParameterConverter converter = (ParameterConverter) operator;
                try {
                    converter.setParameterValues(operator, configuration);
                } catch (Throwable t) {
                    throw new OperatorException(t);
                }
            } else {
                DefaultParameterConverter defaultParameterConverter = new DefaultParameterConverter();
                defaultParameterConverter.setParameterValues(operator, configuration);
            }
        }
    }
}
