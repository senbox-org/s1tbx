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

package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureEditor;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;


public class FigureInsertEdit extends AbstractUndoableEdit {

    private FigureEditor figureEditor;
    private Figure[] addedFigures;

    public FigureInsertEdit(FigureEditor figureEditor, boolean performInsert, Figure... figuresToInsert) {
        this.figureEditor = figureEditor;
        this.addedFigures = performInsert ? figureEditor.getFigureCollection().addFigures(figuresToInsert) : figuresToInsert.clone();
        figureEditor.getFigureSelection().removeAllFigures();
        figureEditor.getFigureSelection().addFigures(addedFigures);
        figureEditor.getFigureSelection().setSelectionStage(1);
    }

    @Override
    public String getPresentationName() {
        return addedFigures.length == 1 ? "Insert Figure" : "Insert Figures";
    }

    @Override
    public void die() {
        super.die();
        figureEditor = null;
        addedFigures = null;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        figureEditor.getFigureSelection().removeAllFigures();
        figureEditor.getFigureSelection().setSelectionStage(0);
        figureEditor.getFigureCollection().removeFigures(addedFigures);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        figureEditor.getFigureSelection().removeAllFigures();
        figureEditor.getFigureSelection().setSelectionStage(0);
        figureEditor.getFigureCollection().addFigures(addedFigures);
    }
}
