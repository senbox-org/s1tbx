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

    /**
     * Invoked when the mouse enters a component.
     * <p/>
     * The default implementation does nothing.
     *
     * @param event The mouse event.
     */
    @Override
    public void mouseEntered(MouseEvent event) {
    }

    /**
     * Invoked when the mouse exits a component.
     * <p/>
     * The default implementation does nothing.
     *
     * @param event The mouse event.
     */
    @Override
    public void mouseExited(MouseEvent event) {
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     * <p/>
     * The default implementation does nothing.
     *
     * @param event The mouse event.
     */
    @Override
    public void mousePressed(MouseEvent event) {
    }

    /**
     * Invoked when a mouse button has been released on a component.
     * <p/>
     * The default implementation does nothing.
     *
     * @param event The mouse event.
     */
    @Override
    public void mouseReleased(MouseEvent event) {
    }

    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a component.
     * <p/>
     * The default implementation does nothing.
     *
     * @param event The mouse event.
     */
    @Override
    public void mouseClicked(MouseEvent event) {
    }

    /**
     * Invoked when a mouse button is pressed on a component and then
     * dragged.  <code>MOUSE_DRAGGED</code> events will continue to be
     * delivered to the component where the drag originated until the
     * mouse button is released (regardless of whether the mouse position
     * is within the bounds of the component).
     * <p/>
     * Due to platform-dependent Drag&Drop implementations,
     * <code>MOUSE_DRAGGED</code> events may not be delivered during a native
     * Drag&Drop operation.
     * <p/>
     * The default implementation does nothing.
     *
     * @param event The mouse event.
     */
    @Override
    public void mouseDragged(MouseEvent event) {
    }

    /**
     * Invoked when the mouse cursor has been moved onto a component
     * but no buttons have been pushed.
     * <p/>
     * The default implementation does nothing.
     *
     * @param event The mouse event.
     */
    @Override
    public void mouseMoved(MouseEvent event) {
    }

    /**
     * Invoked when a key has been pressed.
     * See the class description for {@link KeyEvent} for a definition of
     * a key pressed event.
     * <p/>
     * The default implementation does nothing.
     *
     * @param event The key event.
     */
    @Override
    public void keyPressed(KeyEvent event) {
    }

    /**
     * Invoked when a key has been released.
     * See the class description for {@link KeyEvent} for a definition of
     * a key released event.
     * <p/>
     * The default implementation does nothing.
     *
     * @param event The key event.
     */
    @Override
    public void keyReleased(KeyEvent event) {
    }

    /**
     * Invoked when a key has been typed.
     * See the class description for {@link KeyEvent} for a definition of
     * a key typed event.
     * <p/>
     * The default implementation calls {@link #cancelInteraction(java.awt.event.InputEvent)} if the
     * "ESC" key has been typed.
     *
     * @param event The key event.
     */
    @Override
    public void keyTyped(KeyEvent event) {
        System.out.println("onKeyTyped: interaction = " + this + ", keyChar = " + (int) event.getKeyChar());
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
