/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package com.bc.swing.dock;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import com.bc.swing.TitledPane;

/**
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class FloatingWindow extends Window implements FloatingComponent {

    private static final long serialVersionUID = -5158606919190241950L;

    public final static int BORDER_SIZE = 4;
    public final static int EDGE_SIZE = 3 * BORDER_SIZE;

    private static final FloatingComponentFactory _factory = new Factory();

    private TitledPane _titledPane;
    private DockableComponent _originator;

    private static Cursor[] _resizeCursors;
    private JButton _closeButton;

    static {
        _resizeCursors = new Cursor[9];
        _resizeCursors[SwingConstants.CENTER] = Cursor.getDefaultCursor();
        _resizeCursors[SwingConstants.NORTH] = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
        _resizeCursors[SwingConstants.SOUTH] = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
        _resizeCursors[SwingConstants.WEST] = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
        _resizeCursors[SwingConstants.EAST] = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        _resizeCursors[SwingConstants.NORTH_WEST] = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
        _resizeCursors[SwingConstants.NORTH_EAST] = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
        _resizeCursors[SwingConstants.SOUTH_WEST] = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
        _resizeCursors[SwingConstants.SOUTH_EAST] = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
    }

    public static Cursor getResizeCursor(int resizeMode) {
        return _resizeCursors[resizeMode];
    }

    public FloatingWindow(Frame owner) {
        super(owner);
        createUI();
    }

    public FloatingWindow(Window owner) {
        super(owner);
        createUI();
    }

    public static FloatingComponentFactory getFactory() {
        return _factory;
    }

    /////////////////////////////////////////////////////////////////////////
    // FloatingComponent interface implementation

    public Icon getIcon() {
        return _titledPane.getIcon();
    }

    public void setIcon(Icon icon) {
        _titledPane.setIcon(icon);
    }

    public String getTitle() {
        return _titledPane.getTitle();
    }

    public void setTitle(String title) {
        _titledPane.setTitle(title);
    }

    public Component getContent() {
        return _titledPane.getContent();
    }

    public void setContent(Component content) {
        _titledPane.setContent(content);
    }

    public DockableComponent getOriginator() {
        return _originator;
    }

    public void setOriginator(DockableComponent floatableComponent) {
        _originator = floatableComponent;
    }

    public void close() {
        processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        setVisible(false);
        remove(_titledPane);
        dispose();
        processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSED));
    }

    public void setClosable(boolean closable) {
        _titledPane.getTitleBar().remove(_closeButton);
    }

    /////////////////////////////////////////////////////////////////////////
    // Private

    private void createUI() {

        MouseInputListener windowMouseHandler = new WindowMouseHandler();
        MouseInputListener titleBarMouseHandler = new TitleBarMouseHandler();

        JButton dockButton = createDockButton();
        _closeButton = createCloseButton();

        _titledPane = new TitledPane(" ");
        _titledPane.getTitleBar().add(dockButton);
        _titledPane.getTitleBar().add(_closeButton);
        _titledPane.getTitleBar().addMouseListener(titleBarMouseHandler);
        _titledPane.getTitleBar().addMouseMotionListener(titleBarMouseHandler);
        _titledPane.setBorder(new TitleBorder());
//        _titledPane.addMouseListener(windowMouseHandler);
//        _titledPane.addMouseMotionListener(windowMouseHandler);
        _titledPane.addMouseListener(windowMouseHandler);
        _titledPane.addMouseMotionListener(windowMouseHandler);
        setLayout(new BorderLayout());
        add(_titledPane, BorderLayout.CENTER);
        pack();
    }

    protected JButton createCloseButton() {
        return TitledPane.createTitleBarButton("close", "Close", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }

    protected JButton createDockButton() {
        return TitledPane.createTitleBarButton("dock", "Dock", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getOriginator().setDocked(true);
            }
        });
    }

    protected Point convertPointToScreen(MouseEvent e) {
        final Point point = e.getPoint();
        SwingUtilities.convertPointToScreen(point, e.getComponent());
        return point;
    }

    private class WindowMouseHandler extends MouseInputAdapter {

        private Point _point;
        private int _resizeMode;
        private Cursor _currentCursor;

        @Override
        public void mousePressed(MouseEvent e) {
            _point = convertPointToScreen(e);
            _resizeMode = getResizeMode(e);
            setResizeCursor(_resizeMode);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            _point = null;
            _resizeMode = 0;
            setResizeCursor(_resizeMode);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (_resizeMode > 0) {
                mouseDragged(e);
            } else {
                setResizeCursor(getResizeMode(e));
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (_resizeMode > 0) {
                Point p = convertPointToScreen(e);
                int dx = p.x - _point.x;
                int dy = p.y - _point.y;
                _point = p;
                if (dx != 0 || dy != 0) {
                    resizeWindow(dx, dy);
                }
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            setResizeCursor(getResizeMode(e));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setResizeCursor(0);
        }

        private void setResizeCursor(int resizeMode) {
            setCurrentCursor(getResizeCursor(resizeMode));
        }

        private void setCurrentCursor(Cursor currentCursor) {
            if (currentCursor != _currentCursor) {
                _currentCursor = currentCursor;
                setCursor(_currentCursor);
            }
        }

        private int getResizeMode(MouseEvent e) {
            int w = getWidth();
            int h = getHeight();
            int a = EDGE_SIZE;
            int b = BORDER_SIZE;
            Point p = e.getPoint();
            if (p.x < a && p.y < a) {
                return SwingConstants.NORTH_WEST;
            } else if (p.x > w - a && p.y < a) {
                return SwingConstants.NORTH_EAST;
            } else if (p.x < a && p.y > h - a) {
                return SwingConstants.SOUTH_WEST;
            } else if (p.x > w - a && p.y > h - a) {
                return SwingConstants.SOUTH_EAST;
            } else if (p.y < b) {
                return SwingConstants.NORTH;
            } else if (p.y > h - b) {
                return SwingConstants.SOUTH;
            } else if (p.x < b) {
                return SwingConstants.WEST;
            } else if (p.x > w - b) {
                return SwingConstants.EAST;
            } else {
                return SwingConstants.CENTER;
            }
        }

        private void resizeWindow(int dx, int dy) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            final int rm = _resizeMode;
            if (rm == SwingConstants.NORTH) {
                y += dy;
                h -= dy;
            } else if (rm == SwingConstants.SOUTH) {
                h += dy;
            } else if (rm == SwingConstants.WEST) {
                x += dx;
                w -= dx;
            } else if (rm == SwingConstants.EAST) {
                w += dx;
            } else if (rm == SwingConstants.NORTH_WEST) {
                x += dx;
                y += dy;
                w -= dx;
                h -= dy;
            } else if (rm == SwingConstants.NORTH_EAST) {
                y += dy;
                w += dx;
                h -= dy;
            } else if (rm == SwingConstants.SOUTH_WEST) {
                x += dx;
                w -= dx;
                h += dy;
            } else if (rm == SwingConstants.SOUTH_EAST) {
                w += dx;
                h += dy;
            }
            setBounds(x, y, w, h);
            invalidate();
            validate();
            repaint();
        }
    }

    private class TitleBarMouseHandler extends MouseInputAdapter {

        private Point _point;

        @Override
        public void mousePressed(MouseEvent e) {
            _point = convertPointToScreen(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            _point = null;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (_point != null) {
                mouseDragged(e);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point p = convertPointToScreen(e);
            int dx = p.x - _point.x;
            int dy = p.y - _point.y;
            _point = p;
            if (dx != 0 || dy != 0) {
                repositionWindow(dx, dy);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setCursor(Cursor.getDefaultCursor());
        }

        private void repositionWindow(int dx, int dy) {
            setLocation(getX() + dx, getY() + dy);
        }
    }

    private static class TitleBorder implements Border {

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(c.getBackground());
            g.draw3DRect(x, y, width - 1, height - 1, true);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    private static class Factory implements FloatingComponentFactory {

        private Factory() {
        }

        public FloatingComponent createFloatingComponent(Window owner) {
            return new FloatingWindow(owner);
        }
    }

}
