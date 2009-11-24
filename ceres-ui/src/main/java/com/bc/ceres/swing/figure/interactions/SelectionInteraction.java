package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.undo.RestorableEdit;
import com.bc.ceres.swing.figure.AbstractInteraction;
import com.bc.ceres.swing.figure.Handle;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.grender.AdjustableView;
import com.bc.ceres.grender.Viewport;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Component;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.event.MouseEvent;

// todo - this Interaction should not be restricted to figure contexts, is the inner Tool interface the solution?
// todo - remove dependency to com.bc.ceres.swing.RestorableEdit

public class SelectionInteraction extends AbstractInteraction {

    private final Tool TOOL_SELECT_POINT = new SelectPointTool();
    private final Tool TOOL_SELECT_RECTANGLE = new SelectRectangleTool();
    private final Tool TOOL_MOVE_SELECTION = new MoveSelectionTool();
    private final Tool TOOL_MOVE_HANDLE = new MoveHandleTool();

    private boolean canceled;
    private Point referencePoint;
    private Object figureMemento;
    private Tool tool;

    public SelectionInteraction() {
        tool = new NullTool();
    }

    public void setSelectionRectangle(Rectangle rectangle) {
        getFigureEditor().setSelectionRectangle(rectangle);
    }

    public Rectangle getSelectionRectangle() {
        return getFigureEditor().getSelectionRectangle();
    }

