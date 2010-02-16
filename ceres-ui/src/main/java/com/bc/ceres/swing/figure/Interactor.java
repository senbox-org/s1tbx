package com.bc.ceres.swing.figure;

import java.awt.Cursor;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Interactors catch user input to a {@link FigureEditor} component and translate them into behaviour.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface Interactor extends MouseListener, MouseMotionListener, KeyListener {

    Cursor getCursor();

    boolean isActive();

    boolean activate();

    void deactivate();

    void addListener(InteractorListener l);

    void removeListener(InteractorListener l);

    InteractorListener[] getListeners();
}
