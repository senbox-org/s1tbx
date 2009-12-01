package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Viewport;

import java.awt.Component;
import java.awt.event.InputEvent;

public abstract class FigureEditorInteractor extends ViewportInteractor {

    protected FigureEditorInteractor() {
    }

    protected FigureEditor getFigureEditor(InputEvent inputEvent) {
        final Component component = inputEvent.getComponent();
        if (component instanceof FigureEditorHolder) {
            return ((FigureEditorHolder) component).getFigureEditor();
        } else if (component instanceof FigureEditor) {
            return (FigureEditor) component;
        } else {
            return null;
        }
    }

    @Override
    protected Viewport getViewport(InputEvent inputEvent) {
        final FigureEditor figureEditor = getFigureEditor(inputEvent);
        return figureEditor != null ? figureEditor.getViewport() : null;
    }
}