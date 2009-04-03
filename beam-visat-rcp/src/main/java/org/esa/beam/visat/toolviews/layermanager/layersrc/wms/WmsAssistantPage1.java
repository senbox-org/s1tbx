package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;

class WmsAssistantPage1 extends AbstractLayerSourceAssistantPage {

    private JComboBox wmsUrlBox;

    WmsAssistantPage1() {
        super("Select WMS");
    }

    @Override
    public boolean validatePage() {
        String wmsUrl = wmsUrlBox.getSelectedItem().toString();
        return wmsUrl != null && !wmsUrl.trim().isEmpty();
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public AbstractLayerSourceAssistantPage getNextPage() {
        LayerSourcePageContext pageContext = getContext();
        WebMapServer wms = null;
        WMSCapabilities wmsCapabilities = null;

        String wmsUrl = wmsUrlBox.getSelectedItem().toString();
        if (wmsUrl != null && !wmsUrl.isEmpty()) {
            try {
                wms = getWms(pageContext.getWindow(), wmsUrl);
                wmsCapabilities = wms.getCapabilities();
            } catch (Exception e) {
                e.printStackTrace();
                pageContext.showErrorDialog("Failed to access WMS:\n" + e.getMessage());
            }
        }

        if (wms != null && wmsCapabilities != null) {
            pageContext.setPropertyValue(WmsLayerSource.PROPERTY_WMS, wms);
            pageContext.setPropertyValue(WmsLayerSource.PROPERTY_WMS_CAPABILITIES, wmsCapabilities);
            return new WmsAssistantPage2();
        } else {
            return null;
        }
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public Component createPageComponent() {
        GridBagConstraints gbc = new GridBagConstraints();
        final JPanel panel = new JPanel(new GridBagLayout());

        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(new JLabel("URL for WMS (e.g. http://<host>/<server>):"), gbc);

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        wmsUrlBox = new JComboBox(new Object[]{
                "http://wms.globexplorer.com/gexservlets/wms",
                "http://demo.cubewerx.com/demo/cubeserv/cubeserv.cgi",
                "http://www.mapserver.niedersachsen.de/freezoneogc/mapserverogc",
        });
        wmsUrlBox.setEditable(true);
        panel.add(wmsUrlBox, gbc);
        wmsUrlBox.addItemListener(new MyItemListener());

        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        JButton button = new JButton("...");
        button.addActionListener(new MyActionListener());
        panel.add(button, gbc);

        return panel;
    }

    private WebMapServer getWms(Window window, String wmsUrl) throws Exception {
        WebMapServer wms;
        try {
            window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            URL url = new URL(wmsUrl);
            wms = new WebMapServer(url);
        } finally {
            window.setCursor(Cursor.getDefaultCursor());
        }
        return wms;
    }

    private class MyActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Show WMS URL Manager
            String url = "";
            wmsUrlBox.setSelectedItem(url);
            getContext().updateState();
        }
    }

    private class MyItemListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            getContext().updateState();
        }
    }

}