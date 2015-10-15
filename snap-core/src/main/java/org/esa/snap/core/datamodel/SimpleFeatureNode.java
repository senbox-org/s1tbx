/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.opengis.feature.simple.SimpleFeature;

class SimpleFeatureNode extends ProductNode {

    private static final String PROPERTY_NAME_SIMPLE_FEATURE = "simpleFeature";

    private final SimpleFeature simpleFeature;

    SimpleFeatureNode(SimpleFeature simpleFeature) {
        this(simpleFeature, null);
    }

    SimpleFeatureNode(SimpleFeature simpleFeature, String description) {
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
