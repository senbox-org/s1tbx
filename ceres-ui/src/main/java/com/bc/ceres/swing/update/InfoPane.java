package com.bc.ceres.swing.update;

import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.swing.CollapsiblePane;
import com.bc.ceres.swing.UriLabel;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;

class InfoPane extends JPanel {

    private static final String MULTIPLE_MODULES_SELECTED = "MultipleModuleSelected";
    private static final String NO_MODULE_SELECTED = "NoModuleSelected";
    private static final String SINGLE_MODULE_SELECTED = "SingleModuleSelected";

    private ModuleItem[] selectedModuleItems;
    private JLabel descriptionArea;
    private JLabel changelogArea;
    private CardLayout cardLayout;
    private JLabel vendorLabel;
    private JLabel addressLabel;
    private JLabel copyrightLabel;
    private UriLabel urlLabel;
    private UriLabel licenseLabel;
    private UriLabel aboutLabel;

    public InfoPane() {
        initUi();
    }

    public ModuleItem getCurrentModule() {
        if (selectedModuleItems.length > 0) {
            return selectedModuleItems[0];
        } else {
            return null;
        }
    }

    public ModuleItem[] getSelectedModuleItems() {
        return selectedModuleItems;
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
            vendorLabel.setText(module.getVendor());
            addressLabel.setText(module.getContactAddress());
            copyrightLabel.setText(module.getCopyright());
            setLinkLabel(urlLabel, module.getUrl());
            setLinkLabel(licenseLabel, module.getLicenseUrl());
            setLinkLabel(aboutLabel, module.getAboutUrl());

            descriptionArea.setText("<html>" + getDescriptionText(module) + "</html>");
            changelogArea.setText("<html>" + getChanglogText(module) + "</html>");

            cardLayout.show(this, SINGLE_MODULE_SELECTED);
        } else if (selectedModuleItems.length > 1) {
            cardLayout.show(this, MULTIPLE_MODULES_SELECTED);
        } else {
            cardLayout.show(this, NO_MODULE_SELECTED);
        }
    }

    private static String getDescriptionText(Module module) {
        String description = module.getDescription();
        if (description == null) {
            description = "";
        }
        return description;
    }

    private static String getChanglogText(Module module) {
        String changelog = module.getChangelog();
        if (changelog == null) {
            changelog = "";
        }
        return changelog;
    }

    private static void setLinkLabel(UriLabel uriLabel, String uriString) {
        uriLabel.setText(uriString);
        uriLabel.setUri(uriString);
    }

    private void initUi() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        descriptionArea = new JLabel();
        descriptionArea.setOpaque(true);
        descriptionArea.setBackground(getBackground().brighter());
        changelogArea = new JLabel();
        changelogArea.setOpaque(true);
        changelogArea.setBackground(getBackground().brighter());

        vendorLabel = new JLabel();
        addressLabel = new JLabel();
        copyrightLabel = new JLabel();
        urlLabel = new UriLabel();
        licenseLabel = new UriLabel();
        aboutLabel = new UriLabel();

        JPanel labelsPanel = new JPanel(new GridLayout(-1, 1, 1, 1));
        JPanel contentPanel = new JPanel(new GridLayout(-1, 1, 1, 1));

        labelsPanel.add(new JLabel("Name:"));
        labelsPanel.add(new JLabel("Address:"));
        labelsPanel.add(new JLabel("Home page:"));
        labelsPanel.add(new JLabel("License:"));
        labelsPanel.add(new JLabel("About:"));
        labelsPanel.add(new JLabel("Copyright:"));

        contentPanel.add(vendorLabel);
        contentPanel.add(addressLabel);
        contentPanel.add(urlLabel);
        contentPanel.add(licenseLabel);
        contentPanel.add(aboutLabel);
        contentPanel.add(copyrightLabel);

        JPanel vendorPanel = new JPanel(new BorderLayout(3, 3));
        vendorPanel.add(labelsPanel, BorderLayout.WEST);
        vendorPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel collapsiblePaneContainer = CollapsiblePane.createCollapsiblePaneContainer();
        collapsiblePaneContainer.add(
                new CollapsiblePane("Description", new JScrollPane(descriptionArea), false, false));
        collapsiblePaneContainer.add(
                new CollapsiblePane("Changelog", new JScrollPane(changelogArea), true, false));
        collapsiblePaneContainer.add(
                new CollapsiblePane("Vendor", vendorPanel, true, true));

        add(collapsiblePaneContainer, SINGLE_MODULE_SELECTED);
        add(new JLabel("No module selected."), NO_MODULE_SELECTED);
        add(new JLabel("Multiple modules selected."), MULTIPLE_MODULES_SELECTED);
        setBorder(BorderFactory.createTitledBorder("Module Information"));
        cardLayout.show(this, NO_MODULE_SELECTED);
    }
}
