/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.jidesoft.swing.SimpleScrollPane;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.StandardChartTheme;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * A common class for chart based panels
 *
 * @author Marcoz
 * @author Tonio
 */
public abstract class ChartPagePanel extends PagePanel {

    protected static final String HELP_TIP_MESSAGE = "For more information about this plot\n" +
            "hit the help button at the bottom right.";
    protected static final String ZOOM_TIP_MESSAGE = "TIP: To zoom within the chart, draw a rectangle\n" +
            "with the mouse or use the context menu.";

    private AbstractButton hideAndShowButton;
    private JPanel backgroundPanel;
    private RoiMaskSelector roiMaskSelector;
    protected AbstractButton refreshButton;
    private final boolean refreshButtonEnabled;
    private AbstractButton switchToTableButton;

    static {
        final StandardChartTheme theme = (StandardChartTheme) ChartFactory.getChartTheme();
        theme.setPlotBackgroundPaint(new Color(225, 225, 225));
    }

    public ChartPagePanel(ToolView parentDialog, String helpId, String titlePrefix, boolean refreshButtonEnabled) {
        super(parentDialog, helpId, titlePrefix);
        this.refreshButtonEnabled = refreshButtonEnabled;
    }

    @Override
    protected void updateComponents() {
        roiMaskSelector.updateMaskSource(getProduct());
        refreshButton.setEnabled(refreshButtonEnabled && (getRaster() != null));
    }

