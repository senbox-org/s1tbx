package com.bc.ceres.swing;

import com.jidesoft.swing.TristateCheckBox;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;

public class GridTest {
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Grid grid = new Grid(4, true); /* {
            @Override
            protected JCheckBox createHeaderRowSelector() {
                return new TristateCheckBox();
            }

            @Override
            protected void adjustHeaderRowSelector(JCheckBox checkBox, int selectedDataRowCount) {
                TristateCheckBox tristateCheckBox = (TristateCheckBox) checkBox;
                if (selectedDataRowCount == 0) {
                    tristateCheckBox.setState(TristateCheckBox.STATE_UNSELECTED);
                } else if (selectedDataRowCount == getRowCount() - 1) {
                    tristateCheckBox.setState(TristateCheckBox.STATE_SELECTED);
                } else {
                    tristateCheckBox.setState(TristateCheckBox.STATE_MIXED);
                }
                System.out.println("tristateCheckBox.state = " + tristateCheckBox.getState());
            }
        };*/
        grid.getLayout().setTablePadding(4, 4);
        grid.getLayout().setColumnFill(1, TableLayout.Fill.HORIZONTAL);
        grid.getLayout().setColumnWeightX(1, 1.0);
        grid.setHeaderRow(new JLabel("<html><b>Name</b>"), new JLabel("<html><b>Letter</b>"), new JLabel("<html><b>Units</b>"));

        GridControlPanel gridControlPanel = new GridControlPanel(grid, new MyController());

        JFrame frame = new JFrame(Grid.class.getSimpleName());
        frame.getContentPane().add(new JScrollPane(grid), BorderLayout.CENTER);
        //frame.getContentPane().add(new JScrollPane(grid), BorderLayout.NORTH);
        //frame.getContentPane().add(new JLabel("Wraw!"), BorderLayout.CENTER);
        frame.getContentPane().add(gridControlPanel, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(340, 340);
        frame.setVisible(true);
    }


    private static class MyController implements GridControlPanel.Controller {

        final static Object[] valueSet = {genName(), genName(), genName(), genName(), genName()};

        @Override
        public JComponent[] newDataRow(final GridControlPanel gridControlPanel) {
            return new JComponent[]{new JTextField(genName(), 8), new JComboBox<>(valueSet), new JLabel(genName())};
        }

        @Override
        public boolean removeDataRows(GridControlPanel gridControlPanel) {
            return true;
        }

        @Override
        public boolean moveDataRowUp(GridControlPanel gridControlPanel, int selectedIndex) {
            return true;
        }

        @Override
        public boolean moveDataRowDown(GridControlPanel gridControlPanel, int selectedIndex) {
            return true;
        }
    }


    public static String genName() {
        int n = 3 + (int) (Math.random() * 8);
        char[] str = new char[n];
        char[] voc = new char[]{'a', 'a', 'a', 'e', 'e', 'e', 'e', 'e', 'i', 'i', 'o', 'o', 'u', 'u', 'y'};
        for (int i = 0; i < str.length; i++) {
            if (i % 2 == 0) {
                str[i] = voc[(int) (Math.random() * voc.length)];
            } else {
                str[i] = (char) ('a' + (int) (('z' - 'a') * Math.random()));
            }

        }
        str[0] = Character.toUpperCase(str[0]);
        return new String(str);
    }
}
