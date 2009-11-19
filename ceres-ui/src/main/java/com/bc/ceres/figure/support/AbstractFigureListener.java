package com.bc.ceres.figure.support;

import com.bc.ceres.figure.FigureListener;
import com.bc.ceres.figure.Figure;

public abstract class AbstractFigureListener implements FigureListener {
    @Override
    public void figureChanged(Figure figure) {
    }

    @Override
    public void figuresAdded(Figure parent, Figure[] children) {
    }

    @Override
    public void figuresRemoved(Figure parent, Figure[] children) {
    }
}
