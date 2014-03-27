package org.esa.beam.visat.actions.imgfilter;

import org.esa.beam.visat.actions.imgfilter.model.Filter;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;


public class FilterKernelCanvas extends JPanel implements Filter.Listener {

    private final Filter filter;
    private double fillValue;
    private double maxAbsElementValue;

    public FilterKernelCanvas(Filter filter) {
        this.filter = filter;
        this.fillValue = 0;
        setFont(new Font("Verdana", Font.PLAIN, 10));
        this.filter.addListener(this);
        maxAbsElementValue = -1;
    }

    public Filter getFilter() {
        return filter;
    }

    public double getFillValue() {
        return fillValue;
    }

    public void setFillValue(double fillValue) {
        double fillValueOld = this.fillValue;
        this.fillValue = fillValue;
        firePropertyChange("fillValue", fillValueOld, fillValue);
    }

    @Override
    public void filterModelChanged(Filter filter) {
        updateMaxAbsElementValue();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(
                Math.max(filter.getKernelWidth() * 16, 200),
                Math.max(filter.getKernelHeight() * 16, 200));
    }

    public int getKernelElementIndex(int x, int y) {
        Insets insets = getInsets();
        int w = getWidth() - (insets.left + insets.right);
        int h = getHeight() - (insets.top + insets.bottom);
        int kernelWidth = filter.getKernelWidth();
        int kernelHeight = filter.getKernelHeight();
        int cellSize = Math.min(w / kernelWidth, h / kernelHeight);
        int x0 = insets.left + (w - cellSize * kernelWidth) / 2;
        int y0 = insets.top + (h - cellSize * kernelHeight) / 2;
        int i = (x - x0) / cellSize;
        int j = (y - y0) / cellSize;
        if (i >= 0 && i < kernelWidth && j >= 0 && j < kernelHeight) {
            return j * kernelWidth + i;
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (filter == null) {
            return;
        }

        if (maxAbsElementValue < 0) {
            updateMaxAbsElementValue();
        }

        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        Insets insets = getInsets();
        int w = getWidth() - (insets.left + insets.right);
        int h = getHeight() - (insets.top + insets.bottom);
        int kernelWidth = filter.getKernelWidth();
        int kernelHeight = filter.getKernelHeight();

        int cellSize = Math.min(w / kernelWidth, h / kernelHeight);
        int x0 = insets.left + (w - cellSize * kernelWidth) / 2;
        int y0 = insets.top + (h - cellSize * kernelHeight) / 2;
        for (int j = 0; j < kernelHeight; j++) {
            int y = y0 + j * cellSize;
            for (int i = 0; i < kernelWidth; i++) {
                int x = x0 + i * cellSize;
                paintKernelElement(g, x, y, cellSize, cellSize, j * kernelWidth + i);
            }
        }

        g.setColor(Color.GRAY);
        g.drawRect(x0, y0, kernelWidth * cellSize, kernelHeight * cellSize);
    }

    private void paintKernelElement(Graphics g, int x, int y, int cw, int ch, int index) {
        if (filter.getOperation() == Filter.Operation.CONVOLVE) {
            paintConvolutionElement(g, x, y, cw, ch, index);
        } else {
            paintStructuringElement(g, x, y, cw, ch, index);
        }
    }

    private void paintConvolutionElement(Graphics g, int x, int y, int cw, int ch, int index) {

        double value = filter.getKernelElement(index);

        Color color = Color.WHITE;
        if (value > 0) {
            int comp = 255 - (int) (maxAbsElementValue == 0 ? 0 : (100 * value) / maxAbsElementValue);
            color = new Color(255, comp, comp);
        } else if (value < 0) {
            int comp = 255 - (int) (maxAbsElementValue == 0 ? 0 : (100 * -value) / maxAbsElementValue);
            color = new Color(comp, comp, 255);
        }

        g.setColor(color);
        g.fillRect(x, y, cw, ch);

        int cellSize = Math.min(cw, ch);
        //System.out.println("cellSize = " + cellSize);
        if (cellSize >= 4) {
            g.setColor(Color.GRAY);
            g.drawRect(x, y, cw, ch);
            if (isOffsetIndex(index)) {
                g.setColor(Color.GRAY);
                g.drawRect(x + 1, y + 1, cw - 2, ch - 2);
            }
        }

        if (cellSize > 12) {
            float fontSize = Math.min(16.0f, 0.5f * cellSize);
            String text = value == (int) value ? String.valueOf((int) value) : String.valueOf(value);
            g.setFont(getFont().deriveFont(fontSize));
            Rectangle2D bounds = g.getFontMetrics().getStringBounds(text, g);
            int x1 = x + (int) (0.5 * cw - 0.5 * bounds.getWidth());
            int y1 = y + (int) (ch - 0.5 * bounds.getHeight());
            g.setColor(filter.isEditable() ? getForeground() : Color.DARK_GRAY);
            g.drawString(text, x1, y1);
        }
    }

    private boolean isOffsetIndex(int index) {
        return index == filter.getKernelOffsetY() * filter.getKernelWidth() + filter.getKernelOffsetX();
    }

    private void paintStructuringElement(Graphics g, int x, int y, int cw, int ch, int index) {
        g.setColor(filter.getKernelElement(index) != 0.0 ? Color.DARK_GRAY : Color.WHITE);
        g.fillRect(x, y, cw, ch);
        int cellSize = Math.min(cw, ch);
        //System.out.println("cellSize = " + cellSize);
        if (cellSize >= 4) {
            g.setColor(Color.GRAY);
            g.drawRect(x, y, cw, ch);
            if (isOffsetIndex(index)) {
                g.setColor(Color.GRAY);
                g.drawRect(x + 1, y + 1, cw - 2, ch - 2);
            }
        }
    }

    private void updateMaxAbsElementValue() {
        maxAbsElementValue = 0;
        if (filter != null) {
            for (double v : filter.getKernelElements()) {
                maxAbsElementValue = Math.max(maxAbsElementValue, Math.abs(v));
            }
        }
    }
}
