package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.opengis.feature.simple.SimpleFeature;

public class SimpleFeatureNode extends ProductNode {

    private static final String PROPERTY_NAME_SIMPLE_FEATURE = "simpleFeature";

    private final SimpleFeature simpleFeature;

    public SimpleFeatureNode(SimpleFeature simpleFeature) {
        this(simpleFeature, null);
    }

    public SimpleFeatureNode(SimpleFeature simpleFeature, String description) {
        super(simpleFeature.getID(), description);
        this.simpleFeature = simpleFeature;
    }

    public SimpleFeature getSimpleFeature() {
        return simpleFeature;
    }

    @Override
    public long getRawStorageSize(ProductSubsetDef subsetDef) {
        // todo: rq/* estimate feature size (2009-12-16)
        return getDescription().length();
    }

    @Override
    public void acceptVisitor(ProductVisitor visitor) {
    }

    public final Object getSimpleFeatureAttribute(String name) {
        return simpleFeature.getAttribute(name);
    }

    public final void setSimpleFeatureAttribute(String name, Object value) {
        final Object oldValue = getSimpleFeatureAttribute(name);
        if (!equals(oldValue, value)) {
            simpleFeature.setAttribute(name, value);
            fireProductNodeChanged(name, oldValue, value);
        }
    }

    public final void setDefaultGeometry(Object geometry) {
        setSimpleFeatureAttribute(simpleFeature.getFeatureType().getGeometryDescriptor().getLocalName(), geometry);
    }

    public final Object getDefaultGeometry() {
        return simpleFeature.getDefaultGeometry();
    }

    public void fireSimpleFeatureChanged() {
        fireProductNodeChanged(PROPERTY_NAME_SIMPLE_FEATURE);
    }

    private static boolean equals(Object value, Object other) {
        return value == other || value != null && value.equals(other);
    }
}
