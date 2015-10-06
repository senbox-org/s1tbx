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

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.diagram.DiagramGraph;
import org.esa.snap.util.io.SnapFileFilter;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

class TimeSeriesExportAction extends AbstractAction {

    private final TimeSeriesToolView toolView;

    public TimeSeriesExportAction(final TimeSeriesToolView toolView) {
        super("exportTimeSeries");
        this.toolView = toolView;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        export();
    }

    private void export() {
        final TimeSeriesDiagram diagram = toolView.getDiagram();
        if (diagram == null) {
            return;
        }

        final DiagramGraph[] graphs = diagram.getGraphs();

        DiagramGraphIO.writeGraphs(SnapApp.getDefault().getMainFrame(),
                                   "Export Pin",
                                   new SnapFileFilter[]{DiagramGraphIO.CSV_FILE_FILTER, DiagramGraphIO.DBF_FILE_FILTER},
                                   Config.instance().preferences(), graphs);
    }
}