package org.esa.beam.visat.toolviews.imageinfo;

import com.jidesoft.swing.TitledSeparator;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class MoreOptionsPane {
    private static ImageIcon iconE;
    private static ImageIcon iconC;
    private static ImageIcon iconER;
    private static ImageIcon iconCR;

    private final ColorManipulationForm colorManipulationForm;
    private final JPanel contentPanel;
    private final AbstractButton collapseButton;

    private JComponent component;
    private boolean collapsed;

    MoreOptionsPane(ColorManipulationForm colorManipulationForm) {
        if (iconE == null) {
            iconC = UIUtils.loadImageIcon("icons/PanelCollapse12.png");
            iconCR = ToolButtonFactory.createRolloverIcon(iconC);
            iconE = UIUtils.loadImageIcon("icons/PanelExpand12.png");
            iconER = ToolButtonFactory.createRolloverIcon(iconE);
        }

        this.colorManipulationForm = colorManipulationForm;

        component = new JLabel(); // dummy
        collapsed = true;

        collapseButton = ToolButtonFactory.createButton(iconC, false);
        collapseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setCollapsed(!isCollapsed());
            }
        });

        final JPanel titleBar = new JPanel(new BorderLayout(2, 2));
        titleBar.add(collapseButton, BorderLayout.WEST);
        titleBar.add(new TitledSeparator("More Options"), BorderLayout.CENTER);

        contentPanel = new JPanel(new BorderLayout(2, 2));
        contentPanel.add(titleBar, BorderLayout.NORTH);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public JComponent getComponent() {
        return component;
    }

    public void setComponent(JComponent component) {
        contentPanel.remove(this.component);
        this.component = component;
        updateState();
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        updateState();
    }

    private void updateState() {
        if (collapsed) {
            contentPanel.remove(this.component);
        } else {
            contentPanel.add(this.component, BorderLayout.CENTER);
        }
        collapseButton.setIcon(collapsed ? iconC : iconE);
        collapseButton.setRolloverIcon(collapsed ? iconCR : iconER);
        colorManipulationForm.revalidateToolViewPaneControl();
    }
}
