package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.InteractionListener;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.Cursor;

// todo - interaction should work also on non-figure contexts, e.g. SelectionInteraction is actually very generic
public interface Interaction extends MouseListener, MouseMotionListener, KeyListener {
    FigureEditor getFigureEditor();

    void setFigureEditor(FigureEditor figureEditor);

    Cursor getCursor();

    void activate();

    void deactivate();

    void start();

    void stop();

    void cancel();

    void addListener(InteractionListener l);

    void removeListener(InteractionListener l);

    InteractionListener[] getListeners();
}
