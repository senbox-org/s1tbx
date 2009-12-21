package org.esa.beam.framework.ui.product;

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.support.DefaultFigureEditor;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.util.Debug;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.Feature;

import java.util.Arrays;
import java.util.List;


public class ProductSceneViewFigureEditor extends DefaultFigureEditor {
    private ProductSceneView productSceneView;
    private VectorDataNode currentVectorDataNode;

    public ProductSceneViewFigureEditor(ProductSceneView productSceneView) {
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

    public void vectorDataLayerSelected(VectorDataLayer vectorDataLayer) {
        Debug.trace("ProductSceneViewFigureEditor.vectorDataLayerSelected: " + vectorDataLayer.getName());

        this.currentVectorDataNode = vectorDataLayer.getVectorDataNode();

        setFigureCollection(vectorDataLayer.getFigureCollection());
        setFigureFactory(vectorDataLayer.getFigureFactory());

        final DefaultFigureStyle style = new DefaultFigureStyle();
        style.fromCssString(vectorDataLayer.getVectorDataNode().getDefaultCSS());
        setDefaultLineStyle(style);
        setDefaultPolygonStyle(style);
    }

    @Override
    public void insertFigures(boolean performInsert, Figure... figures) {
        Debug.trace("ProductSceneViewFigureEditor.insertFigures " + performInsert + ", " + figures.length);
        super.insertFigures(performInsert, figures);
        if (currentVectorDataNode != null) {
            currentVectorDataNode.getFeatureCollection().addAll(toSimpleFeatureList(figures));
        } else {
            // warn
        }
    }

    @Override
    public void deleteFigures(boolean performDelete, Figure... figures) {
        Debug.trace("ProductSceneViewFigureEditor.deleteFigures " + performDelete + ", " + figures.length);
        super.deleteFigures(performDelete, figures);
        if (currentVectorDataNode != null) {
            currentVectorDataNode.getFeatureCollection().removeAll(toSimpleFeatureList(figures));
        } else {
            // warn
        }
    }

    @Override
    public void changeFigure(Figure figure, Object figureMemento, String presentationName) {
        Debug.trace("ProductSceneViewFigureEditor.changeFigure " + figure + ", " + presentationName);
        super.changeFigure(figure, figureMemento, presentationName);
        if (currentVectorDataNode != null) {
            if (figure instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure featureFigure = (SimpleFeatureFigure) figure;
                SimpleFeature[] features = {featureFigure.getSimpleFeature()};
                currentVectorDataNode.fireFeatureCollectionChanged(features, features);
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
