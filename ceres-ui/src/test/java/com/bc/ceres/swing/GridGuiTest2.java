package com.bc.ceres.swing;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GridGuiTest2 {
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame(Grid.class.getSimpleName());
        frame.getContentPane().add(createGridPanel(), BorderLayout.CENTER);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(340, 340);
        frame.setVisible(true);
    }

    static JPanel createGridPanel() {
        final Grid grid = new Grid(6, false);
        grid.getLayout().setTablePadding(4, 3);
        grid.getLayout().setTableAnchor(TableLayout.Anchor.BASELINE);
        grid.getLayout().setTableAnchor(TableLayout.Anchor.NORTHWEST);
        grid.getLayout().setColumnFill(2, TableLayout.Fill.HORIZONTAL);
        grid.getLayout().setColumnFill(3, TableLayout.Fill.HORIZONTAL);
        grid.getLayout().setColumnFill(4, TableLayout.Fill.HORIZONTAL);
        grid.getLayout().setColumnWeightX(2, 1.0);
        grid.getLayout().setColumnWeightX(3, 1.0);
        grid.getLayout().setColumnWeightX(4, 1.0);
        grid.setHeaderRow(
                /*1*/ new JLabel("<html><b>Agg.</b>"),
                /*2*/ new JLabel("<html><b>Source</b>"),
                /*3*/ new JLabel("<html><b>Targets</b>"),
                /*4*/ new JLabel("<html><b>Parameters</b>"),
                /*5*/ null
        );
        ListControlBar gridControlBar = ListControlBar.create(ListControlBar.HORIZONTAL, grid, new GridController(grid));

        final JCheckBox sel = new JCheckBox();
        sel.setToolTipText("Show/hide selection column");
        sel.setBorderPaintedFlat(true);
        sel.setBorderPainted(false);
        sel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                grid.setShowSelectionColumn(sel.isSelected());
            }
        });
        gridControlBar.add(sel, 0);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(new JScrollPane(grid), BorderLayout.CENTER);
        panel.add(gridControlBar, BorderLayout.SOUTH);
        return panel;
    }


    static class GridController extends ListControlBar.AbstractListController {

        final Grid grid;

        private GridController(Grid grid) {
            this.grid = grid;
        }

        @Override
        public boolean addRow(int index) {
            String[] AGGS = new String[]{"AVG", "AVG_ML", "PERCENTILE", "ON_MAX_SET", "MIN_MAX_SET", "COUNT"};
            int n = AGGS.length;
            final JButton button = new JButton("...");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int rowIndex = grid.findDataRowIndex(button);
                    JOptionPane.showMessageDialog(grid, "Editing row #" + rowIndex);
                }
            });
            grid.addDataRow(
                    /*1*/ new JLabel("<html><b>" + (AGGS[random(n)]) + "</b>"),
                    /*2*/ new JLabel("<html>" + genName()),
                    /*3*/ new JLabel("<html>" + genNames(1 + random(2), "<br/>")),
                    /*4*/ new JLabel("<html>" + genNames(1 + random(3), "<br/>")),
                    /*5*/ button);
            return true;
        }

        @Override
        public boolean removeRows(int[] indices) {
            grid.removeDataRows(indices);
            return true;
        }

        @Override
        public boolean moveRowUp(int index) {
            grid.moveDataRowUp(index);
            return true;
        }

        @Override
        public boolean moveRowDown(int index) {
            grid.moveDataRowDown(index);
            return true;
        }
    }

    private static int random(int n) {
        return (int) (Math.random() * n);
    }

    public static String genNames(int n, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(genName());
        }
        return sb.toString();
    }


    public static String genName() {
        int n = 3 + random(8);
        char[] str = new char[n];
        char[] voc = new char[]{'a', 'a', 'a', 'e', 'e', 'e', 'e', 'e', 'i', 'i', 'o', 'o', 'u', 'u', 'y'};
        for (int i = 0; i < str.length; i++) {
            if (i % 2 == 0) {
                str[i] = voc[random(voc.length)];
            } else {
                str[i] = (char) ('a' + (int) (('z' - 'a') * Math.random()));
            }

        }
        str[0] = Character.toUpperCase(str[0]);
        return new String(str);
    }
}
