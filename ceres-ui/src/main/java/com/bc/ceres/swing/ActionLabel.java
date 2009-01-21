package com.bc.ceres.swing;

import javax.swing.JLabel;
import javax.swing.event.MouseInputAdapter;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

/**
 * A {@link JLabel} which fires action events when clicked.
 */
public class ActionLabel extends JLabel {

    private String oldText;
    private Cursor oldCursor;

    /**
     * Constructs an <code>ActionLabel</code> instance.
     * The label is aligned against the leading edge of its display area,
     * and centered vertically.
     */
    public ActionLabel() {
        this(null);
    }


    /**
     * Constructs an <code>ActionLabel</code> instance with the specified text.
     * The label is aligned against the leading edge of its display area,
     * and centered vertically.
     *
     * @param text The text to be displayed by the label.
     */
    public ActionLabel(String text) {
        super(text);
        oldText = getText();
        addMouseListener(new MouseHandler());
        setForeground(Color.RED.darker().darker());
    }

    public void addActionListener(ActionListener actionListener) {
        super.listenerList.add(ActionListener.class, actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        super.listenerList.remove(ActionListener.class, actionListener);
    }

    private void startHover() {
        oldText = getText();
        if (oldText != null) {
            setText("<html><u>" + oldText + "</u></html>");
        }
        oldCursor = getCursor();
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void stopHover() {
        setText(oldText);
        setCursor(oldCursor);
    }

    private void fireActionPerformed() {
        final ActionEvent event = new ActionEvent(ActionLabel.this, 0, getName());
        final ActionListener[] listeners = listenerList.getListeners(ActionListener.class);
        for (ActionListener listener : listeners) {
            listener.actionPerformed(event);
        }
    }

    private class MouseHandler extends MouseInputAdapter {

        @Override
        public void mouseEntered(MouseEvent e) {
            startHover();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            stopHover();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            fireActionPerformed();
        }
    }
}
