package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.undo.UndoContext;
import com.bc.ceres.swing.figure.InteractorHolder;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import com.bc.ceres.grender.ViewportOwner;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Rectangle;

/**
 * A figure editor is used to insert, delete and modify figures stored in a figure collection.
 * It also publishes a selection context representing the selected figures.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface FigureEditor extends InteractorHolder, ViewportOwner {

    JComponent getEditorComponent();

    SelectionContext getSelectionContext();

    Rectangle getSelectionRectangle();

    void setSelectionRectangle(Rectangle rectangle);

    void setInteractor(Interactor interactor);

    FigureSelection getFigureSelection();

    FigureCollection getFigureCollection();

    void setCursor(Cursor cursor);

    void insertFigures(boolean performInsert, Figure... figures);

    void deleteFigures(boolean performDelete, Figure... figures);

    void changeFigure(Figure figure, Object figureMemento, String presentationName);
}
