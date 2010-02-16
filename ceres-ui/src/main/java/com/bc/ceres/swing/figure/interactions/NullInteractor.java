package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.InteractorListener;

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public final class NullInteractor implements Interactor {

    public static final NullInteractor INSTANCE = new NullInteractor();

    private NullInteractor() {
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getDefaultCursor();
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean activate() {
        return false;
    }

    @Override
    public void deactivate() {
    }

    @Override
    public void addListener(InteractorListener l) {
    }

    @Override
    public void removeListener(InteractorListener l) {
    }

    @Override
    public InteractorListener[] getListeners() {
        return new InteractorListener[0];
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
