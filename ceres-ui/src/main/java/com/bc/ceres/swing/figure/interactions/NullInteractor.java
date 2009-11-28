package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.AbstractInteractor;

public final class NullInteractor extends AbstractInteractor {
    public static final NullInteractor INSTANCE = new NullInteractor();

    private NullInteractor() {
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }

    @Override
    public void cancelInteraction() {
    }

    @Override
    public void startInteraction() {
    }

    @Override
    public void stopInteraction() {
    }
}
