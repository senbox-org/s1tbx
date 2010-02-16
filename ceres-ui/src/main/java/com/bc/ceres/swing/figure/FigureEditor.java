package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.ViewportAware;
import com.bc.ceres.swing.selection.SelectionContext;

import javax.swing.JComponent;
import java.awt.Rectangle;

/**
 * A figure editor is used to insert, delete and modify figures stored in a figure collection.
 * It also publishes a selection context representing the selected figures.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface FigureEditor extends InteractorAware, ViewportAware, SelectionContext {
    FigureFactory getFigureFactory();

    JComponent getEditorComponent();

    SelectionContext getSelectionContext();

    Rectangle getSelectionRectangle();

    void setSelectionRectangle(Rectangle rectangle);

    void setInteractor(Interactor interactor);

    FigureSelection getFigureSelection();

    FigureCollection getFigureCollection();

    void insertFigures(boolean performInsert, Figure... figures);

    void deleteFigures(boolean performDelete, Figure... figures);

    void changeFigure(Figure figure, Object figureMemento, String presentationName);

    FigureStyle getDefaultLineStyle();

    FigureStyle getDefaultPolygonStyle();
}
