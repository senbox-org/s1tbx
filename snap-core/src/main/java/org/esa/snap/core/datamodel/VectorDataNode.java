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

import com.bc.ceres.core.Assert;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.ObjectUtils;
import org.esa.snap.core.util.ObservableFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * A container which allows to store vector data in the BEAM product model.
 *
 * @author Norman Fomferra
 * @see Product#getVectorDataGroup()
 * @since BEAM 4.7
 */
public class VectorDataNode extends ProductNode {

    public static final String PROPERTY_NAME_FEATURE_COLLECTION = "featureCollection";
    public static final String PROPERTY_NAME_STYLE_CSS = "styleCss";
    public static final String PROPERTY_NAME_DEFAULT_STYLE_CSS = "defaultStyleCss";

    private static final String DEFAULT_STYLE_FORMAT = "fill:%s; fill-opacity:0.5; stroke:#ffffff; stroke-opacity:1.0; stroke-width:1.0; symbol:cross";
    private static final String[] FILL_COLORS = {
            "#ff0000", // red
            "#00ff00", // green
            "#0000ff", // blue
            "#aaff00",
            "#00aaff",
            "#ffaa00",
            "#ff00aa",
            "#aa00ff",
            "#00ffaa",
    };
    private static int fillColorIndex;


    private final SimpleFeatureType featureType;
    private final ObservableFeatureCollection featureCollection;
    private final PlacemarkDescriptor placemarkDescriptor;
    private PlacemarkGroup placemarkGroup;
    private String defaultStyleCss;
    private String styleCss;
    private boolean permanent;

    /**
     * Constructs a new vector data node for the given feature type.
     *
     * @param name        The node name.
     * @param featureType The feature type.
     * @throws IllegalArgumentException if the given name is not a valid node identifier
     */
    public VectorDataNode(String name, SimpleFeatureType featureType) {
        this(name, new DefaultFeatureCollection(name, featureType), getPlacemarkDescriptor(featureType));
    }

    /**
     * Constructs a new vector data node for the given feature collection.
     *
     * @param name              The node name.
     * @param featureCollection A feature collection. A copy of this collection will be used. This collection instance is not modified.
     * @throws IllegalArgumentException if the given name is not a valid node identifier
     */
    public VectorDataNode(String name, FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
        this(name, featureCollection, getPlacemarkDescriptor(featureCollection.getSchema()));
    }

    /**
     * Constructs a new vector data node for the given feature collection and placemark descriptor.
     *
     * @param name                The node name.
     * @param featureCollection   A feature collection. A copy of this collection will be used. This collection instance is not modified.
     * @param placemarkDescriptor The placemark descriptor
     * @throws IllegalArgumentException if the given name is not a valid node identifier
     */
    public VectorDataNode(String name, FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, PlacemarkDescriptor placemarkDescriptor) {
        super(name, "");
        this.featureType = featureCollection.getSchema();
        this.featureCollection = new ObservableFeatureCollection(featureCollection);
        this.featureCollection.addListener(this::fireCollectionEvent);
        this.defaultStyleCss = String.format(DEFAULT_STYLE_FORMAT, FILL_COLORS[(fillColorIndex++) % FILL_COLORS.length]);
        this.placemarkDescriptor = placemarkDescriptor;
        Debug.trace(String.format("VectorDataNode created: name=%s, featureType.typeName=%s, placemarkDescriptor.class=%s",
                                  name, featureType.getTypeName(), placemarkDescriptor.getClass()));
    }

    private void fireCollectionEvent(ObservableFeatureCollection.EVENT_TYPE type, SimpleFeature[] changedFeatures) {
        if (type == ObservableFeatureCollection.EVENT_TYPE.ADDED) {
            _fireFeaturesAdded(changedFeatures);
        } else if (type == ObservableFeatureCollection.EVENT_TYPE.REMOVED) {
            _fireFeaturesRemoved(changedFeatures);
        }
    }

