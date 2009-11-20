package com.bc.ceres.figure;

public interface FigureChangeListener {
    
    void figureChanged(FigureChangeEvent event);
    
    void figuresAdded(FigureChangeEvent event);
    
    void figuresRemoved(FigureChangeEvent event);
}
