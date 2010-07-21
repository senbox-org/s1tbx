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

import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.InteractorAware;

import javax.swing.JComponent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class InteractionDispatcher implements MouseListener, MouseMotionListener, KeyListener {
    private final InteractorAware interactorAware;
    private final boolean debug = Boolean.getBoolean(InteractionDispatcher.class.getName() + ".debug");

    public InteractionDispatcher(InteractorAware interactorAware) {
        this.interactorAware = interactorAware;
    }

    public void registerListeners(JComponent component) {
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
        component.addKeyListener(this);
    }

    public void unregisterListeners(JComponent component) {
        component.removeMouseListener(this);
        component.removeMouseMotionListener(this);
        component.removeKeyListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (debug) {
            System.out.println("InteractionDispatcher.mouseClicked: event = " + event);
        }
        if (getInteractor().isActive()) {
            getInteractor().mouseClicked(event);
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (debug) {
            System.out.println("InteractionDispatcher.mousePressed: event = " + event);
        }
        ensureKeyEventsReceived(event);
        if (getInteractor().isActive()) {
            getInteractor().mousePressed(event);
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        if (debug) {
            // System.out.println("InteractionDispatcher.mouseMoved: event = " + event);
        }
        if (getInteractor().isActive()) {
            getInteractor().mouseMoved(event);
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (debug) {
            // System.out.println("InteractionDispatcher.mouseDragged: event = " + event);
        }
        if (getInteractor().isActive()) {
            getInteractor().mouseDragged(event);
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (debug) {
            System.out.println("InteractionDispatcher.mouseReleased: event = " + event);
        }
        if (getInteractor().isActive()) {
            getInteractor().mouseReleased(event);
        }
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        if (debug) {
            System.out.println("InteractionDispatcher.mouseEntered: event = " + event);
        }
        ensureKeyEventsReceived(event);
        if (getInteractor().isActive()) {
            getInteractor().mouseEntered(event);
        }
    }

    @Override
    public void mouseExited(MouseEvent event) {
        if (debug) {
            System.out.println("InteractionDispatcher.mouseExited: event = " + event);
        }
        if (getInteractor().isActive()) {
            getInteractor().mouseExited(event);
        }
    }

    @Override
    public void keyTyped(KeyEvent event) {
        if (debug) {
            System.out.println("InteractionDispatcher.keyTyped: event = " + event);
        }
        if (getInteractor().isActive()) {
            getInteractor().keyTyped(event);
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (debug) {
            System.out.println("InteractionDispatcher.keyPressed: event = " + event);
        }
        if (getInteractor().isActive()) {
            getInteractor().keyPressed(event);
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        if (debug) {
            System.out.println("InteractionDispatcher.keyReleased: event = " + event);
        }
        if (getInteractor().isActive()) {
            getInteractor().keyReleased(event);
        }
    }

    private Interactor getInteractor() {
        return interactorAware.getInteractor();
    }

    private void ensureKeyEventsReceived(MouseEvent e) {
        // request focus to get notified about key events
        e.getComponent().requestFocusInWindow();
    }

}
