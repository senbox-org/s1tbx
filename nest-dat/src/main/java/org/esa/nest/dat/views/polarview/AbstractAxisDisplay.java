package org.esa.nest.dat.views.polarview;

import org.esa.beam.visat.VisatApp;

import java.awt.*;

abstract class AbstractAxisDisplay {

    Graphics g = null;
    private Color backgroundColor;

    public static final int MAX_POINTS = 16380;

    AbstractAxisDisplay() {
        backgroundColor = VisatApp.getApp().getDesktopPane().getBackground();
    }

    public Color getBackground() {
        return backgroundColor;
    }

    public void setBackground(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    abstract void drawLine(int i, int j, int k, int l);

    abstract void drawTitle(String title, Font titleFont, int length);

    abstract Rectangle getTickName(String s, int i, int j, int k, int l, FontMetrics fontmetrics);

    abstract void drawTickName(String s, int i, int j, int k, int l, int i1, int j1);

    abstract int maxTickSize(String[] tickNames, int tickCount, Font font);

    void setGraphics(Graphics g) {
        this.g = g;
    }

    void drawTick(int tickPixel, int tickLength) {
        drawLine(tickPixel, 0, tickPixel, tickLength);
    }

    void drawMultiLineTickName(String name, int tickPixel, int tickLength, FontMetrics fm) {
        final int leading = Math.max(fm.getLeading(), 3);
        int lineCount = 0;
        int p = 0;
        do {
            p = name.indexOf('\n', p);
            lineCount++;
        } while (++p > 0);
        int f = 0;
        for (int i = 1; i <= lineCount; i++) {
            p = name.indexOf('\n', f);
            if (p < 0)
                p = name.length();
            final String text = name.substring(f, p);
            final Rectangle box = getTickName(text, tickPixel, tickLength, i, lineCount, fm);
            final Color col = g.getColor();
            g.setColor(backgroundColor);
            g.fillRect(box.x - leading, box.y, box.width + 2 * leading, box.height + leading);
            g.setColor(col);
            g.drawString(text, box.x, box.y + fm.getAscent());
            f = p + 1;
        }
    }

    Dimension getTickLabelBounds(String name, Font font) {
        final FontMetrics fm = g.getFontMetrics(font);
        int maxWidth = 0;
        int height = 0;
        int p;
        int f = 0;
        do {
            p = name.indexOf('\n', f);
            int n;
            if (p < 0)
                n = name.length();
            else
                n = p;
            maxWidth = Math.max(fm.stringWidth(name.substring(f, n)), maxWidth);
            height += fm.getAscent();
            f = p + 1;
        } while (f > 0);
        return new Dimension(maxWidth, height);
    }

    public static class XAxisDisplay extends AbstractAxisDisplay {
        final boolean isBottomLeft;

        public XAxisDisplay(boolean bottomLeft) {
            isBottomLeft = bottomLeft;
        }

        @Override
        void drawLine(int x1, int y1, int x2, int y2) {
            if (isBottomLeft)
                g.drawLine(x1, -y1, x2, -y2);
            else
                g.drawLine(x1, y1, x2, y2);
        }

        @Override
        void drawTitle(String title, Font titleFont, int length) {
            if (title != null) {
                final FontMetrics tfm = g.getFontMetrics(titleFont);
                g.drawString(title, (length - tfm.stringWidth(title)) / 2, tfm.getHeight() * 3);
            }
        }

        @Override
        Rectangle getTickName(String name, int tickPixel, int tickLength, int lineNumber, int lineCount, FontMetrics fm) {
            final int height = fm.getAscent();
            final int width = fm.stringWidth(name);
            int x = tickPixel;
            int y = tickLength >= 0 ? 0 : tickLength;
            x -= width / 2;
            if (isBottomLeft) {
                y -= Math.round((float) height * ((float) lineNumber + 0.5F));
                y = -y;
            } else {
                y -= Math.round((float) height * ((float) lineNumber - 0.5F));
            }
            return new Rectangle(x, y - height, width, height);
        }

        @Override
        void drawTickName(String name, int tickPixel, int tickLength, int width, int height, int lineNumber, int lineCount) {
            int x = tickPixel;
            int y = tickLength >= 0 ? 0 : tickLength;
            x -= width / 2;
            if (isBottomLeft) {
                y -= Math.round((float) height * ((float) lineNumber + 0.5F));
                g.drawString(name, x, -y);
            } else {
                y -= Math.round((float) height * ((float) lineNumber - 0.5F));
                g.drawString(name, x, y);
            }
        }

        @Override
        int maxTickSize(String[] tickNames, int tickCount, Font font) {
            int maxWidth = 0;
            for (int i = 0; i < tickCount; i++) {
                maxWidth = Math.max(getTickLabelBounds(tickNames[i], font).width + 6, maxWidth);
            }
            return maxWidth;
        }
    }

    public static class YAxisDisplay extends AbstractAxisDisplay {
        final boolean isBottomLeft;

        public YAxisDisplay(boolean bottomLeft) {
            isBottomLeft = bottomLeft;
        }

        @Override
        void drawLine(int x1, int y1, int x2, int y2) {
            if (isBottomLeft)
                g.drawLine(y1, -x1, y2, -x2);
            else
                g.drawLine(-y1, -x1, -y2, -x2);
        }

        @Override
        void drawTitle(String title, Font titleFont, int length) {
            if (title != null) {
                final FontMetrics fm = g.getFontMetrics(titleFont);
                final int y = length + fm.getHeight();
                final int w = fm.stringWidth(title);
                final int x = isBottomLeft ? -w / 2 : -(w / 2);
                g.drawString(title, x, -y);
            }
        }

        @Override
        Rectangle getTickName(String name, int tickPixel, int tickLength, int lineNumber, int lineCount, FontMetrics fm) {
            final int height = fm.getAscent();
            final int width = fm.stringWidth(name);
            int y = tickPixel;
            int x = tickLength >= 0 ? 0 : tickLength;
            y -= height * lineNumber - (height * lineCount) / 2;
            x -= height / 2;
            if (isBottomLeft) {
                x -= width;
                x = -x;
            }
            return new Rectangle(-x, -y - height, width, height);
        }

        @Override
        void drawTickName(String name, int tickPixel, int tickLength, int width, int height, int lineNumber, int lineCount) {
            int y = tickPixel;
            int x = tickLength >= 0 ? 0 : tickLength;
            y -= height * lineNumber - (height * lineCount) / 2;
            x -= height / 2;
            if (isBottomLeft) {
                x -= width;
                g.drawString(name, x, -y);
            } else {
                g.drawString(name, -x, -y);
            }
        }

        @Override
        int maxTickSize(String[] tickNames, int tickCount, Font font) {
            int maxHeight = 0;
            for (int i = 0; i < tickCount; i++) {
                maxHeight = Math.max(getTickLabelBounds(tickNames[i], font).height + 6, maxHeight);
            }
            return maxHeight;
        }
    }

}
