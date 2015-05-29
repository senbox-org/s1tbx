package org.jlinda.nest.dat;

import org.esa.snap.graphbuilder.rcp.dialogs.GraphBuilderDialog;
import org.esa.snap.rcp.SnapApp;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class StitchAction extends AbstractAction {
    
    @Override
    public void actionPerformed(ActionEvent event) {

        final GraphBuilderDialog dialog = new GraphBuilderDialog(new SnapApp.SnapContext(),
                "Stitch Unwrapped Tiles", "StitchOp", false);
        dialog.show();

        final File graphPath = GraphBuilderDialog.getInternalGraphFolder();
        final File graphFile =  new File(graphPath, "StitchTileGraph.xml");

        dialog.LoadGraph(graphFile);
    }
    
}
