package com.bc.ceres.figure;

public interface FigureListener {
    void figureChanged(Figure figure);

    void figuresAdded(Figure parent, Figure[] children);

    void figuresRemoved(Figure parent, Figure[] children);
}
