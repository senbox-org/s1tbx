package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.undo.RestorableEdit;
import com.bc.ceres.swing.figure.interactions.AbstractInteraction;
import com.bc.ceres.swing.figure.Handle;
import com.bc.ceres.swing.figure.Figure;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
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
                getFigureEditor().getFigureSelection().selectHandle(referencePoint);
                final Handle selectedHandle = getFigureEditor().getFigureSelection().getSelectedHandle();
                if (selectedHandle != null) {
                    tool = TOOL_MOVE_HANDLE;
                } else {
                    if (getFigureEditor().getFigureSelection().contains(referencePoint)) {
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
        for (Handle handle : getFigureEditor().getFigureSelection().getHandles()) {
            if (handle.contains(event.getPoint())) {
                cursor = handle.getCursor();
            }
        }
        if (cursor == null && getFigureEditor().getFigureSelection().contains(event.getPoint())) {
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
            final int dx = event.getX() - referencePoint.x;
            final int dy = event.getY() - referencePoint.y;
            referencePoint = event.getPoint();
            getFigureEditor().getFigureSelection().move(dx, dy);
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
            // If a handle is selected, it is moved by dragging
            selectedHandle.move(event.getX() - referencePoint.x,
                                event.getY() - referencePoint.y);
            referencePoint = event.getPoint();
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
            for (Handle handle : getFigureEditor().getFigureSelection().getHandles()) {
                if (handle.isSelectable() && handle.contains(referencePoint)) {
                    getFigureEditor().getFigureSelection().setSelectedHandle(handle);
                    return;
                }
            }
            // Then check if user has selected a figure
            Figure clickedFigure = getFigureEditor().getFigureCollection().getFigure(referencePoint);
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
                final Figure[] figures = getFigureEditor().getFigureCollection().getFigures(getSelectionRectangle());
                if (figures.length > 0) {
                    getFigureEditor().getFigureSelection().addFigures(figures);
                    getFigureEditor().getFigureSelection().setSelectionLevel(1);
                }
                setSelectionRectangle(null);
            }
        }
    }


}
