package org.esa.beam.unmixing.visat;

import org.esa.beam.framework.ui.diagram.DiagramCanvas;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

class EndmemberForm extends JPanel {
    EndmemberFormModel formModel;
    JList endmemberList;
    DiagramCanvas diagramCanvas;
    JButton addButton;
    JButton removeButton;
    JButton exportButton;

    public EndmemberForm(EndmemberFormModel formModel) {
        this.formModel = formModel;
        initComponents();
    }

    private void initComponents() {

        endmemberList = new JList();
        endmemberList.setModel(formModel.getEndmemberListModel());
        endmemberList.setSelectionModel(formModel.getEndmemberListSelectionModel());
        endmemberList.setPreferredSize(new Dimension(80, 160));

        diagramCanvas = new DiagramCanvas();
        diagramCanvas.setDiagram(formModel.getEndmemberDiagram());
        formModel.getPropertyChangeSupport().addPropertyChangeListener("selectedEndmemberIndex", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                diagramCanvas.repaint();
            }
        });

        JButton testButton = new JButton(formModel.getTestAction());
        JButton addButton = new JButton(formModel.getAddAction());
        JButton removeButton = new JButton(formModel.getRemoveAction());
        JButton clearButton = new JButton(formModel.getClearAction());
        JButton exportButton = new JButton(formModel.getExportAction());

        GridBagLayout gbl = new GridBagLayout();
        JPanel actionPanel = new JPanel(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipady = 2;
        gbc.gridy = 0;
        actionPanel.add(testButton, gbc);
        gbc.gridy++;
        actionPanel.add(addButton, gbc);
        gbc.gridy++;
        actionPanel.add(removeButton, gbc);
        gbc.gridy++;
        actionPanel.add(clearButton, gbc);
        gbc.gridy++;
        actionPanel.add(exportButton, gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        actionPanel.add(new JLabel(), gbc);

        JPanel endmemberSelectionPanel = new JPanel(new BorderLayout());
        endmemberSelectionPanel.add(new JScrollPane(endmemberList), BorderLayout.CENTER);
        endmemberSelectionPanel.add(actionPanel, BorderLayout.EAST);

        JPanel endmemberPreviewPanel = new JPanel(new BorderLayout());
        endmemberPreviewPanel.add(diagramCanvas, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(endmemberSelectionPanel);
        splitPane.setRightComponent(endmemberPreviewPanel);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setUI(createPlainDividerSplitPaneUI());

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
    }

    private BasicSplitPaneUI createPlainDividerSplitPaneUI() {
        return new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    /**
                     * Overridden in order to do nothing (instead of painting an ugly default divider)
                     *
                     * @param g
                     */
                    @Override
                    public void paint(Graphics g) {
                        // do nothing
                    }
                };
            }
        };
    }

}
