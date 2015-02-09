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

package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ObjectUtils;
import org.geotools.feature.CollectionEvent;
import org.geotools.feature.CollectionListener;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;

/**
 * A container which allows to store vector data in the BEAM product model.
 * <p/>
 * This is a preliminary API under construction for BEAM 4.7. Not intended for public use.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
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
    static int fillColorIndex;


    private final SimpleFeatureType featureType;
    private final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
    private final CollectionListener featureCollectionListener;
    private final PlacemarkDescriptor placemarkDescriptor;
    private PlacemarkGroup placemarkGroup;
    private String defaultStyleCss;
    private String styleCss;
    private ReferencedEnvelope bounds;
    private boolean permanent;

    /**
     * Constructs a new vector data node for the given feature collection.
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
     * @param featureCollection A feature collection.
     * @throws IllegalArgumentException if the given name is not a valid node identifier
     */
    public VectorDataNode(String name, FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
        this(name, featureCollection, getPlacemarkDescriptor(featureCollection.getSchema()));
    }

    /**
     * Constructs a new vector data node for the given feature collection.
     *
     * @param name                The node name.
     * @param featureCollection   A feature collection.
     * @param placemarkDescriptor The placemark descriptor
     * @throws IllegalArgumentException if the given name is not a valid node identifier
     */
    public VectorDataNode(String name, FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, PlacemarkDescriptor placemarkDescriptor) {
        super(name, "");
        this.featureType = featureCollection.getSchema();
        this.featureCollection = featureCollection;
        this.featureCollectionListener = new CollectionListener() {
            @Override
            public void collectionChanged(CollectionEvent tce) {
                if (tce.getEventType() == CollectionEvent.FEATURES_ADDED) {
                    fireFeaturesAdded(tce.getFeatures());
                } else if (tce.getEventType() == CollectionEvent.FEATURES_REMOVED) {
                    fireFeaturesRemoved(tce.getFeatures());
                } else if (tce.getEventType() == CollectionEvent.FEATURES_CHANGED) {
                    fireFeaturesChanged(tce.getFeatures());
                }
            }
        };
        this.featureCollection.addListener(featureCollectionListener);
        this.defaultStyleCss = String.format(DEFAULT_STYLE_FORMAT, FILL_COLORS[(fillColorIndex++) % FILL_COLORS.length]);
        this.placemarkDescriptor = placemarkDescriptor;
        Debug.trace(String.format("VectorDataNode created: name=%s, featureType.typeName=%s, placemarkDescriptor.class=%s",
                                  name, featureType.getTypeName(), placemarkDescriptor.getClass()));
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
     * Informs clients which have registered a {@link ProductNodeListener} with the {@link Product}
     * containing this {@link VectorDataNode}, that one or more OpenGIS {@code SimpleFeature}s have
     * been added to the underlying {@code FeatureCollection}.
     * <p/>
     * The method fires a product node property change event, where the {@code propertyName}
     * is {@link #PROPERTY_NAME_FEATURE_COLLECTION}, the {@code oldValue} is {@code null}, and
     * the {@code newValue} is the array of features added.
     *
     * @param features The feature(s) added.
     */
    public void fireFeaturesAdded(SimpleFeature... features) {
        bounds = null;
        fireProductNodeChanged(PROPERTY_NAME_FEATURE_COLLECTION, null, features);
    }

    /**
     * Informs clients which have registered a {@link ProductNodeListener} with the {@link Product}
     * containing this {@link VectorDataNode}, that one or more OpenGIS {@code SimpleFeature}s have
     * been removed from the underlying {@code FeatureCollection}.
     * <p/>
     * The method fires a product node property change event, where the {@code propertyName}
     * is {@link #PROPERTY_NAME_FEATURE_COLLECTION}, the {@code oldValue} is the array of features
     * removed, and the {@code newValue} is {@code null}.
     *
     * @param features The feature(s) removed.
     */
    public void fireFeaturesRemoved(SimpleFeature... features) {
        bounds = null;
        fireProductNodeChanged(PROPERTY_NAME_FEATURE_COLLECTION, features, null);
    }

    /**
     * Informs clients which have registered a {@link ProductNodeListener} with the {@link Product}
     * containing this {@link VectorDataNode}, that one or more OpenGIS {@code SimpleFeature}s from
     * from the underlying {@code FeatureCollection} have been changed.
     * <p/>
     * The method fires a product node property change event, where the {@code propertyName}
     * is {@link #PROPERTY_NAME_FEATURE_COLLECTION}, and both {@code oldValue} and {@code newValue}
     * are the same array of features changed.
     *
     * @param features The feature(s) changed.
     */
    public void fireFeaturesChanged(SimpleFeature... features) {
        bounds = null;
        fireProductNodeChanged(PROPERTY_NAME_FEATURE_COLLECTION, features, features);
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
    public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollection() {
        return featureCollection;
    }

    /**
     * Gets the bounding box for the features in this feature collection.
     *
     * @return the envelope of the geometries contained by this feature
     *         collection.
     */
    public ReferencedEnvelope getEnvelope() {
        if (bounds == null) {
            bounds = new ReferencedEnvelope(featureType.getCoordinateReferenceSystem());

            FeatureIterator<SimpleFeature> iterator = featureCollection.features();
            try {
                while (iterator.hasNext()) {
                    BoundingBox geomBounds = iterator.next().getBounds();
                    if (!geomBounds.isEmpty()) {
                        bounds.include(geomBounds);
                    }
                }
            } finally {
                iterator.close();
            }
        }
        return bounds;
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
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    @Override
    public void dispose() {
        featureCollection.removeListener(featureCollectionListener);
        super.dispose();
    }

    private void updateFeatureCollectionByPlacemarkGroup() {
        final FeatureIterator<SimpleFeature> iterator = featureCollection.features();
        try {
            while (iterator.hasNext()) {
                generatePlacemarkForFeature(iterator.next());
            }
        } finally {
            iterator.close();
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

    /////////////////////////////////////////////////////////////////////////
    // Deprecated API
    /////////////////////////////////////////////////////////////////////////

    /**
     * @return true if the feature type's name starts with "org.esa.beam."
     * @deprecated Since BEAM 4.10. No use.
     */
    @Deprecated
    public boolean isInternalNode() {
        return getFeatureType().getTypeName().startsWith("org.esa.beam.");
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
}

