package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.InteractorListener;

import java.awt.event.InputEvent;

public class AbstractInteractorListener implements InteractorListener, InteractorInterceptor {
    @Override
    public void interactorActivated(Interactor interactor) {
    }

    @Override
    public void interactorDeactivated(Interactor interactor) {
    }

    @Override
    public void interactionStarted(Interactor interactor, InputEvent inputEvent) {
    }

    @Override
    public void interactionStopped(Interactor interactor, InputEvent inputEvent) {
    }

    @Override
    public void interactionCancelled(Interactor interactor, InputEvent inputEvent) {
    }

    /**
     * @param interactor The interactor.
     * @return The default implementation returns {@code true}.
     */
    @Override
    public boolean canActivateInteractor(Interactor interactor) {
        return true;
    }

    /**
     * @param interactor The interactor.
     * @param inputEvent The interactor.
     * @return The default implementation returns {@code true}.
     */
    @Override
    public boolean canStartInteraction(Interactor interactor, InputEvent inputEvent) {
        return true;
    }
}
