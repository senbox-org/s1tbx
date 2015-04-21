/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.swing.update;

import com.bc.ceres.core.runtime.Module;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import java.awt.BorderLayout;
import java.awt.Desktop;

class InfoPane extends JPanel {

    private static final String NOT_AVAILABLE = "Not available.";

    private ModuleItem[] selectedModuleItems;
    private JEditorPane infoPanel;

    public InfoPane() {
        initUi();
    }

    public void setSelectedModuleItems(ModuleItem[] selectedModuleItems) {
        this.selectedModuleItems = selectedModuleItems;
        updateUiState();
    }

    public void updateUiState() {
        if (selectedModuleItems.length == 1) {
            ModuleItem currentModule = selectedModuleItems[0];
            Module module;
            // todo - if an update exists the information from the repository
            // todo - is displayed even if the installed module is selected
            if (currentModule.getRepositoryModule() != null) {
                module = currentModule.getRepositoryModule();
            } else {
                module = currentModule.getModule();
            }

            StringBuilder html = new StringBuilder(1024);
            html.append("<html>");
            html.append("<body>");
            html.append("<p>");
            html.append("<u>Module description:</u><br/>");
            html.append(getTextValue(module.getDescription()));
            html.append("");
            html.append("<p>");
            html.append("<u>Changelog:</u><br/>");
            html.append(getTextValue(module.getChangelog()));
            html.append("");
            html.append("<p>");
            html.append("<u>Vendor information:</u><br/>");
            html.append("<ul>");
            addText(html, "Name", module.getVendor());
            addText(html, "Contact address", module.getContactAddress());
            addText(html, "Copyright", module.getCopyright());
            addUrl(html, "Home page", module.getUrl());
            addUrl(html, "License", module.getLicenseUrl());
            if (module.getFunding() != null && !module.getFunding().trim().isEmpty()) {
                addText(html, "Funding", module.getFunding());
            }
            addUrl(html, "About", module.getAboutUrl());
            html.append("</ul>");
            html.append("");
            html.append("</body>");
            html.append("</html>");

            HTMLDocument document = (HTMLDocument) infoPanel.getEditorKit().createDefaultDocument();
            try {
                document.insertAfterStart(document.getRootElements()[0].getElement(0), html.toString());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(InfoPane.this, "Can not show description", "Error", JOptionPane.ERROR_MESSAGE);
            }
            infoPanel.setDocument(document);

        } else if (selectedModuleItems.length > 1) {
            infoPanel.setText("<html><body></body></html>");
        } else {
            infoPanel.setText("<html><body></body></html>");
        }
    }

    private static void addUrl(StringBuilder html, String label, String url) {
        addItem(html, label, getUrlValue(url));
    }

    private static void addText(StringBuilder html, String label, String text) {
        addItem(html, label, getTextValue(text));
    }

    private static void addItem(StringBuilder html, String label, String value) {
        html.append("<li><b>");
        html.append(label);
        html.append(":</b> ");
        html.append(value);
        html.append("</li>");
    }

    private static String getUrlValue(String url) {
        if (!isTextAvailable(url)) {
            return NOT_AVAILABLE;
        }
        StringBuilder html = new StringBuilder(32);
        html.append("<a href=\"");
        html.append(url);
        html.append("\">");
        html.append(url);
        html.append("</a>");
        return html.toString();
    }

    private static String getTextValue(String text) {
        if (!isTextAvailable(text)) {
            return NOT_AVAILABLE;
        }
        return text;
    }

    private static boolean isTextAvailable(String text) {
        return !(text == null || text.length() == 0);
    }

    private void initUi() {
        setLayout(new BorderLayout());

        infoPanel = new JEditorPane("text/html", null);
        infoPanel.setEditable(false);
        infoPanel.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(InfoPane.this, "Failed to open URL:\n" + e.getURL() + ":\n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        JScrollPane infoPanelScrollPane = new JScrollPane(infoPanel);
        setBorder(BorderFactory.createTitledBorder("Module Information"));
        add(infoPanelScrollPane, BorderLayout.CENTER);
    }
}