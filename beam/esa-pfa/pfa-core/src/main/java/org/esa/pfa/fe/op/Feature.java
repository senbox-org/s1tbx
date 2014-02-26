package org.esa.pfa.fe.op;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ExtensibleObject;

/**
 * A feature represents an arbitrary name/value pair or name/attribute set pair.
 * It is an instantiation of a {@link FeatureType}.
 *
 * @author Norman Fomferra
 */
public class Feature extends ExtensibleObject {
    private final FeatureType featureType;
    private final Object value;
    private final Object[] attributeValues;

    public Feature(FeatureType featureType, Object value, Object... attributeValues) {
        Assert.notNull(featureType, "featureType");
        validate(featureType, value, attributeValues);
        this.featureType = featureType;
        this.value = value;
        this.attributeValues = attributeValues;
    }

    private void validate(FeatureType featureType, Object value, Object[] attributeValues) {
        if (value != null) {
            Class<?> valueType = featureType.getValueType();
            if (!valueType.isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException("Expected " + valueType + ", but got " + value.getClass());
            }
        }
        if (featureType.hasAttributes()) {
            AttributeType[] attributeTypes = featureType.getAttributeTypes();
            if (attributeTypes != null) {
                if (attributeTypes.length != attributeValues.length) {
                    throw new IllegalArgumentException("Expected " + attributeTypes.length + " attribute value(s), but got " + attributeValues.length);
                }
            } else {
                throw new IllegalArgumentException("Expected no attribute values, but got " + attributeValues.length);
            }
        }
    }

    public String getName() {
        return featureType.getName();
    }

    public Class<?> getValueType() {
        return featureType.getValueType();
    }

    public FeatureType getFeatureType() {
        return featureType;
    }

    public Object getValue() {
        return value;
    }

    public Object[] getAttributeValues() {
        return attributeValues;
    }

    public boolean hasAttributes() {
        return featureType.hasAttributes() && attributeValues != null && attributeValues.length > 0;
    }
}
