package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Viewport;

import java.awt.Component;
import java.awt.event.InputEvent;

public abstract class FigureEditorInteractor extends ViewportInteractor {

    protected FigureEditorInteractor() {
    }

    protected FigureEditor getFigureEditor(InputEvent inputEvent) {
        return getFigureEditor(inputEvent.getComponent());
    }

    @Override
    protected Viewport getViewport(InputEvent inputEvent) {
        final Viewport viewport =super.getViewport(inputEvent);
        if (viewport != null) {
            return viewport;
        }
        FigureEditor figureEditor = getFigureEditor(inputEvent);
        return figureEditor != null ? figureEditor.getViewport() : null;
    }

    private FigureEditor getFigureEditor(Component component) {
        while (true) {
            if (component instanceof FigureEditorAware) {
                return ((FigureEditorAware) component).getFigureEditor();
            } else if (component instanceof FigureEditor) {
                return (FigureEditor) component;
            } else if (component == null || component.getParent() == null) {
                return null;
            }
            component = component.getParent();
        }
    }

}