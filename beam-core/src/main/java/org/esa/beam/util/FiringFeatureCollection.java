package org.esa.beam.util;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.CollectionEvent;
import org.geotools.feature.CollectionListener;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.Collection;
import java.util.List;

public class FiringFeatureCollection extends ListFeatureCollection {

    public FiringFeatureCollection(SimpleFeatureType schema) {
        super(schema);
    }

    public FiringFeatureCollection(SimpleFeatureType schema, List<SimpleFeature> list) {
        super(schema, list);
    }

    @Override
    public boolean add(SimpleFeature f) {
        final boolean added = super.add(f);
        fireCollectionChanged(new SimpleFeature[]{f}, CollectionEvent.FEATURES_ADDED);
        return added;
    }

    @Override
    public boolean addAll(Collection<? extends SimpleFeature> c) {
        final boolean added = super.addAll(c);
        fireCollectionChanged(c.toArray(new SimpleFeature[c.size()]), CollectionEvent.FEATURES_ADDED);
        return added;
    }

    @Override
    public boolean addAll(FeatureCollection<? extends SimpleFeatureType, ? extends SimpleFeature> c) {
        final boolean added = super.addAll(c);
        fireCollectionChanged(c.toArray(new SimpleFeature[c.size()]), CollectionEvent.FEATURES_ADDED);
        return added;
    }

    @Override
    public void clear() {
        super.clear();
        fireCollectionChanged(new SimpleFeature[]{}, CollectionEvent.FEATURES_REMOVED);
    }

    @Override
    public boolean remove(Object o) {
        final boolean remove = super.remove(o);
        fireCollectionChanged(new SimpleFeature[]{(SimpleFeature) o}, CollectionEvent.FEATURES_REMOVED);
        return remove;
    }

    private void fireCollectionChanged(SimpleFeature[] features, int type) {
        for (CollectionListener listener : listeners) {
            listener.collectionChanged(new CollectionEvent(this, features, type));
        }
    }

}
