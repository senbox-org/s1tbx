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

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.support.DefaultFigureEditor;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.util.Debug;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Arrays;
import java.util.List;


public class VectorDataFigureEditor extends DefaultFigureEditor {

    private final ProductSceneView productSceneView;
    private VectorDataNode vectorDataNode;

    public VectorDataFigureEditor(ProductSceneView productSceneView) {
        super(productSceneView.getLayerCanvas(),
              productSceneView.getLayerCanvas().getViewport(),
              productSceneView.getUndoContext(),
              ProductSceneView.NullFigureCollection.INSTANCE,
              null);
        this.productSceneView = productSceneView;
    }

    public ProductSceneView getProductSceneView() {
        return productSceneView;
    }

    public VectorDataNode getVectorDataNode() {
        return vectorDataNode;
    }

    public void vectorDataLayerSelected(VectorDataLayer vectorDataLayer) {
        Debug.trace("VectorDataFigureEditor.vectorDataLayerSelected: " + vectorDataLayer.getName());

        this.vectorDataNode = vectorDataLayer.getVectorDataNode();

        setFigureCollection(vectorDataLayer.getFigureCollection());
        setFigureFactory(vectorDataLayer.getFigureFactory());

        final DefaultFigureStyle style = new DefaultFigureStyle();
        style.fromCssString(vectorDataLayer.getVectorDataNode().getDefaultStyleCss());
        setDefaultLineStyle(style);
        setDefaultPolygonStyle(style);
    }

    @Override
    public void insertFigures(boolean performInsert, Figure... figures) {
        Debug.trace("VectorDataFigureEditor.insertFigures " + performInsert + ", " + figures.length);
        super.insertFigures(performInsert, figures);
        if (vectorDataNode != null) {
            vectorDataNode.getFeatureCollection().addAll(toSimpleFeatureList(figures));
        } else {
            // warn
        }
    }

    @Override
    public void deleteFigures(boolean performDelete, Figure... figures) {
        Debug.trace("VectorDataFigureEditor.deleteFigures " + performDelete + ", " + figures.length);
        super.deleteFigures(performDelete, figures);
        if (vectorDataNode != null) {
            vectorDataNode.getFeatureCollection().removeAll(toSimpleFeatureList(figures));
        } else {
            // warn
        }
    }

    @Override
    public void changeFigure(Figure figure, Object figureMemento, String presentationName) {
        Debug.trace("VectorDataFigureEditor.changeFigure " + figure + ", " + presentationName);
        super.changeFigure(figure, figureMemento, presentationName);
        if (vectorDataNode != null) {
            if (figure instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure featureFigure = (SimpleFeatureFigure) figure;
                vectorDataNode.fireFeaturesChanged(featureFigure.getSimpleFeature());
            }
        } else {
            // warn
        }
    }

    private List<SimpleFeature> toSimpleFeatureList(Figure[] figures) {
        SimpleFeature[] features = new SimpleFeature[figures.length];
        for (int i = 0, figuresLength = figures.length; i < figuresLength; i++) {
            Figure figure = figures[i];
            if (figure instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure simpleFeatureFigure = (SimpleFeatureFigure) figure;
                features[i] = simpleFeatureFigure.getSimpleFeature();
            }
        }
        return Arrays.asList(features);
    }

}
