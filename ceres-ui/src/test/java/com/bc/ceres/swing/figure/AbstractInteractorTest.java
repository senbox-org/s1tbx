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

import com.bc.ceres.swing.figure.support.DefaultFigureCollection;
import com.bc.ceres.swing.figure.support.DefaultFigureFactory;
import com.bc.ceres.swing.figure.support.FigureEditorPanel;
import junit.framework.TestCase;

import javax.swing.JMenu;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class AbstractInteractorTest extends TestCase {

    public void testListeners() {
        FigureEditorPanel figureEditorPanel = new FigureEditorPanel(null,
                                                                    new DefaultFigureCollection(),
                                                                    new DefaultFigureFactory());
        AbstractInteractor interactor = new AbstractInteractor() {
        };

        MyInteractorListener listener = new MyInteractorListener();
        interactor.addListener(listener);

        MouseEvent event = new MouseEvent(figureEditorPanel, 0, 0, 0, 0, 0, 1, false, 0);

        interactor.activate();
        assertEquals("a?;a;", listener.trace);

        interactor.deactivate();
        assertEquals("a?;a;d;", listener.trace);

        interactor.activate();
        assertEquals("a?;a;d;a?;a;", listener.trace);

        interactor.cancelInteraction(event);
        assertEquals("a?;a;d;a?;a;c;", listener.trace);

        interactor.activate();
        assertEquals("a?;a;d;a?;a;c;", listener.trace);

        interactor.startInteraction(event);
        assertEquals("a?;a;d;a?;a;c;s?;s;", listener.trace);

        interactor.stopInteraction(event);
        assertEquals("a?;a;d;a?;a;c;s?;s;e;", listener.trace);

        interactor.deactivate();
        assertEquals("a?;a;d;a?;a;c;s?;s;e;d;", listener.trace);
    }

    public void testEscKeyPressedInvokesCancel() {
        AbstractInteractor interaction = new AbstractInteractor() {
        };
        MyInteractorListener listener = new MyInteractorListener();
        interaction.addListener(listener);

        JMenu source = new JMenu();

        interaction.keyTyped(new KeyEvent(source, 0, 0, 0, ' ', ' '));
        assertEquals("", listener.trace); // ==> cancel() NOT called

        interaction.keyTyped(new KeyEvent(source, 0, 0, 0, 27, (char) 27));
        assertEquals("c;", listener.trace); // ==> cancel() called

        interaction.keyTyped(new KeyEvent(source, 0, 0, 0, 'A', 'A'));
        assertEquals("c;", listener.trace); // ==> cancel() NOT called
    }

    private static class MyInteractorListener extends AbstractInteractorInterceptor {

        String trace = "";

        @Override
        public boolean interactorAboutToActivate(Interactor interactor) {
            trace += "a?;";
            return true;
        }

        @Override
        public boolean interactionAboutToStart(Interactor interactor, InputEvent inputEvent) {
            trace += "s?;";
            return true;
        }

        @Override
        public void interactorActivated(Interactor interactor) {
            trace += "a;";
        }

        @Override
        public void interactorDeactivated(Interactor interactor) {
            trace += "d;";
        }

        @Override
        public void interactionStarted(Interactor interactor, InputEvent event) {
            trace += "s;";
        }

        @Override
        public void interactionStopped(Interactor interactor, InputEvent inputEvent) {
            trace += "e;";
        }

        @Override
        public void interactionCancelled(Interactor interactor, InputEvent inputEvent) {
            trace += "c;";
        }
    }
}
