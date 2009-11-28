package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.InteractorListener;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.Cursor;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public abstract class AbstractInteractor implements Interactor {
    private FigureEditor figureEditor;
    private ArrayList<InteractorListener> listeners;

    protected AbstractInteractor() {
        listeners = new ArrayList<InteractorListener>(3);
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
    public void cancelInteraction() {
        fireCancelled();
    }

    @Override
    public void startInteraction() {
        fireStarted();
    }

    @Override
    public void stopInteraction() {
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
            cancelInteraction();
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

    protected AffineTransform v2m() {
        return getFigureEditor().getViewport().getViewToModelTransform();
    }

    protected AffineTransform m2v() {
        return getFigureEditor().getViewport().getModelToViewTransform();
    }

    protected Point2D toModelPoint(MouseEvent event) {
        return toModelPoint(event.getPoint());
    }

    protected Point2D toModelPoint(Point2D point) {
        return v2m().transform(point, null);
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

    private void fireStarted() {
        for (InteractorListener listener : getListeners()) {
            listener.interactionStarted(this);
        }
    }

    private void fireStopped() {
        for (InteractorListener listener : getListeners()) {
            listener.interactionStopped(this);
        }
    }

    private void fireCancelled() {
        for (InteractorListener listener : getListeners()) {
            listener.interactionCancelled(this);
        }
    }
}
