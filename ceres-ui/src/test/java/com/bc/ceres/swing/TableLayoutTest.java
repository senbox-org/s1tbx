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

import org.junit.*;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.*;

public class TableLayoutTest {

    @Test
    public void testCoolnessOfTableLayout() {
        TableLayout layout = new TableLayout(3);
        assertEquals(3, layout.getColumnCount());
    }

    public TableLayoutTest() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        showFrame(createPanel1());
        showFrame(createPanel2());
    }


    private static JPanel createPanel1() {
        final TableLayout layout = new TableLayout(3);

        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);

        layout.setTablePadding(2, 2);
        layout.setColumnWeightX(0, 0.1);
        layout.setColumnWeightX(1, 1.0);
        layout.setColumnWeightX(2, 0.1);
        layout.setCellColspan(2, 0, 3);
        layout.setCellColspan(3, 0, 3);

        final JPanel panel = new JPanel(layout);

        panel.add(new JLabel("Wavelength:"));
        panel.add(new JTextField(16));
        panel.add(new JLabel("nm"));

        panel.add(new JLabel("Bandwidth:"));
        panel.add(new JTextField(16));
        panel.add(new JLabel("nm"));

        panel.add(new JCheckBox("Use no-data value"));

//        panel.add(new JLabel("Expression:"), new TableLayout.Cell(0, 3));
//        panel.add(new JTextArea(4, 10), new TableLayout.Cell(1, 3, 2, 1));

        panel.add(new JCheckBox("Use expression whenever it make sense"));

        System.out.println("layout = " + layout);
        return panel;
    }

    private static JPanel createPanel2() {
        final TableLayout layout = new TableLayout(2);

        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setRowFill(0, TableLayout.Fill.BOTH);
        layout.setRowFill(1, TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(2, 2);
        layout.setColumnWeightX(0, 0.5);
        layout.setColumnWeightX(1, 0.5);
        layout.setRowWeightX(0, 1.0);
        layout.setRowWeightY(0, 1.0);
        layout.setRowWeightY(1, 0.0);

        final JPanel panel = new JPanel(layout);
        panel.add(new JScrollPane(new JList<>(new Object[]{"Ernie", "Bibo", "Bert"})));
        panel.add(new JScrollPane(new JList<>(new Object[]{"Ernie", "Bibo", "Bert"})));
        JButton comp = new JButton("Start");
        comp.setMinimumSize(null);
        comp.setMaximumSize(null);
        comp.setPreferredSize(null);
        JPanel p = new JPanel();
        p.add(comp);
        panel.add(p);

        //panel.add(new JCheckBox("Report"));
//        panel.add(new JPanel());
//        panel.add(new JPanel());

        System.out.println("layout = " + layout);
        return panel;
    }


    private static void showFrame(JPanel panel) {
        final JFrame frame = new JFrame(TableLayoutTest.class.getName());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }

}
