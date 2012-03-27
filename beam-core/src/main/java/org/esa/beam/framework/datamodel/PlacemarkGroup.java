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

import org.opengis.feature.simple.SimpleFeature;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PlacemarkGroup extends ProductNodeGroup<Placemark> {

    private final VectorDataNode vectorDataNode;
    private final Map<SimpleFeature, Placemark> placemarkMap;
    private final ProductNodeListener listener;

    PlacemarkGroup(Product product, String name, VectorDataNode vectorDataNode) {
        super(product, name, true);
        this.vectorDataNode = vectorDataNode;
        this.placemarkMap = Collections.synchronizedMap(new HashMap<SimpleFeature, Placemark>());
        listener = new VectorDataNodeListener();
        getProduct().addProductNodeListener(listener);
    }

    public VectorDataNode getVectorDataNode() {
        return vectorDataNode;
    }

    public final Placemark getPlacemark(SimpleFeature feature) {
        return placemarkMap.get(feature);
    }

    @Override
    public boolean add(Placemark placemark) {
        final boolean added = _add(placemark);
        if (added) {
            addToVectorData(placemark);
        }
        return added;
    }

    @Override
    public void add(int index, Placemark placemark) {
        _add(index, placemark);
        addToVectorData(placemark);
    }

    @Override
    public boolean remove(Placemark placemark) {
        final boolean removed = _remove(placemark);
        if (removed) {
            removeFromVectorData(placemark);
        }
        return removed;
    }

    @Override
    public void dispose() {
        if (getProduct() != null) {
            getProduct().removeProductNodeListener(listener);
        }
        placemarkMap.clear();
        super.dispose();
    }

    private boolean _add(Placemark placemark) {
        final boolean added = super.add(placemark);
        if (added) {
            placemarkMap.put(placemark.getFeature(), placemark);
        }
        return added;
    }

    private void _add(int index, Placemark placemark) {
        super.add(index, placemark);
        placemarkMap.put(placemark.getFeature(), placemark);
    }

    private boolean _remove(Placemark placemark) {
        final boolean removed = super.remove(placemark);
        if (removed) {
            placemarkMap.remove(placemark.getFeature());
        }
        return removed;
    }

    private void addToVectorData(final Placemark placemark) {
        if (!vectorDataNode.getFeatureCollection().contains(placemark.getFeature())) {
            vectorDataNode.getFeatureCollection().add(placemark.getFeature());
        }
    }

    private void removeFromVectorData(Placemark placemark) {
        vectorDataNode.getFeatureCollection().remove(placemark.getFeature());
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
                            final Placemark placemark = placemarkMap.get(feature);
                            if (placemark == null) {
                                // Only call add() if we don't have the pin already
                                _add(vectorDataNode.getPlacemarkDescriptor().createPlacemark(feature));
                            }
                        }
                    } else if (newFeatures == null) { // features removed?
                        for (SimpleFeature feature : oldFeatures) {
                            final Placemark placemark = placemarkMap.get(feature);
                            if (placemark != null) {
                                // Only call add() if we don't have the pin already
                                _remove(placemark);
                            }
                        }
                    } else { // features changed
                        for (SimpleFeature feature : newFeatures) {
                            final Placemark placemark = placemarkMap.get(feature);
                            if (placemark != null) {
                                placemark.updatePositions();
                            }
                        }
                    }
                }
            }
        }
    }
}
