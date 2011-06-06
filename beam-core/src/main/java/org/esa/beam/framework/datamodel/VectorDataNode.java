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
import org.geotools.feature.CollectionEvent;
import org.geotools.feature.CollectionListener;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;

import java.util.Iterator;

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
    private final PlacemarkDescriptor placemarkDescriptor;
    private PlacemarkGroup placemarkGroup;
    private final CollectionListener featureCollectionListener;
    private String defaultCSS;
    private ReferencedEnvelope bounds = null;

    /**
     * Constructs a new vector data node for the given feature collection.
     *
     * @param name        The node name.
     * @param featureType The feature type.
     * @throws IllegalArgumentException if the given name is not a valid node identifier
     */
    public VectorDataNode(String name, SimpleFeatureType featureType) {
        this(name, new DefaultFeatureCollection(name, featureType));
    }

    /**
     * Constructs a new vector data node for the given feature collection.
     *
     * @param name              The node name.
     * @param featureCollection A feature collection.
     * @throws IllegalArgumentException if the given name is not a valid node identifier
     */
    public VectorDataNode(String name, FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
        this(name, featureCollection, new GeometryDescriptor());
    }

    public VectorDataNode(String name, SimpleFeatureType featureType, PlacemarkDescriptor placemarkDescriptor) {
        this(name, new DefaultFeatureCollection(name, featureType), placemarkDescriptor);
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
        this.defaultCSS = String.format(DEFAULT_STYLE_FORMAT, FILL_COLORS[(fillColorIndex++) % FILL_COLORS.length]);
        this.placemarkDescriptor = placemarkDescriptor;
    }

    public PlacemarkDescriptor getPlacemarkDescriptor() {
        return placemarkDescriptor;
    }

    public PlacemarkGroup getPlacemarkGroup() {
        if (placemarkGroup == null) {
            placemarkGroup = new PlacemarkGroup(getProduct(), getName(), this);
        }
        return placemarkGroup;
    }

    @Override
    public void setModified(boolean modified) {
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

            for (Iterator<SimpleFeature> i = featureCollection.iterator(); i.hasNext(); ) {
                BoundingBox geomBounds = i.next().getBounds();
                if (!geomBounds.isEmpty()) {
                    bounds.include(geomBounds);
                }
            }
        }
        return bounds;
    }


    @Override
    public long getRawStorageSize(ProductSubsetDef subsetDef) {
        // todo - estimate shapefile size (nf, 10.2009)
        return 0;
    }

    public String getDefaultCSS() {
        return defaultCSS;
    }

    public void setDefaultCSS(String defaultCSS) {
        Assert.notNull(defaultCSS, "defaultCSS");
        this.defaultCSS = defaultCSS;
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

    public boolean isInternalNode() {
        return getFeatureType() == Placemark.getFeatureType();
    }

}