    /**
     * Called when this node is added to or removed from a product.
     * Overridden in order to create placemarks for features that are still
     * without a placemark counterpart.
     *
     * @param owner the new owner
     */
    @Override
    public synchronized void setOwner(ProductNode owner) {
        super.setOwner(owner);
        if (getProduct() != null) {
            updateFeatureCollectionByPlacemarkGroup();
        }
    }

    public PlacemarkDescriptor getPlacemarkDescriptor() {
        return placemarkDescriptor;
    }

    public PlacemarkGroup getPlacemarkGroup() {
        if (placemarkGroup == null) {
            synchronized (this) {
                if (placemarkGroup == null) {
                    placemarkGroup = new PlacemarkGroup(getProduct(), getName(), this);
                }
            }
        }
        return placemarkGroup;
    }

    @Override
    public synchronized void setModified(boolean modified) {
        super.setModified(modified);
        if (placemarkGroup != null) {
            placemarkGroup.setModified(modified);
        }
    }

    /**
     * @return The feature type (= feature source schema).
     */
    public SimpleFeatureType getFeatureType() {
        return featureType;
    }

    /**
     * @return The feature collection.
     */
    public DefaultFeatureCollection getFeatureCollection() {
        return featureCollection;
    }

    /**
     * Gets the bounding box for the features in this feature collection.
     *
     * @return the envelope of the geometries contained by this feature
     *         collection.
     */
    public ReferencedEnvelope getEnvelope() {
        return featureCollection.getBounds();
    }


    @Override
    public long getRawStorageSize(ProductSubsetDef subsetDef) {
        return featureType.getAttributeCount() * featureCollection.size() * 256;
    }

    public String getDefaultStyleCss() {
        return defaultStyleCss;
    }

    public void setDefaultStyleCss(String defaultStyleCss) {
        Assert.notNull(this.defaultStyleCss, PROPERTY_NAME_DEFAULT_STYLE_CSS);
        if (!ObjectUtils.equalObjects(this.defaultStyleCss, defaultStyleCss)) {
            String oldValue = this.defaultStyleCss;
            this.defaultStyleCss = defaultStyleCss;
            fireProductNodeChanged(PROPERTY_NAME_DEFAULT_STYLE_CSS, oldValue, this.defaultStyleCss);
        }
    }

    // preliminary API, better use Map<String, Object> getStyleProperties() ?
    public String getStyleCss() {
        return styleCss;
    }

    // preliminary API, better use setStyleProperties(Map<String, Object> props) ?
    public void setStyleCss(String styleCss) {
        if (!ObjectUtils.equalObjects(this.styleCss, styleCss)) {
            String oldValue = this.styleCss;
            this.styleCss = styleCss;
            fireProductNodeChanged(PROPERTY_NAME_STYLE_CSS, oldValue, this.styleCss);
        }
    }

    @Override
    public void acceptVisitor(ProductVisitor visitor) {
        if (placemarkGroup != null) {
            placemarkGroup.acceptVisitor(visitor);
        }
        visitor.visit(this);
    }

    /**
     * Internal API. Don't use.
     * @return If true, prevents this node from being removed.
     */
    public boolean isPermanent() {
        return permanent;
    }

