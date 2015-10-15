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

package org.esa.snap.core.util;

import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Reusable forwarding class delegating to a {@link SimpleFeature} instance.
 */
public class ForwardingSimpleFeature implements SimpleFeature {

    private final SimpleFeature simpleFeature;

    public ForwardingSimpleFeature(SimpleFeature simpleFeature) {
        this.simpleFeature = simpleFeature;
    }

    public SimpleFeature getSimpleFeature() {
        return simpleFeature;
    }

    @Override
    public String getID() {
        return simpleFeature.getID();
    }

    @Override
    public AttributeDescriptor getDescriptor() {
        return simpleFeature.getDescriptor();
    }

    @Override
    public Name getName() {
        return simpleFeature.getName();
    }

    @Override
    public boolean isNillable() {
        return simpleFeature.isNillable();
    }

    @Override
    public Map<Object, Object> getUserData() {
        return simpleFeature.getUserData();
    }

    @Override
    public SimpleFeatureType getType() {
        return simpleFeature.getType();
    }

    @Override
    public void setValue(Collection<Property> properties) {
        simpleFeature.setValue(properties);
    }

    @Override
    public Collection<? extends Property> getValue() {
        return simpleFeature.getValue();
    }

    @Override
    public void setValue(Object o) {
        simpleFeature.setValue(o);
    }

    @Override
    public Collection<Property> getProperties(Name name) {
        return simpleFeature.getProperties(name);
    }

    @Override
    public Property getProperty(Name name) {
        return simpleFeature.getProperty(name);
    }

    @Override
    public Collection<Property> getProperties(String s) {
        return simpleFeature.getProperties(s);
    }

    @Override
    public Collection<Property> getProperties() {
        return simpleFeature.getProperties();
    }

    @Override
    public Property getProperty(String s) {
        return simpleFeature.getProperty(s);
    }

    @Override
    public void validate() throws IllegalAttributeException {
        simpleFeature.validate();
    }

    @Override
    public FeatureId getIdentifier() {
        return simpleFeature.getIdentifier();
    }

    @Override
    public BoundingBox getBounds() {
        return simpleFeature.getBounds();
    }

    @Override
    public GeometryAttribute getDefaultGeometryProperty() {
        return simpleFeature.getDefaultGeometryProperty();
    }

    @Override
    public void setDefaultGeometryProperty(GeometryAttribute geometryAttribute) {
        simpleFeature.setDefaultGeometryProperty(geometryAttribute);
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return simpleFeature.getFeatureType();
    }

    @Override
    public List<Object> getAttributes() {
        return simpleFeature.getAttributes();
    }

    @Override
    public void setAttributes(List<Object> objects) {
        simpleFeature.setAttributes(objects);
    }

    @Override
    public void setAttributes(Object[] objects) {
        simpleFeature.setAttributes(objects);
    }

    @Override
    public Object getAttribute(String s) {
        return simpleFeature.getAttribute(s);
    }

    @Override
    public void setAttribute(String s, Object o) {
        simpleFeature.setAttribute(s, o);
    }

    @Override
    public Object getAttribute(Name name) {
        return simpleFeature.getAttribute(name);
    }

    @Override
    public void setAttribute(Name name, Object o) {
        simpleFeature.setAttribute(name, o);
    }

    @Override
    public Object getAttribute(int i) throws IndexOutOfBoundsException {
        return simpleFeature.getAttribute(i);
    }

    @Override
    public void setAttribute(int i, Object o) throws IndexOutOfBoundsException {
        simpleFeature.setAttribute(i, o);
    }

    @Override
    public int getAttributeCount() {
        return simpleFeature.getAttributeCount();
    }

    @Override
    public Object getDefaultGeometry() {
        return simpleFeature.getDefaultGeometry();
    }

    @Override
    public void setDefaultGeometry(Object o) {
        simpleFeature.setDefaultGeometry(o);
    }
}