    private JPanel createTopPanel() {
        refreshButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/ViewRefresh22.png"),
                false);
        refreshButton.setToolTipText("Refresh View");
        refreshButton.setName("refreshButton");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateChartData();
                refreshButton.setEnabled(false);
            }
        });

        switchToTableButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Table24.png"),
                false);
        switchToTableButton.setToolTipText("Switch to Table View");
        switchToTableButton.setName("switchToTableButton");
        switchToTableButton.setEnabled(hasAlternativeView());
        switchToTableButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                showAlternativeView();

            }
        });

        final TableLayout tableLayout = new TableLayout(6);
        tableLayout.setColumnFill(2, TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(2, 1.0);
        tableLayout.setRowPadding(0, new Insets(0,4,0,0));
        JPanel buttonPanel = new JPanel(tableLayout);
        buttonPanel.add(refreshButton);
        tableLayout.setRowPadding(0, new Insets(0,0,0,0));
        buttonPanel.add(switchToTableButton);
        buttonPanel.add(new JPanel());

        return buttonPanel;
    }

    /**
     * Asks the chart panel to update its chart data. This involve a (re-)computation of all datasets.
     */
    protected abstract void updateChartData();

    private JPanel createChartBottomPanel(final ChartPanel chartPanel) {

        final AbstractButton zoomAllButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("/com/bc/ceres/swing/actions/icons_22x22/view-fullscreen.png"),
                false);
        zoomAllButton.setToolTipText("Zoom all.");
        zoomAllButton.setName("zoomAllButton.");
        zoomAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chartPanel.restoreAutoBounds();
                chartPanel.repaint();
            }
        });

        final AbstractButton propertiesButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Edit24.gif"),
                false);
        propertiesButton.setToolTipText("Edit properties.");
        propertiesButton.setName("propertiesButton.");
        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chartPanel.doEditChartProperties();
            }
        });

        final AbstractButton saveButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Export24.gif"),
                false);
        saveButton.setToolTipText("Save chart as image.");
        saveButton.setName("saveButton.");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    chartPanel.doSaveAs();
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(chartPanel,
                                                  "Could not save chart:\n" + e1.getMessage(),
                                                  "Error",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        final AbstractButton printButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Print24.gif"),
                false);
        printButton.setToolTipText("Print chart.");
        printButton.setName("printButton.");
        printButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chartPanel.createChartPrintJob();
            }
        });

        final TableLayout tableLayout = new TableLayout(6);
        tableLayout.setColumnFill(4, TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(4, 1.0);
        JPanel buttonPanel = new JPanel(tableLayout);
        tableLayout.setRowPadding(0, new Insets(0,4,0,0));
        buttonPanel.add(zoomAllButton);
        tableLayout.setRowPadding(0, new Insets(0,0,0,0));
        buttonPanel.add(propertiesButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(printButton);
        buttonPanel.add(new JPanel());
        buttonPanel.add(getHelpButton());

        return buttonPanel;
    }

    protected void createUI(final ChartPanel chartPanel, final JPanel optionsPanel, BindingContext bindingContext) {
        roiMaskSelector = new RoiMaskSelector(bindingContext);

        final JPanel extendedOptionsPanel = GridBagUtils.createPanel();
        GridBagConstraints extendedOptionsPanelConstraints = GridBagUtils.createConstraints("insets.left=4,insets.right=2,anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1");
        GridBagUtils.addToPanel(extendedOptionsPanel, new JSeparator(), extendedOptionsPanelConstraints, "gridy=0");
        GridBagUtils.addToPanel(extendedOptionsPanel, roiMaskSelector.createPanel(), extendedOptionsPanelConstraints, "gridy=1,insets.left=-4");
        GridBagUtils.addToPanel(extendedOptionsPanel, optionsPanel, extendedOptionsPanelConstraints, "insets.left=0,insets.right=0,gridy=2,fill=VERTICAL,fill=HORIZONTAL,weighty=1");
        GridBagUtils.addToPanel(extendedOptionsPanel, new JSeparator(), extendedOptionsPanelConstraints, "insets.left=4,insets.right=2,gridy=5,anchor=SOUTHWEST");

        final JScrollPane optionsScrollPane = new SimpleScrollPane(extendedOptionsPanel,
                                                                   ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                                   ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        optionsScrollPane.setBorder(null);

        final JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(createTopPanel(), BorderLayout.NORTH);
        rightPanel.add(optionsScrollPane, BorderLayout.CENTER);
        rightPanel.add(createChartBottomPanel(chartPanel), BorderLayout.SOUTH);

        final ImageIcon collapseIcon = UIUtils.loadImageIcon("icons/PanelRight12.png");
        final ImageIcon collapseRolloverIcon = ToolButtonFactory.createRolloverIcon(collapseIcon);
        final ImageIcon expandIcon = UIUtils.loadImageIcon("icons/PanelLeft12.png");
        final ImageIcon expandRolloverIcon = ToolButtonFactory.createRolloverIcon(expandIcon);

        hideAndShowButton = ToolButtonFactory.createButton(collapseIcon, false);
        hideAndShowButton.setToolTipText("Collapse Options Panel");
        hideAndShowButton.setName("switchToChartButton");
        hideAndShowButton.addActionListener(new ActionListener() {

            public boolean rightPanelShown;

            @Override
            public void actionPerformed(ActionEvent e) {
                rightPanel.setVisible(rightPanelShown);
                if (rightPanelShown) {
                    hideAndShowButton.setIcon(collapseIcon);
                    hideAndShowButton.setRolloverIcon(collapseRolloverIcon);
                    hideAndShowButton.setToolTipText("Collapse Options Panel");
                } else {
                    hideAndShowButton.setIcon(expandIcon);
                    hideAndShowButton.setRolloverIcon(expandRolloverIcon);
                    hideAndShowButton.setToolTipText("Expand Options Panel");
                }
                rightPanelShown = !rightPanelShown;
            }
        });

        backgroundPanel = new JPanel(new BorderLayout());
        backgroundPanel.add(chartPanel, BorderLayout.CENTER);
        backgroundPanel.add(rightPanel, BorderLayout.EAST);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.add(backgroundPanel, new Integer(0));
        layeredPane.add(hideAndShowButton, new Integer(1));
        add(layeredPane);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        backgroundPanel.setBounds(0, 0, getWidth() - 8, getHeight() - 8);
        hideAndShowButton.setBounds(getWidth() - hideAndShowButton.getWidth() - 12, 2, 24, 24);
    }

}
