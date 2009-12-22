package org.esa.beam.framework.datamodel;

import org.opengis.feature.simple.SimpleFeature;
import org.geotools.feature.CollectionListener;
import org.geotools.feature.CollectionEvent;

import java.util.WeakHashMap;
import java.util.Iterator;

class PinGroup extends ProductNodeGroup<Pin> {

    private final VectorDataNode vectorDataNode;
    private final WeakHashMap<SimpleFeature, Pin> pinMap;

    PinGroup(Product product, String name, VectorDataNode vectorDataNode) {
        super(product, name, true);
        this.vectorDataNode = vectorDataNode;
        this.pinMap = new WeakHashMap<SimpleFeature, Pin>();
        vectorDataNode.getFeatureCollection().addListener(new VectorDataFeatureCollectionListener());
    }

    Pin getPin(SimpleFeature feature) {
        return pinMap.get(feature);
    }

    @Override
    public boolean add(Pin pin) {
        final boolean added = _add(pin);
        if (added) {
            addToVectorData(pin);
        }
        return added;
    }

    @Override
    public void add(int index, Pin pin) {
        _add(index, pin);
        addToVectorData(pin);
    }

    @Override
    public boolean remove(Pin pin) {
        final boolean removed = _remove(pin);
        if (removed) {
            removeFromVectorData(pin);
        }
        return removed;
    }

    @Override
    public void dispose() {
        pinMap.clear();
        super.dispose();
    }

    private boolean _add(Pin pin) {
        final boolean added = super.add(pin);
        if (added) {
            pinMap.put(pin.getFeature(), pin);
        }
        return added;
    }

    private void _add(int index, Pin pin) {
        super.add(index, pin);
        pinMap.put(pin.getFeature(), pin);
    }

    private boolean _remove(Pin pin) {
        final boolean removed = super.remove(pin);
        if (removed) {
            pinMap.remove(pin.getFeature());
        }
        return removed;
    }

    private void addToVectorData(final Pin pin) {
        vectorDataNode.getFeatureCollection().add(pin.getFeature());
    }

    private void removeFromVectorData(Pin pin) {
        final Iterator<SimpleFeature> iterator = vectorDataNode.getFeatureCollection().iterator();
        while (iterator.hasNext()) {
            final SimpleFeature feature = iterator.next();
            if (feature == pin.getFeature()) {
                iterator.remove();
                break;
            }
        }
    }

    private class VectorDataFeatureCollectionListener implements CollectionListener {

        @Override
        public void collectionChanged(CollectionEvent tce) {
            if (tce.getEventType() == CollectionEvent.FEATURES_ADDED) {
                final SimpleFeature[] features = tce.getFeatures();
                for (SimpleFeature feature : features) {
                    final Pin pin = getPin(feature);
                    if (pin == null) {
                        // Only call add() if we don't have the pin already
                        _add(new Pin(feature));
                    }
                }
            } else if (tce.getEventType() == CollectionEvent.FEATURES_REMOVED) {
                final SimpleFeature[] features = tce.getFeatures();
                for (SimpleFeature feature : features) {
                    final Pin pin = getPin(feature);
                    if (pin != null) {
                        // Only call remove() if we still have the pin
                        _remove(pin);
                    }
                }
            } else if (tce.getEventType() == CollectionEvent.FEATURES_CHANGED) {
                final SimpleFeature[] features = tce.getFeatures();
                for (SimpleFeature feature : features) {
                    final Pin pin = getPin(feature);
                    if (pin != null) {
                        pin.fireProductNodeChanged(Pin.PROPERTY_NAME_FEATURE, feature, feature);
                    }
                }
            }
        }
    }
}
