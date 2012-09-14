package org.jdoris.nest.dat;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.plugins.graphbuilder.GraphBuilderDialog;

import java.io.File;

public class CplxIfgAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {

        final GraphBuilderDialog dialog = new GraphBuilderDialog(VisatApp.getApp(),
                "Interferogram Formation", "CplxIfgOp", false);
        dialog.show();

        final File graphPath = GraphBuilderDialog.getInternalGraphFolder();
        final File graphFile =  new File(graphPath, "CplxIfgGraph.xml");

        dialog.LoadGraph(graphFile);
    }
}