package org.esa.beam.framework.datamodel;

import org.opengis.feature.simple.SimpleFeature;

import java.util.Iterator;
import java.util.WeakHashMap;

public class PlacemarkGroup extends ProductNodeGroup<Pin> {

    private final VectorDataNode vectorDataNode;
    private final WeakHashMap<SimpleFeature, Pin> pinMap;
    private final ProductNodeListener listener;

    PlacemarkGroup(Product product, String name, VectorDataNode vectorDataNode) {
        super(product, name, true);
        this.vectorDataNode = vectorDataNode;
        this.pinMap = new WeakHashMap<SimpleFeature, Pin>();
        listener = new VectorDataNodeListener();
        getProduct().addProductNodeListener(listener);
    }

    public VectorDataNode getVectorDataNode() {
        return vectorDataNode;
    }

    public final Pin getPlacemark(SimpleFeature feature) {
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
        if (getProduct() != null) {
            getProduct().removeProductNodeListener(listener);
        }
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

    private class VectorDataNodeListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getSource() == vectorDataNode) {
                if (event.getPropertyName().equals(VectorDataNode.PROPERTY_NAME_FEATURE_COLLECTION)) {
                    final SimpleFeature[] oldFeatures = (SimpleFeature[]) event.getOldValue();
                    final SimpleFeature[] newFeatures = (SimpleFeature[]) event.getNewValue();

                    if (oldFeatures == null) { // features added?
                        for (SimpleFeature feature : newFeatures) {
                            final Pin pin = pinMap.get(feature);
                            if (pin == null) {
                                // Only call add() if we don't have the pin already
                                _add(new Pin(feature));
                            }
                        }
                    } else if (newFeatures == null) { // features removed?
                        for (SimpleFeature feature : oldFeatures) {
                            final Pin pin = pinMap.get(feature);
                            if (pin != null) {
                                // Only call add() if we don't have the pin already
                                _remove(pin);
                            }
                        }
                    } else { // features changed
                        for (SimpleFeature feature : newFeatures) {
                            final Pin pin = pinMap.get(feature);
                            if (pin != null) {
                                pin.updatePixelPos();
                            }
                        }
                    }
                }
            }
        }
    }
}
