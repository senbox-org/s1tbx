/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Viewport;

import java.awt.Component;
import java.awt.event.InputEvent;

public abstract class FigureEditorInteractor extends ViewportInteractor {

    protected FigureEditorInteractor() {
    }

    public FigureEditor getFigureEditor(InputEvent inputEvent) {
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