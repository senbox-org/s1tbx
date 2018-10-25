package org.esa.snap.core.util;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The class does not belong to the public API, only for internal Use.
 *
 * 
 *
 * @author Marco Peters
 */
public class ObservableFeatureCollection extends DefaultFeatureCollection {

    private List<Listener> listeners;

    // bounds not accessible in parent
    private ReferencedEnvelope bounds = null;

    public ObservableFeatureCollection(FeatureCollection<SimpleFeatureType, SimpleFeature> collection) {
        super(collection);
    }

    public final void addListener(Listener listener) {
        if (listeners == null) {
            listeners = Collections.synchronizedList(new ArrayList<>());
        }
        listeners.add(listener);
    }

    public final void removeListener(Listener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    private void fireChange(EVENT_TYPE type, Collection coll) {
        SimpleFeature[] features = new SimpleFeature[coll.size()];
        features = (SimpleFeature[]) coll.toArray(features);
        fireChange(type, features);
    }

    private void fireChange(EVENT_TYPE type, SimpleFeature... features) {
        forceBoundsRecalculation();
        if (listeners != null) {
            for (Listener listener : listeners) {
                listener.changed(type, features);
            }
        }
    }

    public enum EVENT_TYPE {
        ADDED,
        REMOVED
    }

    public interface Listener {
        void changed(EVENT_TYPE type, SimpleFeature... features);
    }

    @Override
    protected boolean add(SimpleFeature feature, boolean fire) {
        boolean changed = super.add(feature, fire);
        if (changed && fire) {
            fireChange(EVENT_TYPE.ADDED, feature);
        }
        return changed;
    }

    @Override
    public boolean addAll(Collection<? extends SimpleFeature> collection) {
        boolean changed = super.addAll(collection);
        if (changed) {
            List<SimpleFeature> featuresAdded = new ArrayList<>(collection.size());
            for (SimpleFeature simpleFeature : collection) {
                if (contains(simpleFeature)) {
                    featuresAdded.add(simpleFeature);
                }
            }
            fireChange(EVENT_TYPE.ADDED, featuresAdded);
        }
        return changed;
    }

    @Override
    public boolean addAll(FeatureCollection<?, ?> collection) {
        boolean changed = super.addAll(collection);
        if (changed) {
            try (FeatureIterator<?> features = collection.features()) {
                List<SimpleFeature> featuresAdded = new ArrayList<>(collection.size());
                while (features.hasNext()) {
                    SimpleFeature feature = (SimpleFeature) features.next();
                    if (contains(feature)) {
                        featuresAdded.add(feature);
                    }
                }
                fireChange(EVENT_TYPE.ADDED, featuresAdded);
            }
        }

        return changed;
    }

    @Override
    public void clear() {
        if (isEmpty()) {
            return;
        }

        SimpleFeature[] oldFeatures = toArray(new SimpleFeature[size()]);
        super.clear();

        fireChange(EVENT_TYPE.REMOVED, oldFeatures);
    }

    @Override
    public boolean remove(Object o) {
        boolean changed = super.remove(o);
        if (changed) {
            fireChange(EVENT_TYPE.REMOVED, (SimpleFeature) o);
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean changed = super.removeAll(collection);
        Iterator<?> iterator = collection.iterator();
        try {
            if (changed) {
                List<SimpleFeature> featuresRemoved = new ArrayList<>(collection.size());
                while (iterator.hasNext()) {
                    SimpleFeature feature = (SimpleFeature) iterator.next();
                    if (!contains(feature)) {
                        featuresRemoved.add(feature);
                    }
                }
                fireChange(EVENT_TYPE.REMOVED, featuresRemoved);
            }
        } finally {
            if (iterator instanceof FeatureIterator) {
                ((FeatureIterator) iterator).close();
            }
        }

        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        List<SimpleFeature> oldFeatures = Arrays.asList(toArray(new SimpleFeature[0]));
        super.retainAll(collection);
        List<SimpleFeature> currentFeatures = Arrays.asList(toArray(new SimpleFeature[0]));
        List<SimpleFeature> featuresRemoved = new ArrayList<>();
        for (SimpleFeature oldFeature : oldFeatures) {
            if (!currentFeatures.contains(oldFeature)) {
                featuresRemoved.add(oldFeature);
            }
        }
        boolean changed = !featuresRemoved.isEmpty();
        if (changed) {
            fireChange(EVENT_TYPE.REMOVED, featuresRemoved);
        }
        return changed;
    }


    private void forceBoundsRecalculation() {
        bounds = null;
    }

    // Overrides to update bounds variable

    public ReferencedEnvelope getBounds() {
        if (bounds == null) {
            bounds = new ReferencedEnvelope();

            SimpleFeatureIterator iterator = features();
            while (iterator.hasNext()) {
                BoundingBox geomBounds = iterator.next().getBounds();
                if (!geomBounds.isEmpty()) {
                    // IanS - as of 1.3, JTS expandToInclude ignores "null" Envelope
                    // and simply adds the new bounds...
                    // This check ensures this behavior does not occur.
                    bounds.include(geomBounds);
                }
            }
        }
        return bounds;
    }

    @Override
    public Iterator<SimpleFeature> iterator() {
        final Iterator<SimpleFeature> iterator = super.iterator();
        return new Iterator<SimpleFeature>() {

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public SimpleFeature next() {
                return iterator.next();
            }

            public void remove() {
                iterator.remove();
                forceBoundsRecalculation();
            }
        };
    }
}
