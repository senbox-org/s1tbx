package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.AbstractInteractor;
import com.bc.ceres.swing.figure.FigureEditor;

public final class NullInteractor extends AbstractInteractor {
    public static final NullInteractor INSTANCE = new NullInteractor();

    private NullInteractor() {
    }

    @Override
    public void activate(FigureEditor figureEditor) {
    }

    @Override
    public void deactivate(FigureEditor figureEditor) {
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
