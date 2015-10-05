/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.analysis.rcp.toolviews.timeseries;

import org.esa.snap.framework.ui.ModalDialog;
import org.esa.snap.framework.ui.ModelessDialog;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.SnapApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Parameter settings for the time series
 */
public class TimeSeriesSettingsDlg extends ModalDialog {

    private final JCheckBox showGridCB = new JCheckBox("Show Grid");
    private final JCheckBox showLegendCB = new JCheckBox("Show Legend");
    private final GridBagConstraints glGbc = DialogUtils.createGridBagConstraints();
    private final JPanel graphListPanel = new JPanel(new GridBagLayout());

    private final List<GraphProductSetPanel> graphList = new ArrayList<>(2);

    private final TimeSeriesToolView view;
    final TimeSeriesSettings settings;

    public TimeSeriesSettingsDlg(final Window parent, final String title, final String helpID,
                                 final TimeSeriesSettings settings, final TimeSeriesToolView view) {
        super(parent, title, ModelessDialog.ID_APPLY_CLOSE, helpID);
        this.settings = settings;
        this.view = view;
        this.getJDialog().setResizable(false);

        final List<GraphData> graphDataLists = settings.getGraphDataList();
        int cnt = 1;
        for (GraphData graphData : graphDataLists) {
            final boolean addDeleteButton = cnt > 1;
            final GraphProductSetPanel productListPanel = new GraphProductSetPanel(SnapApp.getDefault().getAppContext(),
                                                                                   this, graphData, addDeleteButton);
            productListPanel.setProductFileList(graphData.getFileList());
            graphList.add(productListPanel);
            ++cnt;
        }

        initUI();

        showGridCB.setSelected(settings.isShowingGrid());
        showLegendCB.setSelected(settings.isShowingLegend());
    }

    private void initUI() {
        final JPanel content = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        final JPanel optionsPanel = new JPanel();
        optionsPanel.add(showGridCB);
        optionsPanel.add(showLegendCB);

        DialogUtils.addComponent(content, gbc, "", optionsPanel);
        gbc.gridy++;

        final JButton addGraphBtn = DialogUtils.createButton("addGraphBtn", "Add Graph", null, content, DialogUtils.ButtonStyle.Text);
        final TimeSeriesSettingsDlg settingsDlg = this;
        addGraphBtn.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final int cnt = graphList.size() + 1;
                final GraphProductSetPanel productListPanel = new GraphProductSetPanel(SnapApp.getDefault().getAppContext(),
                                                                                       settingsDlg, new GraphData("Graph " + cnt), true);
                graphList.add(productListPanel);
                graphListPanel.add(productListPanel, glGbc);

                glGbc.gridy++;
                graphListPanel.revalidate();
            }
        });
        gbc.gridx = 0;
        content.add(addGraphBtn, gbc);

        final JScrollPane scrollPane = new JScrollPane(graphListPanel);
        scrollPane.setPreferredSize(new Dimension(680, 300));
        DialogUtils.addComponent(content, gbc, "", scrollPane);

        for (GraphProductSetPanel productListPanel : graphList) {
            graphListPanel.add(productListPanel, glGbc);
            glGbc.gridy++;
        }

        setContent(content);
    }

    public void removeGraphPanel(final GraphProductSetPanel productListPanel) {
        graphList.remove(productListPanel);
        graphListPanel.remove(productListPanel);
        graphListPanel.revalidate();
    }

    public List<GraphData> getProductLists() {
        final List<GraphData> graphDataList = new ArrayList<>(graphList.size());
        for (GraphProductSetPanel panel : graphList) {
            graphDataList.add(new GraphData(panel.getTitle(), panel.getFileList(), panel.getColor()));
        }
        return graphDataList;
    }

    @Override
    public void onApply() {
        settings.setGridShown(showGridCB.isSelected());
        settings.setLegendShown(showLegendCB.isSelected());
        settings.setGraphDataList(getProductLists());
        view.refresh();
    }

    @Override
    public void onClose() {
        onApply();
        super.onClose();
    }
}
