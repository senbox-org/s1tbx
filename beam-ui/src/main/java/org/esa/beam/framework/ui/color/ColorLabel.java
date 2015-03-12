package org.esa.beam.framework.ui.color;

import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.MouseInputAdapter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.event.MouseEvent;

/**
 * A component for displaying color values.
 *
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public class ColorLabel extends JComponent {
    private String displayName;
    private Color color;
    private boolean highlighted;
    private boolean hoverEnabled;
    private MouseInputAdapter hoverListener;

    public ColorLabel() {
        this(Color.WHITE);
    }

    public ColorLabel(Color color) {
        this(color, null);
    }

    public ColorLabel(Color color, String displayName) {
        if (color != null) {
            this.color = color;
        } else {
            this.color = ColorComboBox.TRANSPARENCY;
        }
        this.displayName = displayName;
        setPreferredSize(new Dimension(14, 14));
        setBorder(createEmptyBorder());
        updateText();
        hoverListener = new MouseHoverListener();
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        Color oldValue = this.color;
        if (color != null) {
            this.color = color;
        } else {
            this.color = ColorComboBox.TRANSPARENCY;
        }
        updateText();
        repaint();
        firePropertyChange("color", oldValue, this.color);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        String oldValue = this.displayName;
        this.displayName = displayName;
        updateText();
        firePropertyChange("displayName", oldValue, this.displayName);
    }

    public boolean isHoverEnabled() {
        return hoverEnabled;
    }

    public void setHoverEnabled(boolean hoverEnabled) {
        boolean oldValue = this.hoverEnabled;
        this.hoverEnabled = hoverEnabled;
        if (hoverEnabled) {
            addMouseListener(hoverListener);
        } else {
            removeMouseListener(hoverListener);
        }
        firePropertyChange("hoverEnabled", oldValue, this.hoverEnabled);
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public void setHighlighted(boolean highlighted) {
        boolean oldValue = this.highlighted;
        this.highlighted = highlighted;
        firePropertyChange("highlighted", oldValue, this.highlighted);
        if (highlighted) {
            setBorder(createHighlightedBorder());
        } else {
            setBorder(createEmptyBorder());
        }
    }

    private LineBorder createHighlightedBorder() {
        return new LineBorder(Color.BLUE, 1);
    }

    private Border createEmptyBorder() {
        return new EmptyBorder(1, 1, 1, 1);
    }

    private Color getColorBoxLineColor() {
        int a = color.getAlpha();
        Color borderColor;
        if (a < 127) {
            borderColor = Color.GRAY;
        } else {
            //int cMin = Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
            int cMax = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
            if (cMax < 127) {
                borderColor = Color.LIGHT_GRAY;
            } else {
                borderColor = Color.GRAY;
            }
        }
        return borderColor;
    }

    private void updateText() {
        String rgbText;
        if (color.getAlpha() != 255) {
            rgbText = String.format("%d,%d,%d,%d", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        } else {
            rgbText = String.format("%d,%d,%d", color.getRed(), color.getGreen(), color.getBlue());
        }
        String text;
        if (getDisplayName() != null) {
            text = String.format("%s (%s)", getDisplayName(), rgbText);
        } else {
            text = rgbText;
        }
        setToolTipText(text);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int x = getInsets().left;
        int y = getInsets().top;
        int w = getWidth() - (getInsets().left + getInsets().right) - 1;
        int h = getHeight() - (getInsets().top + getInsets().bottom) - 1;
        if (getColor().getAlpha() < 255) {
            drawChessboardBackground(g, x, y, w, h);
        }
        drawColorBox(g, x, y, w, h);
    }

    private void drawColorBox(Graphics g, int x, int y, int w, int h) {
        g.setColor(getColor());
        g.fillRect(x, y, w, h);
        g.setColor(getColorBoxLineColor());
        g.drawRect(x, y, w, h);
    }

    private void drawChessboardBackground(Graphics g, int x, int y, int w, int h) {
        int s = 8;
        int ni = w / s + 1;
        int nj = h / s + 1;
        Shape clip = g.getClip();
        g.setClip(x, y, w, h);
        for (int j = 0; j < nj; j++) {
            for (int i = 0; i < ni; i++) {
                g.setColor(i % 2 != j % 2 ? Color.WHITE : Color.LIGHT_GRAY);
                g.fillRect(x + i * s, y + j * s, s, s);
            }
        }
        g.setClip(clip);
    }

    private class MouseHoverListener extends MouseInputAdapter {

        @Override
        public void mouseExited(MouseEvent e) {
            setHighlighted(false);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            setHighlighted(true);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
        }
    }
}
