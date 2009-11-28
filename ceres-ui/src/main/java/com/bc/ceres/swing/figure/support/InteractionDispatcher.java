package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.InteractorHolder;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class InteractionDispatcher implements MouseListener, MouseMotionListener, KeyListener {
    private final InteractorHolder interactorHolder;

    public InteractionDispatcher(InteractorHolder interactorHolder) {
        this.interactorHolder = interactorHolder;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        getInteractor().mouseClicked(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        getInteractor().mousePressed(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        getInteractor().mouseMoved(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        getInteractor().mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        getInteractor().mouseReleased(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        getInteractor().mouseEntered(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        getInteractor().mouseExited(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        getInteractor().keyTyped(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        getInteractor().keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        getInteractor().keyReleased(e);
    }

    private Interactor getInteractor() {
        return interactorHolder.getInteractor();
    }
}
