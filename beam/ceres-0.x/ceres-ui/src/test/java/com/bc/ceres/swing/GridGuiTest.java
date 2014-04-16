package com.bc.ceres.swing;

import com.jidesoft.utils.Lm;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.util.List;

public class GridGuiTest {
    public static void main(String[] args) {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
                                              createGridPanel(),
                                              createGridPanel());

        JFrame frame = new JFrame(Grid.class.getSimpleName());
        frame.getContentPane().add(splitPane, BorderLayout.WEST);
        frame.getContentPane().add(createGridPanel(), BorderLayout.CENTER);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(340, 340);
        frame.setVisible(true);
    }

    private static JPanel createGridPanel() {
        Grid grid = new Grid(4, true) /* {
            @Override
            protected JCheckBox createHeaderRowSelector() {
                TristateCheckBox tristateCheckBox = new TristateCheckBox();
                tristateCheckBox.setMixed(true);
                tristateCheckBox.set
                return tristateCheckBox;
            }

            @Override
            protected void adjustHeaderRowSelector(JCheckBox headerRowSelector, int selectedDataRowCount) {
                TristateCheckBox tristateCheckBox = (TristateCheckBox) headerRowSelector;
                if (selectedDataRowCount == 0) {
                    tristateCheckBox.setState(TristateCheckBox.STATE_UNSELECTED);
                } else if (selectedDataRowCount == getDataRowCount()) {
                    tristateCheckBox.setState(TristateCheckBox.STATE_SELECTED);
                } else {
                    tristateCheckBox.setState(TristateCheckBox.STATE_MIXED);
                }
                System.out.println("tristateCheckBox.state = " + tristateCheckBox.getState());
            }
        }*/;
        grid.getLayout().setTablePadding(4, 2);
        grid.getLayout().setColumnFill(1, TableLayout.Fill.HORIZONTAL);
        grid.getLayout().setColumnWeightX(1, 1.0);
        grid.setHeaderRow(new JLabel("<html><b>Name</b>"),
                          new JLabel("<html><b>Method</b>"),
                          new JLabel("<html><b>Parameters</b>"));
        GridControlBar gridControlBar = new GridControlBar(GridControlBar.HORIZONTAL, grid, new MyController());

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(new JScrollPane(grid), BorderLayout.CENTER);
        panel.add(gridControlBar, BorderLayout.SOUTH);
        return panel;
    }


    private static class MyController implements GridControlBar.Controller {

        final static Object[] valueSet = {genName(), genName(), genName(), genName(), genName()};

        @Override
        public JComponent[] newDataRow(final GridControlBar gridControlBar) {
            return new JComponent[]{new JTextField(genName(), 8), new JComboBox<>(valueSet), new JLabel(genName())};
        }

        @Override
        public boolean removeDataRows(GridControlBar gridControlBar, List<Integer> selectedIndexes) {
            return true;
        }

        @Override
        public boolean moveDataRowUp(GridControlBar gridControlBar, int selectedIndex) {
            return true;
        }

        @Override
        public boolean moveDataRowDown(GridControlBar gridControlBar, int selectedIndex) {
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
