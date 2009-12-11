package org.esa.beam.util;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyAccessor;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * A decorating {@link SimpleFeature}, which has a {@link PropertySet} backed by
 * the decorated feature's attributes.
 */
public class PropertySetSimpleFeature extends ForwardingSimpleFeature {

    private final PropertySet propertySet;

    /**
     * Constructs a new instance of this class from a simple feature.
     *
     * @param simpleFeature the simple feature.
     */
    public PropertySetSimpleFeature(SimpleFeature simpleFeature) {
        super(simpleFeature);
        propertySet = new PropertyContainer();
        final SimpleFeatureType simpleFeatureType = getSimpleFeature().getFeatureType();

        for (final AttributeDescriptor attributeDescriptor : simpleFeatureType.getAttributeDescriptors()) {
            final String name = attributeDescriptor.getLocalName();
            final Object value = getSimpleFeature().getAttribute(name);
            final PropertyAccessor accessor = new PropertyAccessor() {
                @Override
                public Object getValue() {
                    return getSimpleFeature().getAttribute(name);
                }

                @Override
                public void setValue(Object value) {
                    getSimpleFeature().setAttribute(name, value);
                }
            };
            final PropertyDescriptor propertyDescriptor = new PropertyDescriptor(name, value.getClass());
            propertyDescriptor.setNotNull(!attributeDescriptor.isNillable());
            final Object defaultValue = attributeDescriptor.getDefaultValue();
            propertyDescriptor.setDefaultValue(defaultValue);
            propertySet.addProperty(new Property(propertyDescriptor, accessor));
        }
    }

    /**
     * Returns the property set. The proterty set returned is backed by this feature's
     * attributes.
     *
     * @return the property set.
     */
    public PropertySet getPropertySet() {
        return propertySet;
    }

    @Override
    public void setAttribute(String s, Object o) {
        propertySet.setValue(s, o);
    }
}
