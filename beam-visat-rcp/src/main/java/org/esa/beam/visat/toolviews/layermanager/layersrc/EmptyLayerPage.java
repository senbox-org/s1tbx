package org.esa.beam.visat.toolviews.layermanager.layersrc;


import com.bc.ceres.glayer.Layer;
import org.esa.beam.visat.toolviews.layermanager.LayerPage;
import org.esa.beam.visat.toolviews.layermanager.LayerPageContext;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.TreeSet;
import java.util.ArrayList;

public class EmptyLayerPage extends LayerPage {

    private final static ArrayList<String> names = new ArrayList<String>();
    private JComboBox nameBox;

    static {
        // todo - load names from preferences
        names.add("");
    }

    public EmptyLayerPage() {
        super("Set Layer Name");
    }

    @Override
    public boolean validatePage() {
        return nameBox.getSelectedItem() != null && !nameBox.getSelectedItem().toString().trim().isEmpty();
    }

    @Override
    public boolean hasNextPage() {
        return false;
    }

    @Override
    public boolean canFinish() {
        return true;
    }

    @Override
    public boolean performFinish() {
        Layer selectedLayer = getLayerPageContext().getSelectedLayer();
        int index = -1;
        Layer parentLayer = getLayerPageContext().getView().getRootLayer();
        if (selectedLayer != null) {
            if (selectedLayer.getParent() != null) {
                parentLayer = selectedLayer.getParent();
                index = parentLayer.getChildren().indexOf(selectedLayer);
            }
        }
        if (index == -1) {
            index = parentLayer.getChildren().size();
        }
        final Layer layer = new Layer();
        layer.setName(nameBox.getSelectedItem().toString().trim());
        parentLayer.getChildren().add(index, layer);
        if (!names.contains(layer.getName())) {
            names.add(1, layer.getName());
        }
        return true;
    }

    @Override
    protected Component createLayerPageComponent(LayerPageContext context) {
        nameBox = new JComboBox(names.toArray());
        nameBox.addItemListener(new MyItemListener());
        nameBox.addActionListener(new MyActionListener());
        nameBox.setEditable(true);

        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));

        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.weighty = 0.1;
        constraints.weightx = 0.1;
        panel.add(new JLabel("Layer name:"), constraints);

        constraints.weightx = 0.9;
        panel.add(nameBox, constraints);

        return panel;
    }

    private class MyItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            getLayerPageContext().updateState();
        }
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            getLayerPageContext().updateState();
        }
    }
}