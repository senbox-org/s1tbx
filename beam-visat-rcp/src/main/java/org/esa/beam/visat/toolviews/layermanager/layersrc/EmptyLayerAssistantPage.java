package org.esa.beam.visat.toolviews.layermanager.layersrc;


import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.framework.ui.assistant.AssistantPageContext;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

public class EmptyLayerAssistantPage extends AbstractAppAssistantPage {

    private final static ArrayList<String> names = new ArrayList<String>();
    private JComboBox nameBox;

    static {
        // todo - load names from preferences
        names.add("");
    }

    public EmptyLayerAssistantPage() {
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
    public boolean performFinish(AppAssistantPageContext pageContext) {
        Layer layer = new Layer();
        layer.setName(nameBox.getSelectedItem().toString().trim());
        Layer rootLayer = pageContext.getAppContext().getSelectedProductSceneView().getRootLayer();
        rootLayer.getChildren().add(0, layer);
        if (!names.contains(layer.getName())) {
            names.add(1, layer.getName());
        }
        return true;
    }

    @Override
    public Component createLayerPageComponent(AppAssistantPageContext context) {
        nameBox = new JComboBox(names.toArray());
        nameBox.addItemListener(new MyItemListener(context));
        nameBox.addActionListener(new MyActionListener(context));
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

        private final AppAssistantPageContext pageContext;

        public MyItemListener(AppAssistantPageContext context) {
            pageContext = context;
        }
        
        public void itemStateChanged(ItemEvent e) {
            pageContext.updateState();
        }
    }

    private class MyActionListener implements ActionListener {

        private final AppAssistantPageContext pageContext;

        public MyActionListener(AppAssistantPageContext context) {
            pageContext = context;
        }
        
        public void actionPerformed(ActionEvent e) {
            pageContext.updateState();
        }
    }
}