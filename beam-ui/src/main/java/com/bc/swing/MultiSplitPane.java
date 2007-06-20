/*
 * $Id: MultiSplitPane.java,v 1.2 2006/10/12 15:53:02 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.bc.swing;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ComponentUI;

/**
 * A <code>MultiSplitPane</code> is an alternative to {@link javax.swing.JSplitPane}
 * when you need split multiple components in a row or column.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision: 1.2 $ $Date: 2006/10/12 15:53:02 $
 * @deprecated as of new BEAM 4 UI
 */
public class MultiSplitPane extends JPanel {

    private static final long serialVersionUID = -1955517608737635656L;

    public static final int HORIZONTAL = SwingConstants.HORIZONTAL;
    public static final int VERTICAL = SwingConstants.VERTICAL;

    private int orientation;
    private int dividerSize;
    private float[] dividerPositions;
    private Insets insets;

    public MultiSplitPane() {
        this(HORIZONTAL);
    }

    public MultiSplitPane(int orientation) {
        super(null);
        this.orientation = orientation;
        dividerSize = 5;
        insets = new Insets(0, 0, 0, 0);
        dividerPositions = new float[0];
        final MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
        invalidate();
        validate();
        repaint();
    }

    public int getDividerSize() {
        return dividerSize;
    }

    public void setDividerSize(int dividerSize) {
        this.dividerSize = dividerSize;
        invalidate();
        validate();
        repaint();
    }

    public float[] getDividerPositions() {
        return dividerPositions;
    }

    public void setDividerPositions(float[] dividerPositions) {
        this.dividerPositions = dividerPositions;
    }

    /**
     * If a border has been set on this component, returns the
     * border's insets; otherwise calls <code>super.getInsets</code>.
     *
     * @return the value of the insets property
     *
     * @see #setBorder
     */
    public Insets getInsets() {
        return super.getInsets(insets);
    }

