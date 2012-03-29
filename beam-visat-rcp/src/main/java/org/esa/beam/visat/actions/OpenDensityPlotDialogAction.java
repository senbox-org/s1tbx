/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.toolviews.stat.StatisticDialogHelper;
import org.esa.beam.visat.toolviews.stat.StatisticsToolView;

public class OpenDensityPlotDialogAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        StatisticDialogHelper.openStatisticsDialog(StatisticsToolView.DENSITYPLOT_TAB_INDEX);
    }

    @Override
    public void updateState(final CommandEvent event) {
        StatisticDialogHelper.enableCommandIfProductSelected(VisatApp.getApp(), event);
    }
}
