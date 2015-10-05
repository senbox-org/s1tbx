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

import org.esa.snap.framework.ui.AppContext;
import org.esa.snap.framework.ui.color.ColorComboBox;
import org.esa.snap.graphbuilder.rcp.dialogs.ProductSetPanel;
import org.esa.snap.graphbuilder.rcp.dialogs.PromptDialog;
import org.esa.snap.graphbuilder.rcp.dialogs.support.FileTable;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 */
public class GraphProductSetPanel extends ProductSetPanel {

    private final ColorComboBox colorCombo = new ColorComboBox();
    private final TimeSeriesSettingsDlg settingsDlg;
    private String title;

    public GraphProductSetPanel(final AppContext theAppContext,
                                final TimeSeriesSettingsDlg settingsDlg, final GraphData graphData,
                                final boolean addDeleteButton) {
        super(theAppContext, graphData.getTitle(), new FileTable(), false, true);
        this.title = graphData.getTitle();
        this.settingsDlg = settingsDlg;

        colorCombo.setSelectedColor(graphData.getColor());
        getButtonPanel().add(colorCombo);

        getButtonPanel().add(createRenameButton(this));
        if (addDeleteButton) {
            getButtonPanel().add(createDeleteButton(this));
        }
    }

    private static JButton createRenameButton(final GraphProductSetPanel panel) {
        final JButton renameBtn = DialogUtils.createButton("renameBtn", "Rename", null, panel, DialogUtils.ButtonStyle.Text);
        renameBtn.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final PromptDialog dlg = new PromptDialog("Rename", "Name", panel.getTitle(), false);
                dlg.show();
                if (dlg.IsOK()) {
                    panel.setTitle(dlg.getValue());
                }
            }
        });
        return renameBtn;
    }

    private JButton createDeleteButton(final GraphProductSetPanel panel) {
        final JButton deleteBtn = DialogUtils.createButton("deleteBtn", "Delete Graph", null, panel, DialogUtils.ButtonStyle.Text);
        deleteBtn.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                settingsDlg.removeGraphPanel(panel);
            }
        });
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
