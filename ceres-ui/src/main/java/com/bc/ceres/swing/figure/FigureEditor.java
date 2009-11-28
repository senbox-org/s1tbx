package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.undo.UndoContext;
import com.bc.ceres.swing.figure.InteractorHolder;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.grender.ViewportOwner;

import java.awt.Cursor;
import java.awt.Rectangle;

public interface FigureEditor extends InteractorHolder, ViewportOwner {
    SelectionContext getSelectionContext();

    UndoContext getUndoContext();

    Rectangle getSelectionRectangle();

    void setSelectionRectangle(Rectangle rectangle);

    void setInteractor(Interactor interactor);

    FigureSelection getFigureSelection();

    FigureCollection getFigureCollection();

    void repaint();

    void setCursor(Cursor cursor);
}
