package com.bc.ceres.swing.figure;

/**
 * A change listener which can be registered with a {@link Figure}.
 */
public interface FigureChangeListener {
    
    void figureChanged(FigureChangeEvent event);
    
    void figuresAdded(FigureChangeEvent event);
    
    void figuresRemoved(FigureChangeEvent event);
}
