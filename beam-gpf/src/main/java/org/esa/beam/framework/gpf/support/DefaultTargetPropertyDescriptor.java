package org.esa.beam.framework.gpf.support;

import org.esa.beam.framework.gpf.OperatorSpi;

/**
 * @author Norman Fomferra
 */
public class DefaultTargetPropertyDescriptor implements OperatorSpi.TargetPropertyDescriptor {

    String name;
    String alias;
    String label;
    String description;
    Class<?> dataType;

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
    public Class<?> getDataType() {
        return dataType;
    }
}
