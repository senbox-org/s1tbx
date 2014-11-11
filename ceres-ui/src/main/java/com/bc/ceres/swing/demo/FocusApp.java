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

package com.bc.ceres.swing.demo;

import com.bc.ceres.glayer.swing.AdjustableViewScrollPane;
import com.bc.ceres.grender.AdjustableView;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.DefaultViewport;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Comparator;

public class FocusApp {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // ok
        }

        JPanel panel = new MyJPanel();
        panel.setPreferredSize(new Dimension(512, 512));

        AdjustableViewScrollPane viewScrollPane = new AdjustableViewScrollPane(panel);
        JFrame jFrame = new JFrame("x");
        jFrame.add(viewScrollPane);


        JTree tree1 = new JTree(new Object[]{"Aaaaaaaaaaaaa", "B", "C"});
        JTree tree2 = new JTree(new Object[]{"Xxxxxxxxxxxxx", "Y", "Z"});
        tree1.setCellRenderer(new MyDefaultTreeCellRenderer());
        tree2.setCellRenderer(new MyDefaultTreeCellRenderer());
        tree1.addFocusListener(new MyFocusListener());
        tree2.addFocusListener(new MyFocusListener());
        showFrame(tree1);
        showFrame(tree2);

        JList list1 = new JList(new Object[]{"Aaaaaaaaaaaaa", "B", "C"});
        JList list2 = new JList(new Object[]{"Xxxxxxxxxxxxx", "Y", "Z"});
        list1.setCellRenderer(new MyDefaultListCellRenderer());
        list2.setCellRenderer(new MyDefaultListCellRenderer());
        list1.addFocusListener(new MyFocusListener());
        list2.addFocusListener(new MyFocusListener());
        showFrame(list1);
        showFrame(list2);

        //showUIDefaults();
    }

    private static void showUIDefaults() {
        UIDefaults uiDefaults = UIManager.getLookAndFeelDefaults();
        Object[] keys = uiDefaults.keySet().toArray();
        Arrays.sort(keys, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });

        Object[][] tableData = new Object[keys.length][2];
        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            Object value = uiDefaults.get(key);
            System.out.println(key + " = " + value);
            tableData[i][0] = key.toString();
            tableData[i][1] = value;
        }

        JTable table = new JTable(tableData, new String[]{"Key", "Value"});

        table.getColumnModel().getColumn(1).setCellRenderer(new MyDefaultTableCellRenderer());

        JFrame jFrame = new JFrame("LookAndFeelDefaults");
        jFrame.add(new JScrollPane(table));
        jFrame.setBounds(100, 100, 400, 400);
        jFrame.setVisible(true);
    }

    static int frameNo = 1;

    private static void showFrame(JComponent component) {
        final JFrame frame = new JFrame("FocusApp-" + frameNo++);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(component, BorderLayout.CENTER);
        frame.setBounds(10, 10, 400, 100);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
            }
        });
    }

    private static class MyDefaultListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
            Window window = SwingUtilities.getWindowAncestor(list);
            if (window != null && !window.isFocused()) {
                hasFocus = isSelected;
                isSelected = false;
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);

        }
    }

    private static class MyDefaultTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Window window = SwingUtilities.getWindowAncestor(tree);
            if (window != null && !window.isFocused()) {
                hasFocus = isSelected;
                isSelected = false;
            }
            return super.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, hasFocus);
        }
    }

    private static class MyFocusListener implements FocusListener {
        @Override
        public void focusGained(FocusEvent e) {
            e.getComponent().repaint();
        }

        @Override
        public void focusLost(FocusEvent e) {
            e.getComponent().repaint();
        }
    }

    private static class MyDefaultTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setFont(table.getFont());
            setBackground(Color.WHITE);
            setText(value != null ? value.toString() : "null");
            setBorder(null);
            setIcon(null);
            if (value instanceof javax.swing.plaf.UIResource) {
                if (value instanceof Color) {
                    Color color = (Color) value;
                    setBackground(color);
                    setBorder(new LineBorder(Color.WHITE, 2));
                } else if (value instanceof Font) {
                    Font font = (Font) value;
                    setBackground(Color.WHITE);
                    setFont(font);
                } else if (value instanceof Icon) {
                    Icon icon = (Icon) value;
                    // setIcon(icon);
                } else if (value instanceof Border) {
                    Border border = (Border) value;
                    //setBackground(Color.GRAY);
                    //setBorder(new CompoundBorder(new EmptyBorder(2, 2, 2, 2), border));
                    //setText("");
                }
            }
            return this;
        }
    }

    private static class MyJPanel extends JPanel implements AdjustableView {
        private DefaultViewport viewport;

        public MyJPanel() {
            super(new BorderLayout());
            viewport = new DefaultViewport();
        }

        /**
         * @return The maximum visible model bounds in model coordinates.
         */
        @Override
        public Rectangle2D getMaxVisibleModelBounds() {
            return null;
        }

        /**
         * @return The default zoom factor.
         *
         * @see com.bc.ceres.grender.Viewport#getZoomFactor()
         */
        @Override
        public double getDefaultZoomFactor() {
            return 0;
        }

        /**
         * @return The minimum zoom factor.
         *
         * @see com.bc.ceres.grender.Viewport#getZoomFactor()
         */
        @Override
        public double getMinZoomFactor() {
            return 0.1;
        }

        /**
         * @return The maximum zoom factor.
         *
         * @see com.bc.ceres.grender.Viewport#getZoomFactor()
         */
        @Override
        public double getMaxZoomFactor() {
            return 10;
        }

        /**
         * @return The viewport.
         */
        @Override
        public Viewport getViewport() {
            return viewport;
        }
    }
}
