package org.esa.beam.visat.actions;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.swing.figure.AbstractInteractorInterceptor;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.interactions.SelectionInteractor;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.VectorDataFigureEditor;
import org.esa.beam.framework.ui.product.VectorDataLayer;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public class SelectionInteractorInterceptor extends AbstractInteractorInterceptor {

    @Override
    public void interactionStopped(Interactor interactor, InputEvent inputEvent) {
        if (interactor instanceof SelectionInteractor) {
            final SelectionInteractor selectionInteractor = (SelectionInteractor) interactor;
            FigureEditor editor = selectionInteractor.getFigureEditor(inputEvent);
            if (editor instanceof VectorDataFigureEditor && inputEvent instanceof MouseEvent) {
                final VectorDataFigureEditor figureEditor = (VectorDataFigureEditor) editor;
                final MouseEvent mouseEvent = (MouseEvent) inputEvent;
                // preferring rectangle selection on current selected layer
                if (!isSelectionOnCurrentLayer(figureEditor, mouseEvent)) {
                    findLayerForSelection(figureEditor, mouseEvent);
                }
            }
        }
    }

    private boolean isSelectionOnCurrentLayer(VectorDataFigureEditor figureEditor, MouseEvent mouseEvent) {
        Layer selectedLayer = figureEditor.getProductSceneView().getSelectedLayer();
        if (selectedLayer instanceof VectorDataLayer) {
            VectorDataLayer vectorDataLayer = (VectorDataLayer) selectedLayer;
            return getFigures(vectorDataLayer, figureEditor, mouseEvent).length > 0;
        }
        return false;
    }

    private static Figure[] getFigures(VectorDataLayer vectorDataLayer, VectorDataFigureEditor figureEditor, MouseEvent mouseEvent) {
        Viewport viewport = figureEditor.getViewport();
        AffineTransform v2mTransform = viewport.getViewToModelTransform();
        Rectangle rectangle = figureEditor.getSelectionRectangle();
        Figure[] figures = new Figure[0];
        if (rectangle != null) {
            Shape shape = v2mTransform.createTransformedShape(rectangle);
            figures = vectorDataLayer.getFigureCollection().getFigures(shape);
        } else {
            v2mTransform.transform(mouseEvent.getPoint(), null);
            Point2D modelPoint = v2mTransform.transform(mouseEvent.getPoint(), null);
            AffineTransform m2vTransform = viewport.getModelToViewTransform();
            Figure figure = vectorDataLayer.getFigureCollection().getFigure(modelPoint, m2vTransform);
            if (figure != null) {
                figures = new Figure[]{figure};
            }
        }
        return figures;
    }

    private void findLayerForSelection(VectorDataFigureEditor figureEditor, MouseEvent mouseEvent) {
        LayerWithNearFigureFilter figureFilter = new LayerWithNearFigureFilter(figureEditor, mouseEvent);
        final ProductSceneView sceneView = figureEditor.getProductSceneView();
        final Layer rootLayer = sceneView.getRootLayer();
        selectLayer(rootLayer, figureFilter);
    }

    private void selectLayer(Layer rootLayer, LayerWithNearFigureFilter figureFilter) {
        LayerUtils.getChildLayer(rootLayer, LayerUtils.SearchMode.DEEP, figureFilter);
    }

    private static class LayerWithNearFigureFilter implements LayerFilter {
        private VectorDataFigureEditor figureEditor;
        private final MouseEvent mouseEvent;

        public LayerWithNearFigureFilter(VectorDataFigureEditor figureEditor, MouseEvent mouseEvent) {
            this.figureEditor = figureEditor;
            this.mouseEvent = mouseEvent;
        }

        @Override
        public boolean accept(Layer layer) {
            if (layer instanceof VectorDataLayer) {
                VectorDataLayer vectorDataLayer = (VectorDataLayer) layer;
                final Figure[] figures = getFigures(vectorDataLayer, figureEditor, mouseEvent);
                if (figures.length > 0) {
                    ProductSceneView sceneView = figureEditor.getProductSceneView();
                    // selectVectorDataLayer changes selectionRectangle
                    // to preserve selection get rectangle first and set it afterwards
                    Rectangle selectionRectangle = figureEditor.getSelectionRectangle();
                    sceneView.selectVectorDataLayer(vectorDataLayer.getVectorDataNode());
                    if (selectionRectangle != null) {
                        figureEditor.setSelectionRectangle(selectionRectangle);
                    }
                    return true;
                }
            }
            return false;
        }

    }
}
