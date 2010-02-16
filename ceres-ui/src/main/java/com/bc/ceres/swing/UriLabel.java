package com.bc.ceres.swing;

import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * An {@link ActionLabel} which opens a browser when clicked.
 */
public class UriLabel extends ActionLabel {
    private URI uri;

    /**
     * Constructs an <code>UriLabel</code> instance.
     * The label is aligned against the leading edge of its display area,
     * and centered vertically.
     */
    public UriLabel() {
        this(null, null);
    }

    /**
     * Constructs an <code>UriLabel</code> instance with the specified URI.
     * The label is aligned against the leading edge of its display area,
     * and centered vertically.
     *
     * @param uri The URI to open in the browser.
     */
    public UriLabel(URI uri) {
        this(null, uri);
    }

    /**
     * Constructs an <code>UriLabel</code> instance with the specified text and URI.
     * The label is aligned against the leading edge of its display area,
     * and centered vertically.
     *
     * @param text The text to be displayed by the label.
     * @param uri  The URI to open in the browser.
     */
    public UriLabel(String text, URI uri) {
        super(text);
        setUri(uri);
        addActionListener(new ActionHandler());
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        URI oldUri = this.uri;
        if (oldUri != uri || oldUri != null && !oldUri.equals(uri)) {
            this.uri = uri;
            if (uri != null && (getText() == null || getText().length() == 0)) {
                setText(uri.toString());
            }
            firePropertyChange("uri", oldUri, uri);
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

    private void browseUri() {
        if (getUri() == null) {
            return;
        }
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(getUri());
            } catch (Throwable e) {
                JOptionPane.showMessageDialog(UriLabel.this, "Failed to open URL:\n" + getUri() + ":\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(UriLabel.this, "The desktop command 'browse' is not supported.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class ActionHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            browseUri();
        }
    }
}
