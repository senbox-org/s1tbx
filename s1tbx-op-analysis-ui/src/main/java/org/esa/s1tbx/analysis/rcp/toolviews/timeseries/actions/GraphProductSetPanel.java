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
package org.esa.s1tbx.analysis.rcp.toolviews.timeseries.actions;

import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.GraphData;
import org.esa.snap.graphbuilder.rcp.dialogs.ProductSetPanel;
import org.esa.snap.graphbuilder.rcp.dialogs.PromptDialog;
import org.esa.snap.graphbuilder.rcp.dialogs.support.FileModel;
import org.esa.snap.graphbuilder.rcp.dialogs.support.FileTable;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.color.ColorComboBox;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;

/**
 *
 */
public class GraphProductSetPanel extends ProductSetPanel {

    private final ColorComboBox colorCombo = new ColorComboBox();
    private final TimeSeriesSettingsDlg settingsDlg;
    private final AbstractButton filterButton;
    private String title;

    public GraphProductSetPanel(final AppContext theAppContext,
                                final TimeSeriesSettingsDlg settingsDlg, final GraphData graphData,
                                final boolean addDeleteButton) {
        super(theAppContext, graphData.getTitle(), new FileTable(new FileModel(), new Dimension(500, 250)), false, true);
        this.title = graphData.getTitle();
        this.settingsDlg = settingsDlg;
        final JPanel buttonPanel = getButtonPanel();

        filterButton = DialogUtils.createButton("filterButton", "Filter bands",
                UIUtils.loadImageIcon("icons/Filter24.gif"), null, DialogUtils.ButtonStyle.Icon);
        filterButton.setEnabled(false);
        filterButton.addActionListener(e -> {
            settingsDlg.onApply();
            TimeSeriesFilterAction action = new TimeSeriesFilterAction(settingsDlg.getToolView(), settingsDlg.getToolView().getSettings());
            action.actionPerformed(e);
        });
        buttonPanel.add(filterButton);

        colorCombo.setSelectedColor(graphData.getColor());
        //buttonPanel.add(colorCombo);

        buttonPanel.add(createRenameButton(this));
        buttonPanel.add(addDeleteButton ? createDeleteButton(this) : new JLabel("       "));
    }

    @Override
    protected void updateComponents() {
        super.updateComponents();

        if(settingsDlg != null) {
            final int rowCount = getFileCount();

            final boolean enableButtons = (rowCount > 0);
            if (filterButton != null)
                filterButton.setEnabled(enableButtons);
        }
    }

    private static JButton createRenameButton(final GraphProductSetPanel panel) {
        final JButton renameBtn = DialogUtils.createButton("renameBtn", "Rename", null, panel, DialogUtils.ButtonStyle.Text);
        renameBtn.addActionListener(e -> {
            final PromptDialog dlg = new PromptDialog("Rename", "Name", panel.getTitle(), PromptDialog.TYPE.TEXTFIELD);
            dlg.show();
            if (dlg.IsOK()) {
                try {
                    panel.setTitle(dlg.getValue("Name"));
                } catch (Exception ex) {
                    Dialogs.showError(ex.getMessage());
                }
            }
        });
        return renameBtn;
    }

    private JButton createDeleteButton(final GraphProductSetPanel panel) {
        final JButton deleteBtn = DialogUtils.createButton("deleteBtn", "Delete", null, panel, DialogUtils.ButtonStyle.Text);
        deleteBtn.addActionListener(e -> settingsDlg.removeGraphPanel(panel));
        return deleteBtn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
        setBorderTitle(title);
    }

    public Color getColor() {
        return colorCombo.getSelectedColor();
    }
}
