package com.bc.ceres.swing;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.Arrays;

public class ListControlBarTest {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
                                              GridGuiTest.createGridPanel(),
                                              createListPanel());

        JFrame frame = new JFrame(ListControlBarTest.class.getSimpleName());
        frame.getContentPane().add(splitPane, BorderLayout.WEST);
        frame.getContentPane().add(createTablePanel(), BorderLayout.CENTER);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(340, 340);
        frame.setVisible(true);
    }

    private static JPanel createListPanel() {

        final DefaultListModel<String> listModel = new DefaultListModel<>();
        final JList<String> list = new JList<>(listModel);

        ListControlBar listControlBar = ListControlBar.create(JToolBar.VERTICAL, list, new ListControlBar.AbstractListController() {
            @Override
            public boolean addRow(int index) {
                listModel.addElement(genName());
                list.setSelectedIndex(listModel.getSize() - 1);
                return true;
            }

            @Override
            public boolean removeRows(int[] indices) {
                int[] clone = indices.clone();
                Arrays.sort(clone);
                list.clearSelection();
                for (int i = clone.length - 1; i >= 0; i--) {
                    listModel.remove(indices[i]);
                }
                return true;
            }

            @Override
            public boolean moveRowUp(int index) {
                String element = listModel.remove(index);
                listModel.insertElementAt(element, index - 1);
                list.setSelectedIndex(index - 1);
                return true;
            }

            @Override
            public boolean moveRowDown(int index) {
                String element = listModel.remove(index);
                listModel.insertElementAt(element, index + 1);
                list.setSelectedIndex(index + 1);
                return false;
            }
        });

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(new JLabel("List of stuff:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        panel.add(listControlBar, BorderLayout.EAST);
        return panel;
    }


    private static JPanel createTablePanel() {


        final DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Target name", "Source", "Aggregator", "Parameters"}, 0);
        final JTable table = new JTable(tableModel);

        ListControlBar listControlBar = ListControlBar.create(JToolBar.VERTICAL, table, new ListControlBar.AbstractListController() {
            @Override
            public boolean addRow(int index) {
                tableModel.addRow(new Object[]{genName(), genName(), genName(), genName()});
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().addSelectionInterval(tableModel.getRowCount() - 1, tableModel.getRowCount() - 1);
                return true;
            }

            @Override
            public boolean removeRows(int[] indices) {
                int[] clone = indices.clone();
                Arrays.sort(clone);
                table.getSelectionModel().clearSelection();
                for (int i = clone.length - 1; i >= 0; i--) {
                    tableModel.removeRow(indices[i]);
                }
                return true;
            }

            @Override
            public boolean moveRowUp(int index) {
                tableModel.moveRow(index, index, index - 1);
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().addSelectionInterval(index - 1, index - 1);
                return true;
            }

            @Override
            public boolean moveRowDown(int index) {
                tableModel.moveRow(index, index, index + 1);
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().addSelectionInterval(index + 1, index + 1);
                return true;
            }
        });

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(new JLabel("Table of things:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(listControlBar, BorderLayout.EAST);
        return panel;
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
