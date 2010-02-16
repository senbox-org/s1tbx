package com.bc.ceres.swing.figure;

import java.awt.Cursor;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.InputEvent;

/**
 * Interactor interceptors are used to check whether an interaction can be performed or not.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface InteractorInterceptor {

    boolean interactorAboutToActivate(Interactor interactor);

    boolean interactionAboutToStart(Interactor interactor, InputEvent event);
}