    /**
     * Internal API. Don't use.
     * @param permanent If true, prevents this node from being removed.
     */
    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }


    private void updateFeatureCollectionByPlacemarkGroup() {
        try (FeatureIterator<SimpleFeature> iterator = featureCollection.features()) {
            while (iterator.hasNext()) {
                generatePlacemarkForFeature(iterator.next());
            }
        }
    }

    private void generatePlacemarkForFeature(SimpleFeature feature) {
        final Placemark placemark = getPlacemarkGroup().getPlacemark(feature);
        if (placemark == null) {
            placemarkGroup.add(placemarkDescriptor.createPlacemark(feature));
        }
    }

    private static PlacemarkDescriptor getPlacemarkDescriptor(final SimpleFeatureType featureType) {
        PlacemarkDescriptorRegistry registry = PlacemarkDescriptorRegistry.getInstance();
        PlacemarkDescriptor placemarkDescriptor = registry.getPlacemarkDescriptor(featureType);
        if (placemarkDescriptor == null) {
            return new GenericPlacemarkDescriptor(featureType);
        }
        return placemarkDescriptor;
    }

    private void _fireFeaturesAdded(SimpleFeature[] features) {
        fireProductNodeChanged(PROPERTY_NAME_FEATURE_COLLECTION, null, features);
    }

    private void _fireFeaturesRemoved(SimpleFeature[] features) {
        fireProductNodeChanged(PROPERTY_NAME_FEATURE_COLLECTION, features, null);
    }

    private void _fireFeaturesChanged(SimpleFeature[] features) {
        fireProductNodeChanged(PROPERTY_NAME_FEATURE_COLLECTION, features, features);
    }

    /////////////////////////////////////////////////////////////////////////
    // Deprecated API
    /////////////////////////////////////////////////////////////////////////

    /**
     * @return true if the feature type's name starts with "org.esa.snap."
     * @deprecated Since BEAM 4.10. No use.
     */
    @Deprecated
    public boolean isInternalNode() {
        return getFeatureType().getTypeName().startsWith("org.esa.snap.");
    }

    /**
     * @deprecated since BEAM 4.10, use getDefaultStyleCss()
     */
    @SuppressWarnings({"JavaDoc"})
    @Deprecated
    public String getDefaultCSS() {
        return getDefaultStyleCss();
    }

    /**
     * @deprecated since BEAM 4.10, use setDefaultStyleCss()
     */
    @SuppressWarnings({"JavaDoc"})
    @Deprecated
    public void setDefaultCSS(String defaultCSS) {
        setDefaultStyleCss(defaultCSS);
    }

    /**
     * Informs clients which have registered a {@link ProductNodeListener} with the {@link Product}
     * containing this {@link VectorDataNode}, that one or more OpenGIS {@code SimpleFeature}s have
     * been added to the underlying {@code FeatureCollection}.
     * <p>
     * The method fires a product node property change event, where the {@code propertyName}
     * is {@link #PROPERTY_NAME_FEATURE_COLLECTION}, the {@code oldValue} is {@code null}, and
     * the {@code newValue} is the array of features added.
     *
     * @param features The feature(s) added.
     * @deprecated since 6.0, method is public by accident, should only be used internally
     */
    public void fireFeaturesAdded(SimpleFeature... features) {
        _fireFeaturesAdded(features);
    }

    /**
     * Informs clients which have registered a {@link ProductNodeListener} with the {@link Product}
     * containing this {@link VectorDataNode}, that one or more OpenGIS {@code SimpleFeature}s have
     * been removed from the underlying {@code FeatureCollection}.
     * <p>
     * The method fires a product node property change event, where the {@code propertyName}
     * is {@link #PROPERTY_NAME_FEATURE_COLLECTION}, the {@code oldValue} is the array of features
     * removed, and the {@code newValue} is {@code null}.
     *
     * @param features The feature(s) removed.
     * @deprecated since 6.0, method is public by accident, should only be used internally
     */
    public void fireFeaturesRemoved(SimpleFeature... features) {
        _fireFeaturesRemoved(features);
    }

    /**
     * Informs clients which have registered a {@link ProductNodeListener} with the {@link Product}
     * containing this {@link VectorDataNode}, that one or more OpenGIS {@code SimpleFeature}s from
     * from the underlying {@code FeatureCollection} have been changed.
     * <p>
     * The method fires a product node property change event, where the {@code propertyName}
     * is {@link #PROPERTY_NAME_FEATURE_COLLECTION}, and both {@code oldValue} and {@code newValue}
     * are the same array of features changed.
     *
     * @param features The feature(s) changed.
     * @deprecated since 6.0, method is public by accident, should only be used internally
     */
    public void fireFeaturesChanged(SimpleFeature... features) {
        _fireFeaturesChanged(features);
    }

}

