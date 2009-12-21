package org.esa.beam.framework.datamodel;

import org.geotools.feature.CollectionEvent;
import org.geotools.feature.CollectionListener;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class ChangeEmittingFeatureCollection implements FeatureCollection<SimpleFeatureType, SimpleFeature> {

    private final FeatureCollection<SimpleFeatureType, SimpleFeature> delegate;
    private final ArrayList<CollectionListener> collectionListeners;

    public ChangeEmittingFeatureCollection(FeatureCollection<SimpleFeatureType, SimpleFeature> delegate) {
        this.delegate = delegate;
        this.collectionListeners = new ArrayList<CollectionListener>();
    }

    public CollectionListener[] getListeners() {
        return collectionListeners.toArray(new CollectionListener[collectionListeners.size()]);
    }

    public void fireFeaturesChanged(SimpleFeature... features) {
        CollectionListener[] listeners = getListeners();
        CollectionEvent event = new CollectionEvent(this, features, CollectionEvent.FEATURES_CHANGED);
        for (CollectionListener listener : listeners) {
            listener.collectionChanged(event);
        }
    }

    @Override
    public void addListener(CollectionListener collectionListener) throws NullPointerException {
        delegate.addListener(collectionListener);
        collectionListeners.add(collectionListener);
    }

    @Override
    public void removeListener(CollectionListener collectionListener) throws NullPointerException {
        delegate.removeListener(collectionListener);
        collectionListeners.remove(collectionListener);
    }

    @Override
    public FeatureIterator<SimpleFeature> features() {
        return delegate.features();
    }

    @Override
    public void close(FeatureIterator<SimpleFeature> fFeatureIterator) {
        delegate.close(fFeatureIterator);
    }

    @Override
    public void close(Iterator<SimpleFeature> fIterator) {
        delegate.close(fIterator);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return delegate.getSchema();
    }

    @Override
    public String getID() {
        return delegate.getID();
    }

    @Override
    public void accepts(FeatureVisitor featureVisitor, ProgressListener progressListener) throws IOException {
    }

    @Override
    public FeatureCollection<SimpleFeatureType, SimpleFeature> subCollection(Filter filter) {
        return delegate.subCollection(filter);
    }

    @Override
    public FeatureCollection<SimpleFeatureType, SimpleFeature> sort(SortBy sortBy) {
        return delegate.sort(sortBy);
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return delegate.getBounds();
    }

    @Override
    public Iterator<SimpleFeature> iterator() {
        return delegate.iterator();
    }

    @Override
    @Deprecated
    public void purge() {
        delegate.purge();
    }

    @Override
    public boolean add(SimpleFeature f) {
        return delegate.add(f);
    }

    @Override
    public boolean addAll(Collection<? extends SimpleFeature> fs) {
        return delegate.addAll(fs);
    }

    @Override
    public boolean addAll(FeatureCollection<? extends SimpleFeatureType, ? extends SimpleFeature> featureCollection) {
        return delegate.addAll(featureCollection);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> objects) {
        return delegate.containsAll(objects);
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        return delegate.removeAll(objects);
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        return delegate.retainAll(objects);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <O> O[] toArray(O[] os) {
        return delegate.toArray(os);
    }
}
