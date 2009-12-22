package org.esa.beam.visat.toolviews.layermanager.layersrc;

import org.esa.beam.framework.ui.layer.LayerSource;
import org.esa.beam.framework.ui.layer.LayerSourceDescriptor;
import org.esa.beam.framework.ui.layer.LayerSourcePageContext;
import org.esa.beam.framework.ui.layer.AbstractLayerSourceAssistantPage;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 */
public class SelectLayerSourceAssistantPage extends AbstractLayerSourceAssistantPage {

    private JList list;
    private Map<LayerSourceDescriptor, LayerSource> layerSourceMap;

    public SelectLayerSourceAssistantPage(LayerSourceDescriptor[] sourceDescriptors) {
        super("Select Layer Source");
        layerSourceMap = new HashMap<LayerSourceDescriptor, LayerSource>();
        for (LayerSourceDescriptor sourceDescriptor : sourceDescriptors) {
            layerSourceMap.put(sourceDescriptor, sourceDescriptor.createLayerSource());
        }
    }

    @Override
    public boolean validatePage() {
        return list.getSelectedIndex() >= 0;
    }

    @Override
    public boolean hasNextPage() {
        LayerSourceDescriptor selected = (LayerSourceDescriptor) list.getSelectedValue();
        if (selected == null) {
            return false;
        }
        return layerSourceMap.get(selected).hasFirstPage();
    }

    @Override
    public AbstractLayerSourceAssistantPage getNextPage() {
        LayerSourceDescriptor selected = (LayerSourceDescriptor) list.getSelectedValue();
        if (selected == null) {
            return null;
        }
        LayerSource layerSource = layerSourceMap.get(selected);
        LayerSourcePageContext pageContext = getContext();
        pageContext.setLayerSource(layerSource);
        return layerSource.getFirstPage(pageContext);
    }

    @Override
    public boolean canFinish() {
        LayerSourceDescriptor selected = (LayerSourceDescriptor) list.getSelectedValue();
        if (selected == null) {
            return false;
        }
        return layerSourceMap.get(selected).canFinish(getContext());
    }

    @Override
    public boolean performFinish() {
        LayerSourceDescriptor selected = (LayerSourceDescriptor) list.getSelectedValue();
        if (selected == null) {
            return false;
        }
        return layerSourceMap.get(selected).performFinish(getContext());
    }

    @Override
    public Component createPageComponent() {
        LayerSourcePageContext context = getContext();
        Set<LayerSourceDescriptor> descriptorSet = layerSourceMap.keySet();
        List<LayerSourceDescriptor> descriptorList = new ArrayList<LayerSourceDescriptor>(descriptorSet.size());
        for (LayerSourceDescriptor lsd : descriptorSet) {
            LayerSource lsc = layerSourceMap.get(lsd);
            if (lsc.isApplicable(context)) {
                descriptorList.add(lsd);
            }
        }
        Collections.sort(descriptorList, new Comparator<LayerSourceDescriptor>() {
            @Override
            public int compare(LayerSourceDescriptor o1, LayerSourceDescriptor o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        list = new JList(descriptorList.toArray(new LayerSourceDescriptor[descriptorList.size()]));
        list.getSelectionModel().addListSelectionListener(new LayerSourceSelectionListener());
        list.setCellRenderer(new LayerSourceCellRenderer());

        GridBagConstraints gbc = new GridBagConstraints();
        final JPanel panel = new JPanel(new GridBagLayout());
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Available layer sources:"), gbc);
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(list), gbc);

        return panel;
    }

    private class LayerSourceSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            getContext().updateState();
        }
    }

    private static class LayerSourceCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                                                                             cellHasFocus);
            if (value instanceof LayerSourceDescriptor) {
                LayerSourceDescriptor layerSourceDescriptor = (LayerSourceDescriptor) value;
                label.setText("<html><b>" + layerSourceDescriptor.getName() + "</b>");
                label.setToolTipText(layerSourceDescriptor.getDescription());
            } else {
                label.setText("Invalid");
            }
            return label;
        }
    }

}
