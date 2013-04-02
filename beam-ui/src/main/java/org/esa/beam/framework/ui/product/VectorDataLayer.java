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
package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureChangeEvent;
import com.bc.ceres.swing.figure.FigureChangeListener;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.support.DefaultFigureCollection;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Debug;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VectorDataLayer extends Layer {

    private static final VectorDataLayerType TYPE = LayerTypeRegistry.getLayerType(VectorDataLayerType.class);
    private VectorDataNode vectorDataNode;
    private final SimpleFeatureFigureFactory figureFactory;
    private FigureCollection figureCollection;
    private VectorDataChangeHandler vectorDataChangeHandler;
    private boolean reactingAgainstFigureChange;

    private static int id;

    public VectorDataLayer(LayerContext ctx, VectorDataNode vectorDataNode) {
        this(TYPE, vectorDataNode, TYPE.createLayerConfig(ctx));
        getConfiguration().setValue(VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA, vectorDataNode.getName());
    }

    protected VectorDataLayer(VectorDataLayerType vectorDataLayerType, VectorDataNode vectorDataNode, PropertySet configuration) {
        super(vectorDataLayerType, configuration);

        setUniqueId();

        this.vectorDataNode = vectorDataNode;
        setName(vectorDataNode.getName());
        figureFactory = new SimpleFeatureFigureFactory(vectorDataNode.getFeatureType());
        figureCollection = new DefaultFigureCollection();
        updateFigureCollection();

        vectorDataChangeHandler = new VectorDataChangeHandler();
        vectorDataNode.getProduct().addProductNodeListener(vectorDataChangeHandler);
        figureCollection.addChangeListener(new FigureChangeHandler());
    }

    private void setUniqueId() {
        setId(VectorDataLayerType.VECTOR_DATA_LAYER_ID_PREFIX + (++id));
    }

    public VectorDataNode getVectorDataNode() {
        return vectorDataNode;
    }

    @Override
    protected void disposeLayer() {
        vectorDataNode.getProduct().removeProductNodeListener(vectorDataChangeHandler);
        vectorDataNode = null;
        super.disposeLayer();
    }

    private void updateFigureCollection() {
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = vectorDataNode.getFeatureCollection();

        Figure[] figures = figureCollection.getFigures();
        Map<SimpleFeature, SimpleFeatureFigure> figureMap = new HashMap<SimpleFeature, SimpleFeatureFigure>();
        for (Figure figure : figures) {
            if (figure instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure simpleFeatureFigure = (SimpleFeatureFigure) figure;
                figureMap.put(simpleFeatureFigure.getSimpleFeature(), simpleFeatureFigure);
            }
        }

        FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();
        while (featureIterator.hasNext()) {
            SimpleFeature simpleFeature = featureIterator.next();
            SimpleFeatureFigure featureFigure = figureMap.get(simpleFeature);
            if (featureFigure != null) {
                figureMap.remove(simpleFeature);
                figureCollection.removeFigure(featureFigure);
            }
            featureFigure = getFigureFactory().createSimpleFeatureFigure(simpleFeature, vectorDataNode.getDefaultStyleCss());
            figureCollection.addFigure(featureFigure);
            featureFigure.forceRegeneration();
        }

        Collection<SimpleFeatureFigure> remainingFigures = figureMap.values();
        figureCollection.removeFigures(remainingFigures.toArray(new Figure[remainingFigures.size()]));

    }

    private void setLayerStyle(String styleCss) {
        // todo - implement me (nf)
        // this method is called if no figure is selected, but the layer editor is showing and users can modify style settings
        Debug.trace("VectorDataLayer.setLayerStyle: styleCss = " + styleCss);
    }

    public SimpleFeatureFigureFactory getFigureFactory() {
        return figureFactory;
    }

    public FigureCollection getFigureCollection() {
        return figureCollection;
    }

    @Override
    protected Rectangle2D getLayerModelBounds() {
        if (figureCollection.getFigureCount() == 0) {
            return null;
        } else {
            return figureCollection.getBounds();
        }
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        figureCollection.draw(rendering);
    }

    private class VectorDataChangeHandler extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getSourceNode() == getVectorDataNode()) {
                Debug.trace("VectorDataLayer$VectorDataChangeHandler.nodeChanged: event = " + event);
                if (ProductNode.PROPERTY_NAME_NAME.equals(event.getPropertyName())) {
                    setName(getVectorDataNode().getName());
                } else if (VectorDataNode.PROPERTY_NAME_STYLE_CSS.equals(event.getPropertyName())) {
                    if (event.getNewValue() != null) {
                        setLayerStyle(event.getNewValue().toString());
                    }
                } else if (VectorDataNode.PROPERTY_NAME_FEATURE_COLLECTION.equals(event.getPropertyName())) {
                    if (!reactingAgainstFigureChange) {
                        updateFigureCollection();
                        // todo - compute changed modelRegion instead of passing null (nf)
                        fireLayerDataChanged(null);
                    }
                }
            } else if (event.getSourceNode() instanceof Placemark) {
                final Placemark sourceNode = (Placemark) event.getSourceNode();
                if (getVectorDataNode().getPlacemarkGroup().contains(sourceNode)) {
                    if (event.getPropertyName().equals(Placemark.PROPERTY_NAME_STYLE_CSS)) {
                        updateFigureCollection();
                    } else if (event.getPropertyName().equals("geometry")) {
                        updateFigureCollection();
                    }
                }
            }
        }
    }

    private class FigureChangeHandler implements FigureChangeListener {

        @Override
        public void figureChanged(FigureChangeEvent event) {
            final Figure sourceFigure = event.getSourceFigure();
            if (sourceFigure instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure featureFigure = (SimpleFeatureFigure) sourceFigure;
                try {
                    final VectorDataNode vectorDataNode = getVectorDataNode();
                    final SimpleFeature simpleFeature = featureFigure.getSimpleFeature();
                    Debug.trace("VectorDataLayer$FigureChangeHandler: vectorDataNode=" + vectorDataNode.getName() +
                                        ", featureType=" + simpleFeature.getFeatureType().getTypeName());
                    reactingAgainstFigureChange = true;
                    vectorDataNode.fireFeaturesChanged(simpleFeature);
                    // todo - compute changed modelRegion instead of passing null (nf)
                    fireLayerDataChanged(null);
                } finally {
                    reactingAgainstFigureChange = false;
                }
            }
        }
    }
}