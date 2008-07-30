package com.bc.ceres.glayer.swing;

import com.bc.ceres.glayer.Composite;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;

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

/**
 * Preliminary UI class.
 * <p>Provides a Swing component capable of displaying a collection of {@link com.bc.ceres.glayer.Layer}s
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

    public LayerManagerForm(Layer rootLayer) {
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
                    updateLayerStyleUI(getRootLayer().getChildLayers().get(selectedRow));
                }
            }
        });
        final JScrollPane scrollPane = new JScrollPane(layerTable);

        transparencySlider = new JSlider(0, 100, 0);
        alphaCompositeBox = new JComboBox(Composite.values());

        JPanel sliderPanel = new JPanel(new BorderLayout(4, 4));
        sliderPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        sliderPanel.add(new JLabel("Transparency:"), BorderLayout.WEST);
        sliderPanel.add(transparencySlider, BorderLayout.CENTER);
        sliderPanel.add(alphaCompositeBox, BorderLayout.EAST);

        transparencySlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                final int selectedRow = layerTable.getSelectedRow();
                if (selectedRow != -1) {
                    final Layer layer = getRootLayer().getChildLayers().get(selectedRow);
                    adjusting = true;
                    layer.getStyle().setOpacity(1.0 - transparencySlider.getValue() / 100.0f);
                    adjusting = false;
                }
            }
        });

        alphaCompositeBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final int selectedRow = layerTable.getSelectedRow();
                if (selectedRow != -1) {
                    final Layer layer = getRootLayer().getChildLayers().get(selectedRow);
                    adjusting = true;
                    final Composite composite = (Composite) alphaCompositeBox.getSelectedItem();
                    layer.getStyle().setComposite(composite);
                    adjusting = false;
                }
            }
        });

        rootLayer.addListener(new AbstractLayerListener() {
            public void handleLayerStylePropertyChanged(Layer layer, PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("opacity")) {
                    if (!adjusting) {
                        final int selectedRow = layerTable.getSelectedRow();
                        if (selectedRow != -1) {
                            final Layer selectedLayer = getRootLayer().getChildLayers().get(selectedRow);
                            if (selectedLayer == layer) {
                                updateLayerStyleUI(layer);
                            }
                        }
                    }
                }
            }
        });


        formComponent = new JPanel(new BorderLayout(4, 4));
        formComponent.add(scrollPane, BorderLayout.CENTER);
        formComponent.add(sliderPanel, BorderLayout.SOUTH);
    }

    private void updateLayerStyleUI(Layer layer) {
        final double transparency = 1 - layer.getStyle().getOpacity();
        final int n = (int) Math.round(100.0 * transparency);
        transparencySlider.setValue(n);

        alphaCompositeBox.setSelectedItem(layer.getStyle().getComposite());
    }

    public Layer getRootLayer() {
        return layerTableModel.getRootLayer();
    }

    public JComponent getFormComponent() {
        return formComponent;
    }

    public static void showLayerManager(JFrame frame, Layer collectionLayer) {
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
        private Layer rootLayer;
        private static final String[] COLUMN_NAMES = {"Vis", "Name"};
        private static final Class<?>[] COLUMN_CLASS = {Boolean.class, String.class};

        public LayerTableModel(Layer rootLayer) {
            this.rootLayer = rootLayer;
            rootLayer.addListener(new AbstractLayerListener() {
                // todo - handle layer changes
            });
        }

        public Layer getRootLayer() {
            return rootLayer;
        }

        public int getRowCount() {
            return rootLayer.getChildLayers().size();
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            final Layer layer = rootLayer.getChildLayers().get(rowIndex);
            if (columnIndex == 0) {
                return layer.isVisible();
            } else if (columnIndex == 1) {
                return layer.getName();
            }
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            final Layer layer = rootLayer.getChildLayers().get(rowIndex);
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
