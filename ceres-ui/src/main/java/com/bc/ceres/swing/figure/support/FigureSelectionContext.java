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

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureChangeEvent;
import com.bc.ceres.swing.figure.FigureChangeListener;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.support.SelectionChangeSupport;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class FigureSelectionContext extends ExtensibleObject implements SelectionContext {

    private final FigureEditor figureEditor;
    private final FigureCollection figureCollection;
    private final FigureSelection figureSelection;
    private final SelectionChangeSupport selectionChangeSupport;

    public FigureSelectionContext(FigureEditor figureEditor) {
        this(figureEditor,
             new DefaultFigureCollection(),
             new DefaultFigureSelection());
    }

    public FigureSelectionContext(FigureEditor figureEditor,
                                  FigureCollection figureCollection,
                                  FigureSelection figureSelection) {
        Assert.notNull(figureEditor, "figureEditor");
        Assert.notNull(figureCollection, "figureCollection");
        Assert.notNull(figureSelection, "figureSelection");
        this.figureEditor = figureEditor;
        this.figureCollection = figureCollection;
        this.figureSelection = figureSelection;
        this.figureSelection.addChangeListener(new FigureSelectionMulticaster());
        this.selectionChangeSupport = new SelectionChangeSupport(figureEditor);
    }

    public FigureEditor getFigureEditor() {
        return figureEditor;
    }

    public FigureCollection getFigureCollection() {
        return figureCollection;
    }

    public FigureSelection getFigureSelection() {
        return figureSelection;
    }

    @Override
    public Selection getSelection() {
        return figureSelection;
    }

    @Override
    public void setSelection(Selection selection) {
        // todo - implement (select all figures that are equal to the ones in selection)
    }

    @Override
    public void addSelectionChangeListener(SelectionChangeListener listener) {
        selectionChangeSupport.addSelectionChangeListener(listener);
    }

    @Override
    public void removeSelectionChangeListener(SelectionChangeListener listener) {
        selectionChangeSupport.removeSelectionChangeListener(listener);
    }

    @Override
    public SelectionChangeListener[] getSelectionChangeListeners() {
        return selectionChangeSupport.getSelectionChangeListeners();
    }

    @Override
    public void insert(Transferable contents) throws IOException, UnsupportedFlavorException {
        Figure[] figures = (Figure[]) contents.getTransferData(FigureTransferable.FIGURES_DATA_FLAVOR);
        if (figures != null && figures.length  > 0) {
            figureEditor.insertFigures(true, figures);
        }
    }

    @Override
    public boolean canDeleteSelection() {
        return !getFigureSelection().isEmpty();
    }

    @Override
    public void deleteSelection() {
        Figure[] figures = getFigureSelection().getFigures();
        if (figures.length  > 0) {
            figureEditor.deleteFigures(true, figures);
        }
    }

    @Override
    public boolean canInsert(Transferable contents) {
        return contents.isDataFlavorSupported(FigureTransferable.FIGURES_DATA_FLAVOR);
    }

    @Override
    public void selectAll() {
        figureSelection.removeAllFigures();
        figureSelection.addFigures(getFigureCollection().getFigures());
        figureSelection.setSelectionStage(figureSelection.getMaxSelectionStage());
    }

    @Override
    public boolean canSelectAll() {
        return getFigureCollection().getFigureCount() > 0;
    }

    private class FigureSelectionMulticaster implements FigureChangeListener {

        @Override
        public void figureChanged(FigureChangeEvent event) {
            if (event.getType() == FigureChangeEvent.FIGURES_ADDED
                    || event.getType() == FigureChangeEvent.FIGURES_REMOVED) {
                selectionChangeSupport.fireSelectionChange(FigureSelectionContext.this, figureSelection);
            }
        }

    }
}