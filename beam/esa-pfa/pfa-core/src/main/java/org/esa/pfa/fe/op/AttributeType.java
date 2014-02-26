package org.esa.pfa.fe.op;

import com.bc.ceres.core.Assert;

/**
 * Describes an attribute as part of a {@link FeatureType}.
 *
 * @author Norman Fomferra
 */
public class AttributeType {
    private final String name;
    private final String description;
    private final Class<?> valueType;

    public AttributeType(String name, String description, Class<?> valueType) {
        Assert.notNull(name, "name");
        Assert.notNull(description, "description");
        Assert.notNull(valueType, "valueType");
        this.name = name;
        this.description = description;
        this.valueType = valueType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Class<?> getValueType() {
        return valueType;
    }
}
