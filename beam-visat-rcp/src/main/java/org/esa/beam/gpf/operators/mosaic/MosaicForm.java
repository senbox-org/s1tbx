package org.esa.beam.gpf.operators.mosaic;

import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.JTabbedPane;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class MosaicForm extends JTabbedPane {

    private final AppContext appContext;
    private MosaicFormModel mosaicModel;
    private MosaicIOPanel ioPanel;
    private MosaicMapProjectionPanel mapProjectionPanel;
    private MosaicExpressionsPanel expressionsPanel;

    public MosaicForm(TargetProductSelector targetProductSelector, AppContext appContext) {
        this.appContext = appContext;
        mosaicModel = new MosaicFormModel();
        createUI(targetProductSelector);
    }

    private void createUI(TargetProductSelector selector) {
        ioPanel = new MosaicIOPanel(appContext, mosaicModel, selector);
        mapProjectionPanel = new MosaicMapProjectionPanel(appContext, mosaicModel);
        expressionsPanel = new MosaicExpressionsPanel(appContext, mosaicModel);

        addTab("I/O Parameters", ioPanel); /*I18N*/
        addTab("Map Projection Definition", mapProjectionPanel); /*I18N*/
        addTab("Variables & Conditions", expressionsPanel);  /*I18N*/
    }


    MosaicFormModel getFormModel() {
        return mosaicModel;
    }

    void prepareShow() {
        ioPanel.prepareShow();
        mapProjectionPanel.prepareShow();
    }

    void prepareHide() {
        mapProjectionPanel.prepareHide();
        ioPanel.prepareHide();
    }

}
