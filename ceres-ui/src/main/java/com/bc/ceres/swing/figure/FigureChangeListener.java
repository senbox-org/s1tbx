package com.bc.ceres.swing.figure;

public interface FigureChangeListener {
    
    void figureChanged(FigureChangeEvent event);
    
    void figuresAdded(FigureChangeEvent event);
    
    void figuresRemoved(FigureChangeEvent event);
}
