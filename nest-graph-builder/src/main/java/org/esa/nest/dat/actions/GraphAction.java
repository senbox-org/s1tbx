package org.esa.nest.dat.actions;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurationElement;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.nest.dat.plugins.graphbuilder.GraphBuilderDialog;

import java.io.File;

/**
 * <p>An action which creates a default dialog for an operator given by the
 * action property action property {@code operatorName}.</p>
 * <p>Optionally the dialog title can be set via the {@code dialogTitle} property and
 * the ID of the help page can be given using the {@code helpId} property. If not given the
 * name of the operator will be used instead. Also optional the
 * file name suffix for the target product can be given via the {@code targetProductNameSuffix} property.</p>
 */
public class GraphAction extends OperatorAction {
    private String graphFileName;

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        super.configure(config);

        graphFileName = getConfigString(config, "graphFile");
    }

    @Override
    protected ModelessDialog createOperatorDialog() {
        final GraphBuilderDialog dialog = new GraphBuilderDialog(getAppContext(), dialogTitle, getHelpId(), false);
        dialog.show();

        final File graphPath = GraphBuilderDialog.getInternalGraphFolder();
        final File graphFile =  new File(graphPath, graphFileName);

        addIcon(dialog);
        dialog.LoadGraph(graphFile);
        return dialog;
    }
}