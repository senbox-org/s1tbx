package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.Interaction;
import com.bc.ceres.swing.figure.InteractionListener;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public abstract class AbstractInteraction implements Interaction {
    private FigureEditor figureEditor;
    private ArrayList<InteractionListener> listeners;

    protected AbstractInteraction() {
        listeners = new ArrayList<InteractionListener>(3);
    }

    @Override
    public void activate() {
        fireActivated();
    }

    @Override
    public void deactivate() {
        fireDeactivated();
    }

    @Override
    public void cancel() {
        fireCancelled();
    }

    @Override
    public void start() {
        fireStarted();
    }

    @Override
    public void stop() {
        fireStopped();
    }

    @Override
    public FigureEditor getFigureEditor() {
        return figureEditor;
    }

    @Override
    public void setFigureEditor(FigureEditor figureEditor) {
        this.figureEditor = figureEditor;
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
            cancel();
        }
    }

    @Override
    public void addListener(InteractionListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListener(InteractionListener l) {
        listeners.remove(l);
    }

    @Override
    public InteractionListener[] getListeners() {
        return this.listeners.toArray(new InteractionListener[this.listeners.size()]);
    }

    private void fireActivated() {
        for (InteractionListener listener : getListeners()) {
            listener.interactionActivated(this);
        }
    }

    private void fireDeactivated() {
        for (InteractionListener listener : getListeners()) {
            listener.interactionDeactivated(this);
        }
    }

    private void fireStarted() {
        for (InteractionListener listener : getListeners()) {
            listener.interactionStarted(this);
        }
    }

    private void fireStopped() {
        for (InteractionListener listener : getListeners()) {
            listener.interactionStopped(this);
        }
    }

    private void fireCancelled() {
        for (InteractionListener listener : getListeners()) {
            listener.interactionCancelled(this);
        }
    }
}
