package org.esa.beam.framework.gpf.ui;

import org.esa.beam.framework.ui.AppCommand;
import org.esa.beam.framework.ui.AppContext;
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
public class SingleTargetProductDialogAction extends AppCommand {

    private SingleTargetProductDialog dialog;

    public SingleTargetProductDialogAction() {
    }

    public SingleTargetProductDialogAction(AppContext appContext) {
        setAppContext(appContext);
    }

    @Override
    public void actionPerformed(CommandEvent event) {
        if (getAppContext() == null) {
            throw new IllegalStateException("Missing action property 'appContext'.");
        }
        final String operatorName = getProperty("operatorName", (String) null);
        if (operatorName == null) {
            throw new IllegalStateException("Missing action property 'operatorName'.");
        }
        String dialogTitle = getProperty("dialogTitle", operatorName);
        String helpId = getProperty("helpId", operatorName);
        if (dialog == null) {
            dialog = createDialog(operatorName, dialogTitle, helpId);
        }
        dialog.show();
    }

    protected SingleTargetProductDialog createDialog(String operatorName,
                                                     String dialogTitle,
                                                     String helpId) {
        return new DefaultSingleTargetProductDialog(operatorName,
                                                    getAppContext(),
                                                    dialogTitle,
                                                    helpId);
    }
}
