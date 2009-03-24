package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.geotools.data.wms.WebMapServer;

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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

class WmsAssistantPage1 extends AbstractAppAssistantPage {

    private JComboBox wmsUrlBox;
    private final WmsModel wmsModel;

    WmsAssistantPage1(WmsModel wmsModel) {
        super("Select WMS");
        this.wmsModel = wmsModel;
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
    public AbstractAppAssistantPage getNextPage(AppAssistantPageContext pageContext) {
        wmsModel.wms= null;
        wmsModel.wmsCapabilities = null;

        wmsModel.wmsUrl = wmsUrlBox.getSelectedItem().toString();
        if (wmsModel.wmsUrl != null && !wmsModel.wmsUrl.isEmpty()) {
            try {
                wmsModel.wms = getWms(pageContext.getWindow(), wmsModel.wmsUrl);
                wmsModel.wmsCapabilities = wmsModel.wms.getCapabilities();
            } catch (Exception e) {
                e.printStackTrace();
                pageContext.showErrorDialog("Failed to access WMS:\n" + e.getMessage());
            }
        }

        if (wmsModel.wms != null && wmsModel.wmsCapabilities != null) {
            return new WmsAssistantPage2(wmsModel);
        } else {
            return null;
        }
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public Component createLayerPageComponent(AppAssistantPageContext context) {
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
        wmsUrlBox.addItemListener(new MyItemListener(context));

        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        JButton button = new JButton("...");
        button.addActionListener(new MyActionListener(context));
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

        private final AppAssistantPageContext pageContext;
        
        public MyActionListener(AppAssistantPageContext pageContext) {
            this.pageContext = pageContext;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Show WMS URL Manager
            String url = "";
            wmsUrlBox.setSelectedItem(url);
            pageContext.updateState();
        }
    }

    private class MyItemListener implements ItemListener {

        private final AppAssistantPageContext pageContext;
        
        public MyItemListener(AppAssistantPageContext pageContext) {
            this.pageContext = pageContext;
        }
        
        @Override
        public void itemStateChanged(ItemEvent e) {
            pageContext.updateState();
        }
    }

}