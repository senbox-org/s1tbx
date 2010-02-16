package org.esa.beam.visat.toolviews.layermanager;

import org.esa.beam.framework.ui.AppContext;

import javax.swing.JComponent;

/**
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 */
abstract class AbstractLayerForm {
    private final AppContext appContext;

    protected AbstractLayerForm(AppContext appContext) {
        this.appContext = appContext;
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public abstract JComponent getFormControl();

    public abstract void updateFormControl();
}
