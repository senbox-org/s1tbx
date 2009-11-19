package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.interactions.AbstractInteraction;

public final class NullInteraction extends AbstractInteraction {
    public static final NullInteraction INSTANCE = new NullInteraction();

    private NullInteraction() {
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }

    @Override
    public void cancel() {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}
