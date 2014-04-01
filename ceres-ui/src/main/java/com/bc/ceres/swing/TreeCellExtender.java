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

package com.bc.ceres.swing;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TreeCellExtender {
    public final JTree tree;
    private JWindow cellExtender;
    private int offset;
    private int row;
    private boolean active;
    private final MouseHandler mouseHandler;

    public static TreeCellExtender equip(JTree tree) {
        final TreeCellExtender extender = new TreeCellExtender(tree);
        extender.setActive(true);
        return extender;
    }

    public TreeCellExtender(JTree tree) {
        this.tree = tree;
        unsetRow();
        this.mouseHandler = new MouseHandler();
        this.active = false;
    }

    public JTree getTree() {
        return tree;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        final boolean oldValue = this.active;
        if (oldValue) {
            tree.removeMouseListener(mouseHandler);
            tree.removeMouseMotionListener(mouseHandler);
            disposeCellExtender();
            unsetRow();
        }
        this.active = active;
        if (this.active) {
            tree.addMouseListener(mouseHandler);
            tree.addMouseMotionListener(mouseHandler);
            hideCellExtender();
            unsetRow();
        }
    }

    private void showOrHideCellExtender(Point point) {
        final int oldRow = row;
        row = tree.getRowForLocation(point.x, point.y);
        if (row == oldRow) {
            return;
        }

        hideCellExtender();
        if (row < 0) {
            return;
        }

        TreePath path = tree.getPathForRow(row);
        if(path == null) {
            return;
        }
        Rectangle rowRect = tree.getPathBounds(path);
        if(rowRect == null) {
            return;
        }

        Rectangle viewRect;
        if (tree.getParent() instanceof JViewport) {
            viewRect = ((JViewport) tree.getParent()).getViewRect();
        } else {
            viewRect = tree.getBounds();
        }

        int rx1 = rowRect.x;
        int ry1 = rowRect.y;
        int rx2 = rowRect.x + rowRect.width - 1;
        int ry2 = rowRect.y + rowRect.height - 1;

        // int vx1 = viewRect.x;
        int vy1 = viewRect.y;
        int vx2 = viewRect.x + viewRect.width - 1;
        int vy2 = viewRect.y + viewRect.height - 1;

        boolean cellExtenderVisible = rx2 > vx2 && ry1 >= vy1 && ry2 <= vy2;
        if (!cellExtenderVisible) {
            return;
        }

        offset = vx2 - rx1;

        int wx = rx1 < vx2 ? vx2 : rx1;
        int wy = rowRect.y - 1;
        int ww = rx2 - wx + 2;
        int wh = rowRect.height + 2;
        Rectangle windowRect = new Rectangle(wx, wy, ww, wh);
        if (windowRect.isEmpty()) {
            return;
        }

        showCellExtender(windowRect);
    }

    private void showCellExtender(Rectangle windowRect) {
        final Rectangle screenBounds = convertToScreen(tree, windowRect);
        if (cellExtender == null) {
            cellExtender = new JWindow(SwingUtilities.getWindowAncestor(tree));
            cellExtender.getContentPane().add(new CellExtenderPanel(), BorderLayout.CENTER);
        }
        cellExtender.setBounds(screenBounds);
        cellExtender.setVisible(true);
    }

    private void hideCellExtender() {
        if (cellExtender != null) {
            cellExtender.setVisible(false);
        }
    }

    private void disposeCellExtender() {
        if (cellExtender != null) {
            cellExtender.dispose();
            cellExtender = null;
        }
    }

    private void unsetRow() {
        row = -1;
    }

    private static Rectangle convertToScreen(Component c, Rectangle r) {
        Point p = new Point(r.getLocation());
        SwingUtilities.convertPointToScreen(p, c);
        return new Rectangle(p.x, p.y, r.width, r.height);
    }

    private class MouseHandler extends MouseAdapter {
        private Point point;

        @Override
        public void mouseMoved(MouseEvent e) {
            Point point = e.getPoint();
            if (!point.equals(this.point)) {
                this.point = point;
                showOrHideCellExtender(this.point);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            hideCellExtender();
            unsetRow();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // Check, because mouseEntered is also called if cellExtender becomes invisible
            // and cursor is over both, tree and cellExtender.
            if (e.getPoint().equals(this.point)) {
                hideCellExtender();
                unsetRow();
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // Check, because mouseEntered is also called if cellExtender becomes visible
            // and cursor is over both, tree and cellExtender.
            if (e.getPoint().equals(this.point)) {
                disposeCellExtender();
                unsetRow();
            }
        }
    }

    private class CellExtenderPanel extends JPanel {

        public CellExtenderPanel() {
            setBorder(new CellExtenderBorder());
            setBackground(tree.getBackground());
        }

        @Override
        protected void paintComponent(Graphics g) {
            final int width = getWidth();
            final int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }

            TreePath path = tree.getPathForRow(row);
            if (path == null) {
                return;
            }

            Rectangle bounds = getBounds();
            Color color = g.getColor();
            g.setColor(getBackground());
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            g.setColor(color);

            TreeCellRenderer renderer = tree.getCellRenderer();
            Component rendererComponent = renderer.getTreeCellRendererComponent(tree,
                                                                                path.getLastPathComponent(),
                                                                                tree.isPathSelected(path),
                                                                                tree.isExpanded(path),
                                                                                true, // todo - leaf ?
                                                                                row,
                                                                                false); // has focus ?
            rendererComponent.setSize(1024, height);

            Graphics g2 = g.create(0, 0, width, height);
            g2.translate(-offset, 0);
            rendererComponent.paint(g2);
            g2.dispose();
        }
    }

    private static class CellExtenderBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 0, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = 0;
            insets.top = insets.right = insets.bottom = 1;
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x1, int y1, int width, int height) {
            final int x2 = x1 + width - 1;
            final int y2 = y1 + height - 1;
            final Color color = g.getColor();
            g.setColor(Color.DARK_GRAY);
            g.drawLine(x1, y1, x2, y1);
            g.drawLine(x1, y2, x2, y2);
            g.drawLine(x2, y1, x2, y2);
            g.setColor(color);
        }
    }

}
