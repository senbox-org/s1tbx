package org.esa.beam.unmixing.ui;

import org.esa.beam.framework.ui.diagram.DiagramCanvas;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class EndmemberForm extends JPanel {
    EndmemberFormModel formModel;
    JList endmemberList;
    DiagramCanvas diagramCanvas;
    JButton addButton;
    JButton removeButton;
    JButton exportButton;

    public EndmemberForm(AppContext appContext) {
        this.formModel = new EndmemberFormModel(appContext);
        initComponents();
    }

    public EndmemberFormModel getFormModel() {
        return formModel;
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

        AbstractButton addButton = ToolButtonFactory.createButton(formModel.getAddAction(), false);
        AbstractButton removeButton = ToolButtonFactory.createButton(formModel.getRemoveAction(), false);
        AbstractButton clearButton = ToolButtonFactory.createButton(formModel.getClearAction(), false);
        AbstractButton exportButton = ToolButtonFactory.createButton(formModel.getExportAction(), false);

        GridBagLayout gbl = new GridBagLayout();
        JPanel actionPanel = new JPanel(gbl);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 0, 3));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipady = 2;
        gbc.gridy = 0;
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
        final Color color = actionPanel.getBackground();
        final float[] rgbColors = new float[3];
        color.getRGBColorComponents(rgbColors);
        final float factor = 0.9f;
        actionPanel.setBackground(new Color(rgbColors[0] * factor, rgbColors[1] * factor, rgbColors[2] * factor));

        JPanel endmemberSelectionPanel = new JPanel(new BorderLayout());
        endmemberSelectionPanel.add(new JScrollPane(endmemberList), BorderLayout.CENTER);
        endmemberSelectionPanel.add(actionPanel, BorderLayout.WEST);

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
                    @Override
                    public void paint(Graphics g) {
                        // do nothing
                    }
                };
            }
        };
    }

}
