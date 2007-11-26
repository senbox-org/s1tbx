package org.esa.beam.framework.gpf.ui;

import org.esa.beam.framework.ui.AppCommand;
import org.esa.beam.framework.ui.command.CommandEvent;


/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 * <p/>
 * <p>An action which creates a default dialog for an operator given by the
 * action property action property {@code operatorName}. The dialog title can be set via the
 * {@code dialogTitle} property.</p>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public abstract class SingleTargetProductDialogAction extends AppCommand {

    public SingleTargetProductDialogAction() {
    }

    private SingleTargetProductDialog dialog;

    @Override
    public void actionPerformed(CommandEvent event) {
        final String operatorName = getProperty("operatorName", (String) null);
        if (operatorName == null) {
            throw new IllegalStateException("Missing action property 'operatorName'.");
        }
        String dialogTitle = getProperty("dialogTitle", operatorName);
        String helpId = getProperty("helpId", operatorName);
        if (dialog == null) {
            dialog = new DefaultSingleTargetProductDialog(operatorName, getAppContext(), dialogTitle, helpId);
        }
        dialog.show();
    }
}
