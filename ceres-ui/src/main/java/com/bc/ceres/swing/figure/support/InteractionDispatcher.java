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
        getInteraction().mouseClicked(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        getInteraction().mousePressed(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        getInteraction().mouseMoved(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        getInteraction().mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        getInteraction().mouseReleased(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        getInteraction().mouseEntered(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        getInteraction().mouseExited(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        getInteraction().keyTyped(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        getInteraction().keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        getInteraction().keyReleased(e);
    }

    private Interactor getInteraction() {
        return interactorHolder.getInteractor();
    }
}
