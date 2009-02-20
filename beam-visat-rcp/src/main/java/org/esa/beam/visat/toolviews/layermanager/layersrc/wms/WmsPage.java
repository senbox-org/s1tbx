package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import org.esa.beam.visat.toolviews.layermanager.LayerPage;
import org.esa.beam.visat.toolviews.layermanager.LayerPageContext;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;

public class WmsPage extends LayerPage {

    private JComboBox wmsUrlBox;

    public WmsPage() {
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
    public LayerPage getNextLayerPage() {
        WebMapServer wms = null;
        WMSCapabilities wmsCapabilities = null;

        String wmsUrl = wmsUrlBox.getSelectedItem().toString();
        if (wmsUrl != null && !wmsUrl.isEmpty()) {
            try {
                wms = getWms(wmsUrl);
                wmsCapabilities = wms.getCapabilities();
            } catch (Exception e) {
                e.printStackTrace();
                getPageContext().showErrorDialog("Failed to access WMS:\n" + e.getMessage());
            }
        }

        if (wms != null && wmsCapabilities != null) {
            return new WmsPage2(wms, wmsCapabilities);
        } else {
            return null;
        }
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    protected Component createLayerPageComponent(LayerPageContext context) {
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

    private WebMapServer getWms(String wmsUrl) throws Exception {
        WebMapServer wms;
        try {
            getPageContext().getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            URL url = new URL(wmsUrl);
            wms = new WebMapServer(url);
        } finally {
            getPageContext().getWindow().setCursor(Cursor.getDefaultCursor());
        }
        return wms;
    }

    private class MyActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            // Show WMS URL Manager
            String url = "";
            wmsUrlBox.setSelectedItem(url);
            getPageContext().updateState();
        }
    }

    private class MyItemListener implements ItemListener {

        public void itemStateChanged(ItemEvent e) {
            getPageContext().updateState();
        }
    }

}