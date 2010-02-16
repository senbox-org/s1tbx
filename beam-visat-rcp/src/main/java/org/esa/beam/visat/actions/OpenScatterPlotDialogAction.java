package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.toolviews.stat.StatisticDialogHelper;
import org.esa.beam.visat.toolviews.stat.StatisticsToolView;

public class OpenScatterPlotDialogAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        StatisticDialogHelper.openStatisticsDialog(StatisticsToolView.SCATTERPLOT_TAB_INDEX);
    }

    @Override
    public void updateState(final CommandEvent event) {
        StatisticDialogHelper.enableCommandIfProductSelected(VisatApp.getApp(), event);
    }
}
