package com.bc.ceres.swing.figure;

import junit.framework.TestCase;

import javax.swing.JMenu;
import java.awt.event.KeyEvent;

import com.bc.ceres.swing.figure.AbstractInteractor;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.InteractorListener;
import com.bc.ceres.swing.figure.support.DefaultFigureEditor;

public class AbstractInteractorTest extends TestCase {
    public void testListeners() {
        DefaultFigureEditor editor = new DefaultFigureEditor();
        AbstractInteractor interactor = new AbstractInteractor() {
        };
        MyInteractorListener listener = new MyInteractorListener();
        interactor.addListener(listener);

        interactor.activate(editor);
        assertEquals("a;", listener.trace);

        interactor.deactivate(editor);
        assertEquals("a;d;", listener.trace);

        interactor.activate(editor);
        assertEquals("a;d;a;", listener.trace);

        interactor.cancelInteraction();
        assertEquals("a;d;a;c;", listener.trace);

        interactor.activate(editor);
        assertEquals("a;d;a;c;d;a;", listener.trace);

        interactor.startInteraction();
        assertEquals("a;d;a;c;d;a;s;", listener.trace);

        interactor.stopInteraction();
        assertEquals("a;d;a;c;d;a;s;e;", listener.trace);

        interactor.deactivate(editor);
        assertEquals("a;d;a;c;d;a;s;e;d;", listener.trace);
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

    private static class MyInteractorListener implements InteractorListener {
        String trace = "";

        @Override
        public void interactorActivated(Interactor interactor) {
            trace += "a;";
        }

        @Override
        public void interactorDeactivated(Interactor interactor) {
            trace += "d;";
        }

        @Override
        public void interactionStarted(Interactor interactor) {
            trace += "s;";
        }

        @Override
        public void interactionStopped(Interactor interactor) {
            trace += "e;";
        }

        @Override
        public void interactionCancelled(Interactor interactor) {
            trace += "c;";
        }
    }
}
