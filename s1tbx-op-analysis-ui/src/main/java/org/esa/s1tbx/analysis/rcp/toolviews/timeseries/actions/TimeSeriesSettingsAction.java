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

import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.TimeSeriesSettings;
import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.TimeSeriesToolView;
import org.esa.snap.rcp.SnapApp;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class TimeSeriesSettingsAction extends AbstractAction {

    private final TimeSeriesToolView toolView;
    private final TimeSeriesSettings settings;

    public TimeSeriesSettingsAction(final TimeSeriesToolView toolView, final TimeSeriesSettings settings) {
        super("exportTimeSeries");
        this.toolView = toolView;
        this.settings = settings;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final TimeSeriesSettingsDlg settingsDlg = new TimeSeriesSettingsDlg(SnapApp.getDefault().getMainFrame(),
                "Time Series Analysis Settings",
                "help", settings, toolView);
        settingsDlg.show();
    }
}