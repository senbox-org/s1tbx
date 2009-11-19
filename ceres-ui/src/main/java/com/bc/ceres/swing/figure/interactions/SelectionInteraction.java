package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.undo.RestorableEdit;
import com.bc.ceres.swing.figure.interactions.AbstractInteraction;
import com.bc.ceres.figure.support.FigureSelection;
import com.bc.ceres.figure.Handle;
import com.bc.ceres.figure.Figure;

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
            final FigureSelection figureSelection = getFigureEditor().getFigureSelection();
            if (figureMemento != null) {
                figureSelection.setMemento(figureMemento);
                figureMemento = null;
            }
            figureSelection.removeFigures();
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
                final FigureSelection figureSelection = getFigureEditor().getFigureSelection();
                figureSelection.selectHandle(referencePoint);
                final Handle selectedHandle = figureSelection.getSelectedHandle();
                if (selectedHandle != null) {
                    tool = TOOL_MOVE_HANDLE;
                } else {
                    if (figureSelection.contains(referencePoint)) {
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
        final FigureSelection figureSelection = getFigureEditor().getFigureSelection();
        Cursor cursor = null;
        for (Handle handle : figureSelection.getHandles()) {
            if (handle.contains(event.getPoint())) {
                cursor = handle.getCursor();
            }
        }
        if (cursor == null && figureSelection.contains(event.getPoint())) {
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
            final FigureSelection figureSelection = getFigureEditor().getFigureSelection();
            figureMemento = figureSelection.createMemento();
        }

        @Override
        public void drag(MouseEvent event) {
            final FigureSelection figureSelection = getFigureEditor().getFigureSelection();
            final int dx = event.getX() - referencePoint.x;
            final int dy = event.getY() - referencePoint.y;
            referencePoint = event.getPoint();
            figureSelection.move(dx, dy);
        }

        @Override
        public void end(MouseEvent event) {
            final FigureSelection figureSelection = getFigureEditor().getFigureSelection();
            getFigureEditor().postEdit(new RestorableEdit("Move Figure", figureSelection, figureMemento));
        }
    }

    private class MoveHandleTool implements Tool {
        @Override
        public void start(MouseEvent event) {
            final FigureSelection figureSelection = getFigureEditor().getFigureSelection();
            figureMemento = figureSelection.createMemento();
        }

        @Override
        public void drag(MouseEvent event) {
            final FigureSelection figureSelection = getFigureEditor().getFigureSelection();
            final Handle selectedHandle = figureSelection.getSelectedHandle();
            // If a handle is selected, it is moved by dragging
            selectedHandle.move(event.getX() - referencePoint.x,
                                event.getY() - referencePoint.y);
            referencePoint = event.getPoint();
        }

        @Override
        public void end(MouseEvent event) {
            final FigureSelection figureSelection = getFigureEditor().getFigureSelection();
            // Handles may have been moved, selection no longer required
            figureSelection.setSelectedHandle(null);
            getFigureEditor().postEdit(new RestorableEdit("Change Figure Shape", figureSelection, figureMemento));
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
            final FigureSelection figureSelection = getFigureEditor().getFigureSelection();
            for (Handle handle : figureSelection.getHandles()) {
                if (handle.isSelectable() && handle.contains(referencePoint)) {
                    figureSelection.setSelectedHandle(handle);
                    return;
                }
            }
            // Then check if user has selected a figure
            Figure clickedFigure = getFigureEditor().getFigureCollection().getFigure(referencePoint);
            if (clickedFigure == null) {
                figureSelection.removeFigures();
            } else if (figureSelection.getFigureCount() == 0) {
                figureSelection.addFigure(clickedFigure);
                figureSelection.setSelectionLevel(1);
            } else if (figureSelection.getFigureCount() == 1) {
                Figure singleSelectionFigure = figureSelection.getFigure(0);
                if (singleSelectionFigure == clickedFigure) {
                    int selectionLevel = figureSelection.getSelectionLevel() + 1;
                    if (selectionLevel > singleSelectionFigure.getMaxSelectionLevel()) {
                        selectionLevel = 0;
                    }
                    figureSelection.setSelectionLevel(selectionLevel);
                } else {
                    if (!event.isControlDown()) {
                        figureSelection.removeFigures();
                    }
                    figureSelection.addFigure(clickedFigure);
                    figureSelection.setSelectionLevel(1);
                }
            } else if (figureSelection.getFigureCount() >= 2) {
                if (figureSelection.contains(clickedFigure)) {
                    if (event.isControlDown()) {
                        figureSelection.removeFigure(clickedFigure);
                    }
                } else {
                    if (event.isControlDown()) {
                        figureSelection.addFigure(clickedFigure);
                    } else {
                        figureSelection.removeFigures();
                        figureSelection.addFigure(clickedFigure);
                    }
                }
                figureSelection.setSelectionLevel(1);
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
            final FigureSelection figureSelection = getFigureEditor().getFigureSelection();
            if (getSelectionRectangle() != null) {
                final Figure[] figures = getFigureEditor().getFigureCollection().getFigures(getSelectionRectangle());
                if (figures.length > 0) {
                    figureSelection.addFigures(figures);
                    figureSelection.setSelectionLevel(1);
                }
                setSelectionRectangle(null);
            }
        }
    }


}
