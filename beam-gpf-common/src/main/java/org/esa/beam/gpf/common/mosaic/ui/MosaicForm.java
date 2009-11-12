package org.esa.beam.gpf.common.mosaic.ui;

import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.JTabbedPane;
import java.awt.Component;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class MosaicForm extends JTabbedPane {

    private final AppContext appContext;

    public MosaicForm(TargetProductSelector targetProductSelector, AppContext appContext) {
        this.appContext = appContext;
        createUI(targetProductSelector);
    }

    private void createUI(TargetProductSelector selector) {
        addTab("I/O Parameters", new MosaicIOPanel(appContext, selector)); /*I18N*/
        addTab("Map Projection Definition", new MosaicMapProjectionPanel()); /*I18N*/
        addTab("Variables & Conditions", new MosaicVariablesAndConditionsPanel(appContext));  /*I18N*/
    }


    void prepareShow() {
        // todo init source product selectors
//        sourceProductSelector.initProducts();
    }

    void prepareHide() {
        // todo release products of source product selectors
//        sourceProductSelector.releaseProducts();
    }

}
