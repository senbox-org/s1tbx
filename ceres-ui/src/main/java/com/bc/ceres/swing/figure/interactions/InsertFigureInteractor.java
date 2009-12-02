package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.FigureEditorInteractor;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.support.DefaultFigureFactory;

public abstract class InsertFigureInteractor extends FigureEditorInteractor {

    private FigureFactory figureFactory;

    public InsertFigureInteractor() {
        this(new DefaultFigureFactory());
    }

    public InsertFigureInteractor(FigureFactory figureFactory) {
        this.figureFactory = figureFactory;
    }

    public FigureFactory getFigureFactory() {
        return figureFactory;
    }

    public void setFigureFactory(FigureFactory figureFactory) {
        this.figureFactory = figureFactory;
    }
}