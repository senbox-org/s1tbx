package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorInteractor;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.figure.Handle;
import com.bc.ceres.swing.figure.support.VertexHandle;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

// todo - this Interactor should not be restricted to figure contexts, is the inner Tool interface the solution?
// todo - remove dependency to com.bc.ceres.swing.RestorableEdit

public class SelectionInteractor extends FigureEditorInteractor {

    private final Tool selectPointTool = new SelectPointTool();
    private final Tool selectRectangleTool = new SelectRectangleTool();
    private final Tool moveSelectionTool = new MoveSelectionTool();
    private final Tool moveHandleTool = new MoveHandleTool();

    private boolean canceled;
    private Point referencePoint;
    private Object figureMemento;
    private Tool tool;

    public SelectionInteractor() {
        tool = new NullTool();
    }

    @Override
    public void cancelInteraction(InputEvent event) {
        if (!canceled) {
            canceled = true;
            FigureEditor figureEditor = getFigureEditor(event);
            if (figureMemento != null) {
                figureEditor.getFigureSelection().setMemento(figureMemento);
                figureMemento = null;
            }
            figureEditor.getFigureSelection().removeAllFigures();
            super.cancelInteraction(event);
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (startInteraction(event)) {
            referencePoint = event.getPoint();
            canceled = false;
            tool = selectPointTool;
            figureMemento = null;
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (!canceled) {
            if (tool == selectPointTool) {
                if (selectHandle(event)) {
                    tool = moveHandleTool;
                } else {
                    if (isMouseOverSelection(event)) {
                        tool = moveSelectionTool;
                    } else {
                        tool = selectRectangleTool;
                    }
                }
                tool.start(event);
            }
            tool.drag(event);
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (!canceled) {
            stopInteraction(event);
            tool.end(event);
            setCursor(event);
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        setCursor(event);
    }

    private void setCursor(MouseEvent event) {
        Cursor cursor = null;
        Handle handle = findHandle(event);
        if (handle != null) {
            cursor = handle.getCursor();
        }
        if (cursor == null && isMouseOverSelection(event)) {
            cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        }

        FigureEditor figureEditor = getFigureEditor(event);
        if (cursor == null && figureEditor.getSelectionRectangle() != null) {
            cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        if (cursor == null) {
            cursor = Cursor.getDefaultCursor();
        }
        figureEditor.getEditorComponent().setCursor(cursor);
    }

    private Point2D.Double toModelDelta(MouseEvent event) {
        Point2D.Double p = new Point2D.Double(event.getX() - referencePoint.x,
                                              event.getY() - referencePoint.y);
        AffineTransform transform = getViewToModelTransform(event);
        transform.deltaTransform(p, p);
        return p;
    }

    private void dragFigure(Figure figure, MouseEvent event) {
        Point2D.Double p = toModelDelta(event);
        figure.move(p.getX(), p.getY());
        referencePoint = event.getPoint();
    }

    private boolean isMouseOverSelection(MouseEvent event) {
        return getFigureEditor(event).getFigureSelection().isCloseTo(toModelPoint(event),
                                                                     getModelToViewTransform(event));
    }

    private Figure findFigure(MouseEvent event) {
        return getFigureEditor(event).getFigureCollection().getFigure(toModelPoint(event),
                                                                      getModelToViewTransform(event));
    }

    private Handle findHandle(MouseEvent event) {
        FigureEditor figureEditor = getFigureEditor(event);
        FigureSelection figureSelection = figureEditor.getFigureSelection();
        Point2D modelPoint = toModelPoint(event);
        AffineTransform m2v = getModelToViewTransform(event);
        for (Handle handle : figureSelection.getHandles()) {
            if (handle.isSelectable()) {
                if (handle.isCloseTo(modelPoint, m2v)) {
                    return handle;
                }
            }
        }
        return null;
    }

    private boolean selectHandle(MouseEvent event) {
        Handle handle = findHandle(event);
        if (handle != null) {
            getFigureEditor(event).getFigureSelection().setSelectedHandle(handle);
            return true;
        }
        return false;
    }

    // todo - Tool is a helper, it may later be replaced by an Interactor delegate
    private interface Tool {
        void start(MouseEvent event);

        void drag(MouseEvent event);

        void end(MouseEvent event);
    }

    private static class NullTool implements Tool {
        @Override
        public void start(MouseEvent event) {
        }

        @Override
        public void drag(MouseEvent event) {
        }

        @Override
        public void end(MouseEvent event) {
        }
    }

    private class MoveSelectionTool implements Tool {
        @Override
        public void start(MouseEvent event) {
            figureMemento = getFigureEditor(event).getFigureSelection().createMemento();
        }

        @Override
        public void drag(MouseEvent event) {
            dragFigure(getFigureEditor(event).getFigureSelection(), event);
        }

        @Override
        public void end(MouseEvent event) {
            FigureEditor figureEditor = getFigureEditor(event);
            figureEditor.changeFigure(figureEditor.getFigureSelection(), figureMemento, "Move Figure");
        }
    }

    private class MoveHandleTool implements Tool {
        @Override
        public void start(MouseEvent event) {
            figureMemento = getFigureEditor(event).getFigureSelection().createMemento();
            if (event.isControlDown()) {
                maybeAddSegment(event);
            }
        }

        @Override
        public void drag(MouseEvent event) {
            final Handle selectedHandle = getFigureEditor(event).getFigureSelection().getSelectedHandle();
            dragFigure(selectedHandle, event);
        }

        @Override
        public void end(MouseEvent event) {
            if (event.isControlDown()) {
               maybeRemoveSegment(event);
            }
            // Handle selection no longer required
            FigureEditor figureEditor = getFigureEditor(event);
            figureEditor.getFigureSelection().setSelectedHandle(null);
            figureEditor.changeFigure(figureEditor.getFigureSelection(), figureMemento, "Change figure shape");
         }

        private void maybeAddSegment(MouseEvent event) {
            FigureSelection figureSelection = getFigureEditor(event).getFigureSelection();
            Handle selectedHandle = figureSelection.getSelectedHandle();
            if (selectedHandle instanceof VertexHandle) {
                VertexHandle selectedVertexHandle = (VertexHandle) selectedHandle;
                int segmentIndex = selectedVertexHandle.getSegmentIndex();
                Figure figure = figureSelection.getFigure(0);
                double[] segment = figure.getSegment(segmentIndex);
                // Need to add some offsets, otherwise (AWT) shapes won't accept new segment
                segment[0] += 0.1;
                segment[1] += 0.1;
                figure.addSegment(segmentIndex, segment);
                VertexHandle newVertexHandle = new VertexHandle(figure, segmentIndex,
                                                                selectedVertexHandle.getNormalStyle(),
                                                                selectedVertexHandle.getSelectedStyle());
                figureSelection.setSelectedHandle(newVertexHandle);
                for (Handle handle : figureSelection.getHandles()) {
                    if (handle instanceof VertexHandle) {
                        VertexHandle vertexHandle = (VertexHandle) handle;
                        if (vertexHandle != newVertexHandle
                                && vertexHandle.getSegmentIndex() >= segmentIndex) {
                            vertexHandle.setSegmentIndex(vertexHandle.getSegmentIndex() + 1);
                        }
                    }
                }
            }
        }

        private void maybeRemoveSegment(MouseEvent event) {
            FigureSelection figureSelection = getFigureEditor(event).getFigureSelection();
            Handle selectedHandle = figureSelection.getSelectedHandle();
            if (selectedHandle instanceof VertexHandle) {
                VertexHandle selectedVertexHandle = (VertexHandle) selectedHandle;
                AffineTransform m2v = getModelToViewTransform(event);
                Point2D p1 = m2v.transform(selectedVertexHandle.getLocation(), null);
                int segmentIndex = selectedVertexHandle.getSegmentIndex();
                Figure figure = figureSelection.getFigure(0);
                for (Handle handle : figureSelection.getHandles()) {
                    if (handle instanceof VertexHandle) {
                        VertexHandle vertexHandle = (VertexHandle) handle;
                        Point2D p2 = m2v.transform(vertexHandle.getLocation(), null);
                        if ((vertexHandle.getSegmentIndex() == segmentIndex - 1
                                || vertexHandle.getSegmentIndex() == segmentIndex + 1)
                                && vertexHandle.getShape().contains(new Point2D.Double(p1.getX()-p2.getX(), p1.getY()-p2.getY()))) {
                            figure.removeSegment(segmentIndex);
                        }
                    }
                }
            }
        }
    }

    private class SelectPointTool implements Tool {
        @Override
        public void start(MouseEvent event) {
            figureMemento = null;
        }

        @Override
        public void drag(MouseEvent event) {
        }

        @Override
        public void end(MouseEvent event) {
            // Check first if user has selected a selectable handle
            if (selectHandle(event)) {
                return;
            }

            // Then check if user has selected a figure
            Figure clickedFigure = findFigure(event);
            FigureEditor figureEditor = getFigureEditor(event);
            if (clickedFigure == null) {
                // Nothing clicked, thus clear selection.
                figureEditor.getFigureSelection().removeAllFigures();
                figureEditor.getFigureSelection().setSelectionStage(0);
            } else if (figureEditor.getFigureSelection().getFigureCount() == 0) {
                // If figure clicked and current selection is empty then select this figure at first selection level.
                figureEditor.getFigureSelection().addFigure(clickedFigure);
                // Single selection starts at selection level 1 (highlighted boundary)
                figureEditor.getFigureSelection().setSelectionStage(1);
            } else if (figureEditor.getFigureSelection().getFigureCount() == 1) {
                // If figure clicked and we already have a single figure selected.
                if (figureEditor.getFigureSelection().contains(clickedFigure)) {
                    // If the clicked figure is the currently selected figure, then increment selection level.
                    int selectionLevel = figureEditor.getFigureSelection().getSelectionStage() + 1;
                    if (selectionLevel > clickedFigure.getMaxSelectionStage()) {
                        selectionLevel = 0;
                    }
                    figureEditor.getFigureSelection().setSelectionStage(selectionLevel);
                } else {
                    // If the clicked figure is NOT the currently selected figure, then
                    // if CTRL down add the clicked figure to the selection,
                    // otherwise clicked figure is new selection.
                    if (event.isControlDown()) {
                        figureEditor.getFigureSelection().addFigure(clickedFigure);
                        // Multiple selection is always at selection level 2 (scale handles + rotation handle).
                        figureEditor.getFigureSelection().setSelectionStage(2);
                    } else {
                        figureEditor.getFigureSelection().removeAllFigures();
                        figureEditor.getFigureSelection().addFigure(clickedFigure);
                        // Single selection starts at selection level 1 (highlighted boundary)
                        figureEditor.getFigureSelection().setSelectionStage(1);
                    }
                }
            } else if (figureEditor.getFigureSelection().getFigureCount() >= 2) {
                // If figure clicked and we already have more than one figure selected.
                if (figureEditor.getFigureSelection().contains(clickedFigure)) {
                    // If the clicked figure is a currently selected figure, then
                    // if CTRL down, we deleselct clicked figure, otherwise do nothing.
                    if (event.isControlDown()) {
                        figureEditor.getFigureSelection().removeFigure(clickedFigure);
                    }
                } else {
                    // If the clicked figure is NOT a currently selected figure, then
                    // if CTRL down we add clicked figure to selection, otherwise
                    // the clicked figure is the only selected one.
                    if (event.isControlDown()) {
                        figureEditor.getFigureSelection().addFigure(clickedFigure);
                    } else {
                        figureEditor.getFigureSelection().removeAllFigures();
                        figureEditor.getFigureSelection().addFigure(clickedFigure);
                    }
                }
                // Multiple selection is always at selection level 2 (scale handles + rotation handle).
                figureEditor.getFigureSelection().setSelectionStage(2);
            }
        }

    }

    private class SelectRectangleTool implements Tool {
        @Override
        public void start(MouseEvent event) {
            figureMemento = null;
        }

        @Override
        public void drag(MouseEvent event) {
            int width = event.getX() - referencePoint.x;
            int height = event.getY() - referencePoint.y;
            int x = referencePoint.x;
            int y = referencePoint.y;
            if (width < 0) {
                width *= -1;
                x -= width;
            }
            if (height < 0) {
                height *= -1;
                y -= height;
            }
            getFigureEditor(event).setSelectionRectangle(new Rectangle(x, y, width, height));
        }

        @Override
        public void end(MouseEvent event) {
            FigureEditor figureEditor = getFigureEditor(event);
            if (figureEditor.getSelectionRectangle() != null) {
                AffineTransform transform = getViewToModelTransform(event);
                Shape shape = transform.createTransformedShape(figureEditor.getSelectionRectangle());
                if (!event.isControlDown()) {
                    figureEditor.getFigureSelection().removeAllFigures();
                }
                final Figure[] figures = figureEditor.getFigureCollection().getFigures(shape);
                figureEditor.getFigureSelection().addFigures(figures);
                if (figureEditor.getFigureSelection().getFigureCount() == 0) {
                    figureEditor.getFigureSelection().setSelectionStage(0);
                } else if (figureEditor.getFigureSelection().getFigureCount() == 1) {
                    figureEditor.getFigureSelection().setSelectionStage(1);
                } else {
                    figureEditor.getFigureSelection().setSelectionStage(2);
                }
                figureEditor.setSelectionRectangle(null);
            }
        }
    }


}
