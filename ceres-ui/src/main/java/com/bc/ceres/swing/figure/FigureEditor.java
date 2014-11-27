/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.ViewportAware;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.undo.UndoContext;

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

    UndoContext getUndoContext();

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
