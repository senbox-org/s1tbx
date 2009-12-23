package org.esa.beam.visat.actions;

import com.bc.ceres.swing.figure.AbstractInteractorInterceptor;
import com.bc.ceres.swing.figure.Interactor;

import java.awt.event.InputEvent;

import org.esa.beam.util.Debug;

public class SelectionInteractorInterceptor extends AbstractInteractorInterceptor {
    @Override
    public boolean interactionAboutToStart(Interactor interactor, InputEvent inputEvent) {
        Debug.trace("SelectionInteractorInterceptor.interactionAboutToStart: inputEvent="+inputEvent);
        // todo - pre-select layer on clicked figure (nf)
        return true;
    }
}
