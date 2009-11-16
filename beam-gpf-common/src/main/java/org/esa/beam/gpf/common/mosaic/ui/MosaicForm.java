package org.esa.beam.gpf.common.mosaic.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.swing.BindingContext;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class MosaicForm extends JTabbedPane {

    private final AppContext appContext;
    private MosaicFormModel mosaicModel;

    public MosaicForm(TargetProductSelector targetProductSelector, AppContext appContext) {
        this.appContext = appContext;
        mosaicModel = new MosaicFormModel();
        createUI(targetProductSelector);
    }

    private void createUI(TargetProductSelector selector) {
        final PropertyContainer container = mosaicModel.getPropertyContainer();

        final MosaicIOPanel ioPanel = new MosaicIOPanel(appContext, container, selector);
        final JPanel mapProjectionPanel = new MosaicMapProjectionPanel(appContext);
        final BindingContext bindingContext = new BindingContext(container);
        final MosaicVariablesAndConditionsPanel variablesAndConditionsPanel =
                new MosaicVariablesAndConditionsPanel(appContext, mosaicModel);


        addTab("I/O Parameters", ioPanel); /*I18N*/
        addTab("Map Projection Definition", mapProjectionPanel); /*I18N*/
        addTab("Variables & Conditions", variablesAndConditionsPanel);  /*I18N*/
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
