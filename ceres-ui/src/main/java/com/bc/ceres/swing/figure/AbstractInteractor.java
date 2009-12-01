package com.bc.ceres.swing.figure;

import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public abstract class AbstractInteractor implements Interactor {

    private ArrayList<InteractorListener> listeners;
    private boolean active;

    protected AbstractInteractor() {
        listeners = new ArrayList<InteractorListener>(3);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void activate() {
        if (!active) {
            this.active = true;
            fireActivated();
        }
    }

    @Override
    public void deactivate() {
        if (active) {
            this.active = false;
            fireDeactivated();
        }
    }

    protected void startInteraction(InputEvent inputEvent) {
        fireStarted(inputEvent);
    }

    protected void stopInteraction(InputEvent inputEvent) {
        fireStopped(inputEvent);
    }

    protected void cancelInteraction(InputEvent inputEvent) {
        fireCancelled(inputEvent);
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getDefaultCursor();
    }

    @Override
    public void mouseClicked(MouseEvent event) {
    }

    @Override
    public void mousePressed(MouseEvent event) {
    }

    @Override
    public void mouseDragged(MouseEvent event) {
    }

    @Override
    public void mouseMoved(MouseEvent event) {
    }

    @Override
    public void mouseReleased(MouseEvent event) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent event) {
    }

    @Override
    public void keyReleased(KeyEvent event) {
    }

    @Override
    public void keyTyped(KeyEvent event) {
        // System.out.println("onKeyTyped: interaction = " + this + ", keyChar = " + (int) event.getKeyChar());
        if (event.getKeyChar() == 27) {
            cancelInteraction(event);
        }
    }

    @Override
    public void addListener(InteractorListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListener(InteractorListener l) {
        listeners.remove(l);
    }

    @Override
    public InteractorListener[] getListeners() {
        return this.listeners.toArray(new InteractorListener[this.listeners.size()]);
    }

    private void fireActivated() {
        for (InteractorListener listener : getListeners()) {
            listener.interactorActivated(this);
        }
    }

    private void fireDeactivated() {
        for (InteractorListener listener : getListeners()) {
            listener.interactorDeactivated(this);
        }
    }

    private void fireStarted(InputEvent inputEvent) {
        for (InteractorListener listener : getListeners()) {
            listener.interactionStarted(this, inputEvent);
        }
    }

    private void fireStopped(InputEvent inputEvent) {
        for (InteractorListener listener : getListeners()) {
            listener.interactionStopped(this, inputEvent);
        }
    }

    private void fireCancelled(InputEvent inputEvent) {
        for (InteractorListener listener : getListeners()) {
            listener.interactionCancelled(this, inputEvent);
        }
    }
}
