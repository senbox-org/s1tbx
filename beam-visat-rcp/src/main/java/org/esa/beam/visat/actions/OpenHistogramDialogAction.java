package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.toolviews.stat.StatisticDialogHelper;
import org.esa.beam.visat.toolviews.stat.StatisticsToolView;

public class OpenHistogramDialogAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        StatisticDialogHelper.openStatisticsDialog(StatisticsToolView.HISTOGRAM_TAB_INDEX);
    }

    @Override
    public void updateState(final CommandEvent event) {
        StatisticDialogHelper.enableCommandIfRasterSelected(VisatApp.getApp(), event);
    }
}
