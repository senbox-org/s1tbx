package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.Interactor;

import java.awt.event.InputEvent;

public interface InteractorListener {
    void interactorActivated(Interactor interactor);

    void interactorDeactivated(Interactor interactor);

    void interactionStarted(Interactor interactor, InputEvent inputEvent);

    void interactionStopped(Interactor interactor, InputEvent inputEvent);

    void interactionCancelled(Interactor interactor, InputEvent inputEvent);
}