    @Override
    public void cancel() {
        if (!canceled) {
            canceled = true;
            if (figureMemento != null) {
                getFigureEditor().getFigureSelection().setMemento(figureMemento);
                figureMemento = null;
            }
            getFigureEditor().getFigureSelection().removeFigures();
            super.cancel();
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        referencePoint = event.getPoint();
        canceled = false;
        tool = TOOL_SELECT_POINT;
        figureMemento = null;
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (!canceled) {
            if (tool == TOOL_SELECT_POINT) {
                if (selectHandle(event)) {
                    tool = TOOL_MOVE_HANDLE;
                } else {
                    if (isMouseOverSelection(event)) {
                        tool = TOOL_MOVE_SELECTION;
                    } else {
                        tool = TOOL_SELECT_RECTANGLE;
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
        if (cursor == null && getSelectionRectangle() != null) {
            cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        if (cursor == null) {
            cursor = Cursor.getDefaultCursor();
        }
        getFigureEditor().setCursor(cursor);
    }

    private static Viewport getViewport(MouseEvent event) {
        Component component = event.getComponent();
        if (component instanceof AdjustableView) {
            AdjustableView adjustableView = (AdjustableView) component;
            return adjustableView.getViewport();
        }
        return null;
    }

    private static AffineTransform v2m(MouseEvent event) {
        Viewport viewport = getViewport(event);
        if (viewport != null) {
            return viewport.getViewToModelTransform();
        }
        return new AffineTransform();
    }

    private static AffineTransform m2v(MouseEvent event) {
        Viewport viewport = getViewport(event);
        if (viewport != null) {
            return viewport.getModelToViewTransform();
        }
        return new AffineTransform();
    }

    private static Point2D toModelPoint(MouseEvent event) {
        return toModelPoint(event.getPoint(), event);
    }

    private static Point2D toModelPoint(Point2D point, MouseEvent event) {
        return v2m(event).transform(point, null);
    }

    private Point2D.Double toModelDelta(MouseEvent event) {
        Point2D.Double p = new Point2D.Double(event.getX() - referencePoint.x,
                                              event.getY() - referencePoint.y);
        AffineTransform transform = v2m(event);
        transform.deltaTransform(p, p);
        return p;
    }

    private void dragFigure(Figure figure, MouseEvent event) {
        Point2D.Double p = toModelDelta(event);
        figure.move(p.getX(), p.getY());
        referencePoint = event.getPoint();
    }

    private boolean isMouseOverSelection(MouseEvent event) {
        return getFigureEditor().getFigureSelection().contains(toModelPoint(event));
    }

    private Figure findFigure(MouseEvent event) {
        return getFigureEditor().getFigureCollection().getFigure(toModelPoint(event));
    }

    private Handle findHandle(MouseEvent event) {
        for (Handle handle : getFigureEditor().getFigureSelection().getHandles()) {
            if (handle.isSelectable()) {
                Point2D p = handle.getLocation();
                AffineTransform m2v = m2v(event);
                m2v.transform(p, p);
                p = new Point2D.Double(event.getX() - p.getX(),
                                       event.getY() - p.getY());
                if (handle.contains(p)) {
                    return handle;
                }
            }
        }
        return null;
    }

    private boolean selectHandle(MouseEvent event) {
        Handle handle = findHandle(event);
        if (handle != null) {
            getFigureEditor().getFigureSelection().setSelectedHandle(handle);
            return true;
        }
        return false;
    }

    // todo - Tool is a helper, it may later be replaced by an Interaction delegate
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
            figureMemento = getFigureEditor().getFigureSelection().createMemento();
        }

        @Override
        public void drag(MouseEvent event) {
            dragFigure(getFigureEditor().getFigureSelection(), event);
        }

        @Override
        public void end(MouseEvent event) {
            getFigureEditor().postEdit(new RestorableEdit("Move Figure", getFigureEditor().getFigureSelection(), figureMemento));
        }
    }

    private class MoveHandleTool implements Tool {
        @Override
        public void start(MouseEvent event) {
            figureMemento = getFigureEditor().getFigureSelection().createMemento();
        }

        @Override
        public void drag(MouseEvent event) {
            final Handle selectedHandle = getFigureEditor().getFigureSelection().getSelectedHandle();
            dragFigure(selectedHandle, event);
        }

        @Override
        public void end(MouseEvent event) {
            // Handles may have been moved, selection no longer required
            getFigureEditor().getFigureSelection().setSelectedHandle(null);
            getFigureEditor().postEdit(new RestorableEdit("Change Figure Shape", getFigureEditor().getFigureSelection(), figureMemento));
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
            if (clickedFigure == null) {
                // Nothing clicked, thus clear selection.
                getFigureEditor().getFigureSelection().removeFigures();
            } else if (getFigureEditor().getFigureSelection().getFigureCount() == 0) {
                // If figure clicked and current selection is empty then select this figure at first selection level.
                getFigureEditor().getFigureSelection().addFigure(clickedFigure);
                getFigureEditor().getFigureSelection().setSelectionLevel(1);
            } else if (getFigureEditor().getFigureSelection().getFigureCount() == 1) {
                // If figure clicked and we already have a single figure selected.
                if (getFigureEditor().getFigureSelection().contains(clickedFigure)) {
                    // If the clicked figure is the currently selected figure, then increment selection level.
                    int selectionLevel = getFigureEditor().getFigureSelection().getSelectionLevel() + 1;
                    if (selectionLevel > clickedFigure.getMaxSelectionLevel()) {
                        selectionLevel = 0;
                    }
                    getFigureEditor().getFigureSelection().setSelectionLevel(selectionLevel);
                } else {
                    // If the clicked figure is NOT the currently selected figure, then
                    // if CTRL down add the clicked figure to the selection,
                    // otherwise clicked figure is new selection.
                    if (!event.isControlDown()) {
                        getFigureEditor().getFigureSelection().removeFigures();
                    }
                    getFigureEditor().getFigureSelection().addFigure(clickedFigure);
                    getFigureEditor().getFigureSelection().setSelectionLevel(1);
                }
            } else if (getFigureEditor().getFigureSelection().getFigureCount() >= 2) {
                // If figure clicked and we already have more than one figure selected.
                if (getFigureEditor().getFigureSelection().contains(clickedFigure)) {
                    // If the clicked figure is a currently selected figure, then
                    // if CTRL down, we deleselct clicked figure, otherwise do nothing.
                    if (event.isControlDown()) {
                        getFigureEditor().getFigureSelection().removeFigure(clickedFigure);
                    }
                } else {
                    // If the clicked figure is NOT a currently selected figure, then
                    // if CTRL down we add clicked figure to selection, otherwise
                    // the clicked figure is the only selected one.
                    if (event.isControlDown()) {
                        getFigureEditor().getFigureSelection().addFigure(clickedFigure);
                    } else {
                        getFigureEditor().getFigureSelection().removeFigures();
                        getFigureEditor().getFigureSelection().addFigure(clickedFigure);
                    }
                }
                // Multple selection shall always at selection level 1.
                getFigureEditor().getFigureSelection().setSelectionLevel(1);
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
            setSelectionRectangle(new Rectangle(x, y, width, height));
        }

        @Override
        public void end(MouseEvent event) {
            if (getSelectionRectangle() != null) {
                AffineTransform transform = v2m(event);
                Shape shape = transform.createTransformedShape(getSelectionRectangle());

                final Figure[] figures = getFigureEditor().getFigureCollection().getFigures(shape);
                if (figures.length > 0) {
                    getFigureEditor().getFigureSelection().addFigures(figures);
                    getFigureEditor().getFigureSelection().setSelectionLevel(1);
                }
                setSelectionRectangle(null);
            }
        }
    }


}
