package com.bc.ceres.swing.figure;

import com.bc.ceres.selection.SelectionContext;
import com.bc.ceres.swing.undo.UndoContext;
import com.bc.ceres.swing.figure.InteractionHolder;
import com.bc.ceres.swing.figure.Interaction;
import com.bc.ceres.figure.support.FigureSelection;
import com.bc.ceres.figure.support.FigureCollection;

import java.awt.Cursor;
import java.awt.Rectangle;

// todo - remove dependency to com.bc.ceres.swing
public interface FigureEditor extends SelectionContext, UndoContext, InteractionHolder {
    Rectangle getSelectionRectangle();

    void setSelectionRectangle(Rectangle rectangle);

    void setInteraction(Interaction interaction);

    FigureSelection getFigureSelection();

    FigureCollection getFigureCollection();

    void repaint();

    void setCursor(Cursor cursor);
}
