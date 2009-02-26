package org.esa.beam.visat.toolviews.layermanager.layersrc;

import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.visat.toolviews.layermanager.LayerSource;

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

public class SelectLayerSourceAssistantPage extends AbstractAppAssistantPage {

    private JList list;
    private LayerSource[] sources;

    public SelectLayerSourceAssistantPage(LayerSource[] sources) {
        super("Select Layer Source");
        this.sources = sources.clone();
    }

    @Override
    public boolean validatePage() {
        return list.getSelectedIndex() >= 0;
    }

    @Override
    public boolean hasNextPage() {
        return getNextPage(getPageContext()) != null;
    }

    @Override
    public AbstractAppAssistantPage getNextPage(AppAssistantPageContext pageContext) {
        int index = list.getSelectedIndex();
        if (index < 0) {
            return null;
        }
        return sources[index].getPage();
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    protected Component createLayerPageComponent(AppAssistantPageContext context) {
        list = new JList(sources);
        list.getSelectionModel().addListSelectionListener(new MyListSelectionListener());
        list.setCellRenderer(new MyDefaultListCellRenderer());

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

    private class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {

            getPageContext().updateState();
        }
    }

    private static class MyDefaultListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setText("<html><b>" + value + "</b>");
            return label;
        }
    }

}
