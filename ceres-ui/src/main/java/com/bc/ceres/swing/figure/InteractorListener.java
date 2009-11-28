package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.Interactor;

public interface InteractorListener {
    void interactorActivated(Interactor interactor);

    void interactorDeactivated(Interactor interactor);

    void interactionStarted(Interactor interactor);

    void interactionStopped(Interactor interactor);

    void interactionCancelled(Interactor interactor);
}
