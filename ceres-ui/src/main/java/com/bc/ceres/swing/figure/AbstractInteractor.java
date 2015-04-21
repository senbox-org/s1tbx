/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.swing.figure;

import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public abstract class AbstractInteractor implements Interactor {

    private boolean active;
    private ArrayList<InteractorListener> listeners;

    protected AbstractInteractor() {
        listeners = new ArrayList<>(3);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean activate() {
        if (!active) {
            active = canActivateInteractor();
            if (isActive()) {
                for (InteractorListener listener : getListeners()) {
                    listener.interactorActivated(this);
                }
            }
        }
        return isActive();
    }

    @Override
    public void deactivate() {
        if (active) {
            active = false;
            for (InteractorListener listener : getListeners()) {
                listener.interactorDeactivated(this);
            }
        }
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getDefaultCursor();
    }

    /**
     * Invoked when the mouse enters a component.
     * <p>
     * The default implementation does nothing.
     *
     * @param event The mouse event.
     */
    @Override
    public void mouseEntered(MouseEvent event) {
    }

    /**
     * Invoked when the mouse exits a component.
     * <p>
     * The default implementation does nothing.
     *
     * @param event The mouse event.
     */
    @Override
    public void mouseExited(MouseEvent event) {
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     * <p>
     * The default implementation does nothing.
     *
     * @param event The mouse event.
     */
    @Override
    public void mousePressed(MouseEvent event) {
    }

    /**
     * Invoked when a mouse button has been released on a component.
     * <p>
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
     * <p>
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
     * <p>
     * Due to platform-dependent Drag&amp;Drop implementations,
     * <code>MOUSE_DRAGGED</code> events may not be delivered during a native
     * Drag&amp;Drop operation.
     * <p>
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
     * <p>
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
     * <p>
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
     * <p>
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
     * <p>
     * The default implementation calls {@link #cancelInteraction(java.awt.event.InputEvent)} if the
     * "ESC" key has been typed.
     *
     * @param event The key event.
     */
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

    protected boolean startInteraction(InputEvent inputEvent) {
        if (canStartInteraction(inputEvent)) {
            for (InteractorListener listener : getListeners()) {
                listener.interactionStarted(this, inputEvent);
            }
            return true;
        } else {
            return false;
        }
    }

    protected void stopInteraction(InputEvent inputEvent) {
        for (InteractorListener listener : getListeners()) {
            listener.interactionStopped(this, inputEvent);
        }
    }

    protected void cancelInteraction(InputEvent inputEvent) {
        for (InteractorListener listener : getListeners()) {
            listener.interactionCancelled(this, inputEvent);
        }
    }

    protected static boolean isSingleButton1Click(MouseEvent e) {
        return isLeftMouseButtonDown(e) && e.getClickCount() == 1;
    }

    protected static boolean isMultiButton1Click(MouseEvent e) {
        return isLeftMouseButtonDown(e) && e.getClickCount() > 1;
    }

    protected static boolean isLeftMouseButtonDown(MouseEvent e) {
        return (e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0;
    }

    private boolean canActivateInteractor() {
        for (InteractorListener listener : getListeners()) {
            if (listener instanceof InteractorInterceptor) {
                final InteractorInterceptor interactorInterceptor = (InteractorInterceptor) listener;
                if (!interactorInterceptor.interactorAboutToActivate(this)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean canStartInteraction(InputEvent inputEvent) {
        for (InteractorListener listener : getListeners()) {
            if (listener instanceof InteractorInterceptor) {
                final InteractorInterceptor interactorInterceptor = (InteractorInterceptor) listener;
                if (!interactorInterceptor.interactionAboutToStart(this, inputEvent)) {
                    return false;
                }
            }
        }
        return true;
    }

}
