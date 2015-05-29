package org.jlinda.nest.dat;

import org.esa.snap.graphbuilder.rcp.dialogs.GraphBuilderDialog;
import org.esa.snap.rcp.SnapApp;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class UnwrapAction extends AbstractAction {
    
    @Override
    public void actionPerformed(ActionEvent event) {

        final GraphBuilderDialog dialog = new GraphBuilderDialog(new SnapApp.SnapContext(),
                "Unwrapping Tiles", "UnwrapOp", false);
        dialog.show();

        final File graphPath = GraphBuilderDialog.getInternalGraphFolder();
        final File graphFile =  new File(graphPath, "UnwrapTileGraph.xml");

        dialog.LoadGraph(graphFile);
    }
    
}
