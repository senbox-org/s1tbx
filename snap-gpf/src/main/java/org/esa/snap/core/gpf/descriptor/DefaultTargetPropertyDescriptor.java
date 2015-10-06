package org.esa.snap.core.gpf.descriptor;

/**
 * Default implementation of the {@link TargetPropertyDescriptor} interface.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class DefaultTargetPropertyDescriptor implements TargetPropertyDescriptor {

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
