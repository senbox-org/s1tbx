package org.esa.beam.opendap.ui;

import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TitledPanel extends JPanel {

    public TitledPanel(JComponent titleComponent, JComponent bodyComponent) {
        this(titleComponent, bodyComponent, false, false);
    }

    public TitledPanel(JComponent titleComponent, JComponent bodyComponent, boolean isCollapsible, boolean isInitiallyCollapsed) {
        super(new BorderLayout());

        final JPanel titleArea = new JPanel(new BorderLayout());
        if (titleComponent != null) {
            titleArea.add(titleComponent, BorderLayout.WEST);
        }
        titleArea.add(getSeparator(), BorderLayout.CENTER);
        if (isCollapsible) {
            titleArea.add(getCollapseButton(bodyComponent, isInitiallyCollapsed), BorderLayout.EAST);
            bodyComponent.setVisible(!isInitiallyCollapsed);
        }
        add(titleArea, BorderLayout.NORTH);

        if (bodyComponent != null) {
            bodyComponent.setBorder(new EmptyBorder(0, 30, 0, 0));
            add(bodyComponent, BorderLayout.CENTER);
        }

        setBorder(new EmptyBorder(4, 8, 4, 8));
    }

    private JComponent getCollapseButton(JComponent bodyComponent, boolean isInitiallyCollapsed) {
        final AbstractButton hideAndShowButton;
        if (isInitiallyCollapsed) {
            hideAndShowButton = ToolButtonFactory.createButton(CollapseSupport.expandIcon, false);
            hideAndShowButton.setRolloverIcon(CollapseSupport.expandRolloverIcon);
            hideAndShowButton.setToolTipText("Expand Panel");
        } else {
            hideAndShowButton = ToolButtonFactory.createButton(CollapseSupport.collapseIcon, false);
            hideAndShowButton.setRolloverIcon(CollapseSupport.collapseRolloverIcon);
            hideAndShowButton.setToolTipText("Collapse Panel");
        }
        hideAndShowButton.addActionListener(new CollapseSupport(isInitiallyCollapsed, bodyComponent, hideAndShowButton));

        return hideAndShowButton;
    }

    private JPanel getSeparator() {
        final JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        final JPanel separatorPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 1;
        gbc.weighty = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        separatorPanel.add(separator, gbc);
        return separatorPanel;
    }

    private static class CollapseSupport implements ActionListener {

        public boolean componentShown;
        private final JComponent bodyComponent;
        private final AbstractButton hideAndShowButton;
        private static final ImageIcon collapseIcon = UIUtils.loadImageIcon("icons/PanelUp12.png");
        private static final ImageIcon collapseRolloverIcon = ToolButtonFactory.createRolloverIcon(collapseIcon);
        private static final ImageIcon expandIcon = UIUtils.loadImageIcon("icons/PanelDown12.png");
        private static final ImageIcon expandRolloverIcon = ToolButtonFactory.createRolloverIcon(expandIcon);

        public CollapseSupport(boolean isInitiallyCollapsed, JComponent bodyComponent, AbstractButton hideAndShowButton) {
            this.bodyComponent = bodyComponent;
            this.hideAndShowButton = hideAndShowButton;
            this.componentShown = !isInitiallyCollapsed;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            componentShown = !componentShown;
            bodyComponent.setVisible(componentShown);
            if (componentShown) {
                hideAndShowButton.setIcon(collapseIcon);
                hideAndShowButton.setRolloverIcon(collapseRolloverIcon);
                hideAndShowButton.setToolTipText("Collapse Panel");
            } else {
                hideAndShowButton.setIcon(expandIcon);
                hideAndShowButton.setRolloverIcon(expandRolloverIcon);
                hideAndShowButton.setToolTipText("Expand Panel");
            }
        }
    }
}
