package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.InteractorListener;

import java.awt.event.InputEvent;

public class AbstractInteractorInterceptor extends AbstractInteractorListener implements InteractorInterceptor {
    /**
     * @param interactor The interactor.
     * @return The default implementation returns {@code true}.
     */
    @Override
    public boolean interactorAboutToActivate(Interactor interactor) {
        return true;
    }

    /**
     * @param interactor The interactor.
     * @param inputEvent The interactor.
     * @return The default implementation returns {@code true}.
     */
    @Override
    public boolean interactionAboutToStart(Interactor interactor, InputEvent inputEvent) {
        return true;
    }
}