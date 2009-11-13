package org.esa.beam.gpf.common.mosaic.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.swing.BindingContext;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.JTabbedPane;
import java.util.HashMap;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class MosaicForm extends JTabbedPane {

    private final AppContext appContext;
    private final HashMap<String, Object> parameterMap;

    public MosaicForm(TargetProductSelector targetProductSelector, AppContext appContext) {
        this.appContext = appContext;
        parameterMap = new HashMap<String, Object>();
        createUI(targetProductSelector);
    }

    private void createUI(TargetProductSelector selector) {
        PropertyContainer container = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer("Mosaic",
                                                                                                          parameterMap);
        final BindingContext bindingContext = new BindingContext(container);
        addTab("I/O Parameters", new MosaicIOPanel(appContext, selector)); /*I18N*/
        addTab("Map Projection Definition", new MosaicMapProjectionPanel()); /*I18N*/
        addTab("Variables & Conditions", new MosaicVariablesAndConditionsPanel(appContext, bindingContext));  /*I18N*/
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
