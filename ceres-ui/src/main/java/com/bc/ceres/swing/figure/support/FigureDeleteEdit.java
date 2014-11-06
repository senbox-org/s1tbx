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
import java.util.Arrays;
import java.util.HashSet;

public class FigureDeleteEdit extends AbstractUndoableEdit {
    private FigureEditor figureEditor;
    private Figure[] deletedFigures;
    private int[] deletedPositions;

    public FigureDeleteEdit(FigureEditor figureEditor, boolean performDelete, Figure... figuresToDelete) {

        Figure[] figures = figureEditor.getFigureCollection().getFigures();

        this.figureEditor = figureEditor;
        this.deletedFigures = performDelete ? figureEditor.getFigureCollection().removeFigures(figuresToDelete) : figuresToDelete.clone();
        this.deletedPositions = new int[this.deletedFigures.length];

        figureEditor.getFigureSelection().removeFigures(this.deletedFigures);
        figureEditor.getFigureSelection().setSelectionStage(0);

        HashSet<Figure> deletedFigureSet = new HashSet<Figure>(Arrays.asList(this.deletedFigures));
        int index = 0;
        for (int i = 0; i < figures.length; i++) {
            Figure figure = figures[i];
            if (deletedFigureSet.contains(figure)) {
                deletedPositions[index++] = i;
            }
        }

        if (index < deletedPositions.length) {
            throw new IllegalStateException("i < deletedPositions.length");
        }
    }

    @Override
    public String getPresentationName() {
        return deletedFigures.length == 1 ? "Delete Figure" : "Delete Figures";
    }

    @Override
    public void die() {
        super.die();
        Arrays.fill(deletedFigures, null);
        figureEditor = null;
        deletedFigures = null;
        deletedPositions = null;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        for (int i = 0; i < deletedFigures.length; i++) {
            int index = deletedPositions[i];
            if (index >= 0 && index < figureEditor.getFigureCollection().getFigureCount()) {
                figureEditor.getFigureCollection().addFigure(index, deletedFigures[i]);
            } else {
                figureEditor.getFigureCollection().addFigure(deletedFigures[i]);
            }
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        figureEditor.getFigureCollection().removeFigures(deletedFigures);
        figureEditor.getFigureSelection().removeAllFigures();
    }
}
