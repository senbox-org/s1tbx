package com.bc.ceres.swing;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.MouseInputAdapter;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class UriLabel extends JLabel {
    private URI uri;
    private String oldText;
    private Cursor oldCursor;

    public UriLabel() {
        this(null, null);
    }

    public UriLabel(URI uri) {
        this(null, uri);
    }

    /**
     * Creates a <code>JLabel</code> instance with the specified text.
     * The label is aligned against the leading edge of its display area,
     * and centered vertically.
     *
     * @param text The text to be displayed by the label.
     */
    public UriLabel(String text, URI uri) {
        super(text);
        oldText = getText();
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            addMouseListener(new MouseHandler());
        }
        setUri(uri);
        setForeground(Color.RED.darker().darker());
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
        if (uri != null && (getText() == null || getText().length() == 0)) {
            setText(uri.toString());
        }
    }

    public void setUri(String uriString) {
        if (uriString != null) {
            try {
                setUri(new URI(uriString));
            } catch (URISyntaxException e) {
                // ignore
            }
        } else {
            setUri((URI) null);
        }
    }

    private class MouseHandler extends MouseInputAdapter {

        @Override
        public void mouseEntered(MouseEvent e) {
            oldText = getText();
            if (oldText != null) {
                setText("<html><u>" + oldText + "</u></html>");
            }
            oldCursor = getCursor();
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setText(oldText);
            setCursor(oldCursor);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            try {
                Desktop.getDesktop().browse(uri);
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(UriLabel.this, "Failed to open URL:\n" + uri + ":\n" + e1.getMessage(), "I/O Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
