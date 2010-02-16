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
        this(text, null);
    }

    /**
     * Constructs an <code>ActionLabel</code> instance with the specified text and action.
     * The label is aligned against the leading edge of its display area,
     * and centered vertically.
     *
     * @param text   The text to be displayed by the label.
     * @param action The action to be performed if the label is clicked.
     */
    public ActionLabel(String text, ActionListener action) {
        super(text);
        oldText = getText();
        setForeground(Color.RED.darker().darker());
        addMouseListener(new MouseHandler());
        if (action != null) {
            addActionListener(action);
        }
    }

    public void addActionListener(ActionListener actionListener) {
        super.listenerList.add(ActionListener.class, actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        super.listenerList.remove(ActionListener.class, actionListener);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    protected void fireActionPerformed() {
        fireActionPerformed(new ActionEvent(this, 0, getName()));
    }

    protected void fireActionPerformed(ActionEvent event) {
        final ActionListener[] listeners = getActionListeners();
        for (ActionListener listener : listeners) {
            listener.actionPerformed(event);
        }
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
