package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.Interaction;

public interface InteractionListener {
    void interactionActivated(Interaction interaction);

    void interactionDeactivated(Interaction interaction);

    void interactionStarted(Interaction interaction);

    void interactionStopped(Interaction interaction);

    void interactionCancelled(Interaction interaction);
}