    /**
     * If the minimum size has been set to a non-<code>null</code> value
     * just returns it.  If the UI delegate's <code>getMinimumSize</code>
     * method returns a non-<code>null</code> value then return that; otherwise
     * defer to the component's layout manager.
     *
     * @return the value of the <code>minimumSize</code> property
     *
     * @see #setMinimumSize
     * @see ComponentUI
     */
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return getSize(true);
    }

    /**
     * If the <code>preferredSize</code> has been set to a
     * non-<code>null</code> value just returns it.
     * If the UI delegate's <code>getPreferredSize</code>
     * method returns a non <code>null</code> value then return that;
     * otherwise defer to the component's layout manager.
     *
     * @return the value of the <code>preferredSize</code> property
     *
     * @see #setPreferredSize
     * @see ComponentUI
     */
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return getSize(false);
    }


    /**
     * Causes this container to lay out its components.  Most programs
     * should not call this method directly, but should invoke
     * the <code>validate</code> method instead.
     *
     * @see LayoutManager#layoutContainer
     * @see #setLayout
     * @see #validate
     * @since JDK1.1
     */
    public synchronized void doLayout() {
        final int n = getComponentCount();
        if (n == 1) {
            getComponent(0).setBounds(getInternalX(),
                                      getInternalY(),
                                      getInternalWidth(),
                                      getInternalHeight());
        } else if (n > 1) {
            if (dividerPositions.length != n) {
                dividerPositions = createDividerPositions(n);
            }
            int x1 = getInternalX();
            int y1 = getInternalY();
            if (getOrientation() == HORIZONTAL) {
                int ht = getInternalHeight();
                for (int i = 0; i < n; i++) {
                    int x2 = getDividerX(i);
                    getComponent(i).setBounds(x1, y1, x2 - x1, ht);
                    x1 = x2 + getDividerSize();
                }
            } else {
                int wt = getInternalWidth();
                for (int i = 0; i < n; i++) {
                    int y2 = getDividerY(i);
                    getComponent(i).setBounds(x1, y1, wt, y2 - y1);
                    y1 = y2 + getDividerSize();
                }
            }
        }
    }

    protected float[] createDividerPositions(final int n) {
        final float[] dividerPositions = new float[n];
        for (int i = 0; i < n; i++) {
            dividerPositions[i] = (i + 1.0f) / n;
        }
        return dividerPositions;
    }

    /////////////////////////////////////////////////////////////////////////
    // private

    private int getDividerX(int i) {
        return getInternalX() + Math.round(getDividerPos(i) * getInternalWidth());
    }

    private int getInternalX() {
        return getInsets().left;
    }

    private void setDividerX(int i, int x) {
        setDividerPos(i, ((float) x - getInternalX()) / getInternalWidth());
    }

    private int getDividerY(int i) {
        return getInternalY() + Math.round(getDividerPos(i) * getInternalHeight());
    }

    private int getInternalY() {
        return getInsets().top;
    }

    private void setDividerY(int i, int y) {
        setDividerPos(i, ((float) y - getInternalY()) / getInternalHeight());
    }

    private int getInternalWidth() {
        return getWidth() - (getInsets().left + getInsets().right);
    }

    private int getInternalHeight() {
        return getHeight() - (getInsets().top + getInsets().bottom);
    }

    private float getDividerPos(int i) {
        return dividerPositions[i];
    }

    private void setDividerPos(int i, float pos) {
        float delta;
        if (getOrientation() == HORIZONTAL) {
            delta = getDividerSize() / (float) getInternalWidth();
        } else {
            delta = getDividerSize() / (float) getInternalHeight();
        }
        if (i > 0) {
            if (pos < dividerPositions[i - 1] + delta) {
                pos = dividerPositions[i - 1] + delta;
            }
        }
        if (i < getComponentCount() - 1) {
            if (pos > dividerPositions[i + 1] - delta) {
                pos = dividerPositions[i + 1] - delta;
            }
        }
        if (pos < delta) {
            pos = delta;
        }
        if (pos > 1 - delta) {
            pos = 1 - delta;
        }
        dividerPositions[i] = pos;
        invalidate();
        validate();
        repaint();
    }

    private Dimension getSize(boolean minimum) {
        Dimension size = new Dimension();
        final int n = getComponentCount();
        for (int i = 0; i < n; i++) {
            final Component child = getComponent(i);
            Dimension childSize = minimum ? child.getMinimumSize() : child.getPreferredSize();
            if (childSize == null) {
                childSize = new Dimension(32, 32);
            }
            if (getOrientation() == HORIZONTAL) {
                size.width += childSize.width;
                if (i > 0) {
                    size.width += getDividerSize();
                }
                size.height = Math.max(size.height, childSize.height);
            } else {
                size.height += childSize.height;
                if (i > 0) {
                    size.height += getDividerSize();
                }
                size.width = Math.max(size.width, childSize.width);
            }
        }
        final Insets insets = getInsets();
        size.width += insets.left + insets.right;
        size.height += insets.top + insets.bottom;
        return size;
    }

    private class MouseHandler extends MouseInputAdapter {

        private int _dividerIndex;
        private Point _point;
        private Cursor _currentCursor;

        public MouseHandler() {
            _dividerIndex = -1;
        }

        public void mousePressed(MouseEvent e) {
            _dividerIndex = getDividerIndex(e);
            _point = e.getPoint();
        }

        public void mouseReleased(MouseEvent e) {
            _dividerIndex = -1;
            _point = null;
            setDefaultCursor();
        }

        public void mouseExited(MouseEvent e) {
            setDefaultCursor();
        }

        public void mouseDragged(MouseEvent e) {
            if (_dividerIndex >= 0) {
                if (getOrientation() == HORIZONTAL) {
                    int x0 = getDividerX(_dividerIndex);
                    int dx = e.getX() - _point.x;
                    setDividerX(_dividerIndex, x0 + dx);
                } else {
                    int y0 = getDividerY(_dividerIndex);
                    int dy = e.getY() - _point.y;
                    setDividerY(_dividerIndex, y0 + dy);
                }
                _point = e.getPoint();
                setDragCursor();
                e.consume();
            } else {
                setDefaultCursor();
            }
        }

        public void mouseMoved(MouseEvent e) {
            if (_dividerIndex >= 0) {
                mouseDragged(e);
                return;
            }
            int index = getDividerIndex(e);
            if (index >= 0) {
                setDragCursor();
            } else {
                setDefaultCursor();
            }
        }

        private void setDragCursor() {
            final int cursorType = getOrientation() == HORIZONTAL ? Cursor.W_RESIZE_CURSOR : Cursor.N_RESIZE_CURSOR;
            setCurrentCursor(Cursor.getPredefinedCursor(cursorType));
        }

        private void setDefaultCursor() {
            setCurrentCursor(Cursor.getDefaultCursor());
        }

        private void setCurrentCursor(Cursor currentCursor) {
            if (_currentCursor != currentCursor) {
                _currentCursor = currentCursor;
                setCursor(_currentCursor);
            }
        }

        private int getDividerIndex(MouseEvent e) {
            if (getComponentCount() > 1) {
                Rectangle dividerRect = new Rectangle();
                if (getOrientation() == HORIZONTAL) {
                    dividerRect.y = getInsets().top;
                    dividerRect.width = getDividerSize();
                    dividerRect.height = getInternalHeight();
                    for (int i = 0; i < getComponentCount(); i++) {
                        dividerRect.x = getDividerX(i);
                        if (dividerRect.contains(e.getX(), e.getY())) {
                            return i;
                        }
                    }
                } else {
                    dividerRect.x = getInsets().left;
                    dividerRect.width = getInternalWidth();
                    dividerRect.height = getDividerSize();
                    for (int i = 0; i < getComponentCount(); i++) {
                        dividerRect.y = getDividerY(i);
                        if (dividerRect.contains(e.getX(), e.getY())) {
                            return i;
                        }
                    }
                }
            }
            return -1;
        }
    }

}
