package org.esa.beam.framework.ui.color;

import org.junit.Ignore;

import javax.swing.AbstractListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Ignore
public class ColorSelectionTest {
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        DefaultTableModel dm = new DefaultTableModel(new Object[][]{
                {"C1", Color.RED},
                {"C2", Color.GREEN},
                {"C3", Color.BLUE}},
                                                     new String[]{"Name", "Color"}) {


            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? String.class : Color.class;
            }
        };

        JTable table = new JTable(dm);
        ColorTableCellEditor editor = new ColorTableCellEditor();
        ColorTableCellRenderer renderer = new ColorTableCellRenderer();
        table.setDefaultEditor(Color.class, editor);
        table.setDefaultRenderer(Color.class, renderer);
        table.getModel().addTableModelListener(e -> System.out.println("e = " + e));

        ColorComboBox colorComboBox1 = new ColorComboBox(Color.YELLOW);
        ColorComboBox colorComboBox2 = new ColorComboBox(Color.GREEN);
        colorComboBox2.setColorChooserPanelFactory(CustomColorChooserPanel::new);

        JFrame frame = new JFrame("Color Selection Test");
        frame.setLocation(200, 100);
        frame.add(colorComboBox1, BorderLayout.NORTH);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(colorComboBox2, BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static List<ColorItem> getColorItems() {
        try {
            Path path = Paths.get(ColorComboBox.class.getResource("colors.txt").toURI());
            List<String> lines = Files.readAllLines(path);
            List<ColorItem> colors = new ArrayList<>();
            for (String line : lines) {
                int i = line.indexOf('\t');
                Color color = Color.decode(line.substring(0, i).trim());
                String displayName = line.substring(i).trim();
                System.out.println("color = "+color+", displayName = " + displayName);
                colors.add(new ColorItem(displayName, color));
            }
            return colors;
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    static class ColorItem {
        String name;
        Color color;

        public ColorItem(String name, Color color) {
            this.name = name;
            this.color = color;
        }
    }

    private static class ColorItemListCellRenderer extends JLabel implements ListCellRenderer<ColorItem> {
        @Override
        public Component getListCellRendererComponent(JList<? extends ColorItem> list, ColorItem value, int index, boolean isSelected, boolean cellHasFocus) {
            setIcon(new ColorItemIcon(value));
            setText(value.name);
            setBorder(isSelected ? new LineBorder(Color.ORANGE, 2) : new LineBorder(Color.WHITE, 2));
            return this;
        }

        private static class ColorItemIcon implements Icon {
            private final ColorItem value;

            public ColorItemIcon(ColorItem value) {
                this.value = value;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(value.color);
                g.fillRect(0, 0, getIconWidth(),getIconHeight());
            }

            @Override
            public int getIconWidth() {
                return 32;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        }
    }

    private static class CustomColorChooserPanel extends ColorChooserPanel {
        public CustomColorChooserPanel(Color selectedColor) {
            super(selectedColor);
        }

        @Override
        protected JComponent createColorPicker() {
            List<ColorItem> colors = getColorItems();
            JList<ColorItem> view = new JList<>(new AbstractListModel<ColorItem>() {
                @Override
                public int getSize() {
                    return colors.size();
                }

                @Override
                public ColorItem getElementAt(int index) {
                    return colors.get(index);
                }
            });
            view.setCellRenderer(new ColorItemListCellRenderer());
            view.addListSelectionListener(e -> {
                setSelectedColor(view.getSelectedValue().color);
            });
            return new JScrollPane(view);
        }
    }
}
