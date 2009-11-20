package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.FigureChangeEvent;
import com.bc.ceres.swing.figure.FigureChangeListener;

public abstract class AbstractFigureChangeListener implements FigureChangeListener {

    @Override
    public void figureChanged(FigureChangeEvent event) {
    }
    
    @Override
    public void figuresAdded(FigureChangeEvent event) {
    }
    
    @Override
    public void figuresRemoved(FigureChangeEvent event) {
    }
}
