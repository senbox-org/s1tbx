package com.bc.ceres.glayer.swing;

import com.bc.ceres.glayer.GraphicalLayer;
import com.bc.ceres.glayer.AlphaCompositeMode;
import com.bc.ceres.glayer.CollectionLayer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Preliminary UI class.
 * <p>Provides a Swing component capable of displaying a collection of {@link com.bc.ceres.glayer.GraphicalLayer}s
 * in a {@link JTable}.
 * </p>
 *
 * @author Norman Fomferra
 */
public class LayerManagerForm {

    private JTable layerTable;
    private LayerTableModel layerTableModel;
    private JSlider transparencySlider;
    private JComboBox alphaCompositeBox;
    private boolean adjusting;
    private JPanel formComponent;

    public LayerManagerForm(CollectionLayer rootLayer) {
        layerTableModel = new LayerTableModel(rootLayer);
        layerTable = new JTable(layerTableModel);
        layerTable.setUpdateSelectionOnSort(true);
        layerTable.setCellSelectionEnabled(false);
        layerTable.setColumnSelectionAllowed(false);
        layerTable.setRowSelectionAllowed(true);
        layerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        layerTable.getColumnModel().getColumn(0).setWidth(24);

        layerTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {

                final int selectedRow = layerTable.getSelectedRow();
                if (selectedRow != -1) {
                    updateLayerStyleUI(getRootLayer().get(selectedRow));
                }
            }
        });
        final JScrollPane scrollPane = new JScrollPane(layerTable);

        transparencySlider = new JSlider(0, 100, 0);
        alphaCompositeBox = new JComboBox(AlphaCompositeMode.values());

        JPanel sliderPanel = new JPanel(new BorderLayout(4, 4));
        sliderPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        sliderPanel.add(new JLabel("Transparency:"), BorderLayout.WEST);
        sliderPanel.add(transparencySlider, BorderLayout.CENTER);
        sliderPanel.add(alphaCompositeBox, BorderLayout.EAST);

        transparencySlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                final int selectedRow = layerTable.getSelectedRow();
                if (selectedRow != -1) {
                    final GraphicalLayer layer = getRootLayer().get(selectedRow);
                    adjusting = true;
                    layer.setTransparency(transparencySlider.getValue() / 100.0f);
                    adjusting = false;
                }
            }
        });

        alphaCompositeBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final int selectedRow = layerTable.getSelectedRow();
                if (selectedRow != -1) {
                    final GraphicalLayer layer = getRootLayer().get(selectedRow);
                    adjusting = true;
                    final AlphaCompositeMode alphaCompositeMode = (AlphaCompositeMode) alphaCompositeBox.getSelectedItem();
                    layer.setAlphaCompositeMode(alphaCompositeMode);
                    adjusting = false;
                }
            }
        });

        rootLayer.addPropertyChangeListener("transparency", new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (!adjusting) {
                    final int selectedRow = layerTable.getSelectedRow();
                    if (selectedRow != -1) {
                        final GraphicalLayer selectedLayer = getRootLayer().get(selectedRow);
                        final GraphicalLayer layer = (GraphicalLayer) evt.getSource();
                        if (selectedLayer == layer) {
                            updateLayerStyleUI(layer);
                        }
                    }
                }
            }
        });

        formComponent = new JPanel(new BorderLayout(4, 4));
        formComponent.add(scrollPane, BorderLayout.CENTER);
        formComponent.add(sliderPanel, BorderLayout.SOUTH);
    }

    private void updateLayerStyleUI(GraphicalLayer layer) {
        final double transparency = layer.getTransparency();
        final int n = (int) Math.round(100.0 * transparency);
        transparencySlider.setValue(n);

        alphaCompositeBox.setSelectedItem(layer.getAlphaCompositeMode());
    }

    public CollectionLayer getRootLayer() {
        return layerTableModel.getRootLayer();
    }

    public JComponent getFormComponent() {
        return formComponent;
    }

    public static void showLayerManager(JFrame frame, CollectionLayer collectionLayer) {
        final LayerManagerForm layerManagerForm = new LayerManagerForm(collectionLayer);
        final JDialog lm = new JDialog(frame, "Layer Manager", false);
        lm.getContentPane().add(layerManagerForm.getFormComponent(), BorderLayout.CENTER);
        lm.pack();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                lm.setVisible(true);
            }
        });
    }

    private static class LayerTableModel extends AbstractTableModel {
        private CollectionLayer rootLayer;
        private static final String[] COLUMN_NAMES = {"Vis", "Name"};
        private static final Class<?>[] COLUMN_CLASS = {Boolean.class, String.class};

        public LayerTableModel(CollectionLayer rootLayer) {
            this.rootLayer = rootLayer;
            rootLayer.addPropertyChangeListener(new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    //fireTableDataChanged(); // todo - be more specific
                }
            });
        }

        public CollectionLayer getRootLayer() {
            return rootLayer;
        }

        public int getRowCount() {
            return rootLayer.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            final GraphicalLayer layer = rootLayer.get(rowIndex);
            if (columnIndex == 0) {
                return layer.isVisible();
            } else if (columnIndex == 1) {
                return layer.getName();
            }
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            final GraphicalLayer layer = rootLayer.get(rowIndex);
            if (columnIndex == 0) {
                layer.setVisible((Boolean) aValue);
            } else if (columnIndex == 1) {
                layer.setName((String) aValue);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return COLUMN_CLASS[columnIndex];
        }
    }
}
