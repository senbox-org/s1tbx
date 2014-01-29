package org.esa.beam.framework.gpf.support;

import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;

/**
 * @author Norman Fomferra
 */
public class DefaultOperatorDescriptor implements OperatorSpi.OperatorDescriptor {

    String name;
    String alias;
    String label;
    String version;
    String description;
    String authors;
    String copyright;
    Boolean internal;

    DefaultSourceProductDescriptor[] sourceProductDescriptors;
    DefaultParameterDescriptor[] parameterDescriptors;
    DefaultTargetProductDescriptor targetProductDescriptor;
    DefaultTargetPropertyDescriptor[] targetPropertyDescriptors;


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
    public Class<? extends Operator> getDataType() {
        return Operator.class;
    }

    @Override
    public OperatorSpi.SourceProductDescriptor[] getSourceProductDescriptors() {
        return sourceProductDescriptors;
    }

    @Override
    public OperatorSpi.ParameterDescriptor[] getParameterDescriptors() {
        return parameterDescriptors;
    }

    @Override
    public OperatorSpi.TargetPropertyDescriptor[] getTargetPropertyDescriptors() {
        return targetPropertyDescriptors;
    }

    @Override
    public OperatorSpi.TargetProductDescriptor getTargetProductDescriptor() {
        return targetProductDescriptor;
    }
}
