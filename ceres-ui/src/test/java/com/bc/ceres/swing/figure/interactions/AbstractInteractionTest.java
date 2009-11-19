package com.bc.ceres.swing.figure.interactions;

import junit.framework.TestCase;

import javax.swing.JMenu;
import java.awt.event.KeyEvent;

import com.bc.ceres.swing.figure.interactions.AbstractInteraction;
import com.bc.ceres.swing.figure.Interaction;
import com.bc.ceres.swing.figure.InteractionListener;

public class AbstractInteractionTest extends TestCase {
    public void testListeners() {
        AbstractInteraction interaction = new AbstractInteraction() {
        };
        MyInteractionListener listener = new MyInteractionListener();
        interaction.addListener(listener);

        interaction.activate();
        assertEquals("a;", listener.trace);

        interaction.deactivate();
        assertEquals("a;d;", listener.trace);

        interaction.activate();
        assertEquals("a;d;a;", listener.trace);

        interaction.cancel();
        assertEquals("a;d;a;c;", listener.trace);

        interaction.activate();
        assertEquals("a;d;a;c;a;", listener.trace);

        interaction.start();
        assertEquals("a;d;a;c;a;s;", listener.trace);

        interaction.stop();
        assertEquals("a;d;a;c;a;s;e;", listener.trace);

        interaction.deactivate();
        assertEquals("a;d;a;c;a;s;e;d;", listener.trace);
    }

    public void testEscKeyPressedInvokesCancel() {
        AbstractInteraction interaction = new AbstractInteraction() {
        };
        MyInteractionListener listener = new MyInteractionListener();
        interaction.addListener(listener);

        JMenu source = new JMenu();

        interaction.keyTyped(new KeyEvent(source, 0, 0, 0, ' ', ' '));
        assertEquals("", listener.trace); // ==> cancel() NOT called

        interaction.keyTyped(new KeyEvent(source, 0, 0, 0, 27, (char) 27));
        assertEquals("c;", listener.trace); // ==> cancel() called

        interaction.keyTyped(new KeyEvent(source, 0, 0, 0, 'A', 'A'));
        assertEquals("c;", listener.trace); // ==> cancel() NOT called
    }

    private static class MyInteractionListener implements InteractionListener {
        String trace = "";

        @Override
        public void interactionActivated(Interaction interaction) {
            trace += "a;";
        }

        @Override
        public void interactionDeactivated(Interaction interaction) {
            trace += "d;";
        }

        @Override
        public void interactionStarted(Interaction interaction) {
            trace += "s;";
        }

        @Override
        public void interactionStopped(Interaction interaction) {
            trace += "e;";
        }

        @Override
        public void interactionCancelled(Interaction interaction) {
            trace += "c;";
        }
    }
}
