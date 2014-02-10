package org.esa.pfa.fe.op;

/**
* @author Norman Fomferra
*/
public class FeatureType extends AttributeType {
    private final AttributeType[] attributeTypes;

    public FeatureType(String name, String description, Class<?> valueType) {
        super(name, description, valueType);
        this.attributeTypes = null;
    }

    public FeatureType(String name, String description, AttributeType... attributeTypes) {
        super(name, description, Void.TYPE);
        this.attributeTypes = attributeTypes;
    }

    public AttributeType[] getAttributeTypes() {
        return attributeTypes;
    }

    public boolean hasAttributes() {
        return attributeTypes != null && attributeTypes.length > 0;
    }
}
