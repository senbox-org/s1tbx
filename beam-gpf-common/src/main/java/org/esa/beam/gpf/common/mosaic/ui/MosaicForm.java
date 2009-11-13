package org.esa.beam.gpf.common.mosaic.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.swing.BindingContext;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.JPanel;
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
        final MosaicIOPanel ioPanel = new MosaicIOPanel(appContext, selector);
        final JPanel mapProjectionPanel = new MosaicMapProjectionPanel(appContext);

        // todo: rq/rq - implement expression context (20091113)
        final ExpressionContext expressionContext = new ExpressionContext() {
            @Override
            public Product getProduct() {
                return null;
            }

            @Override
            public void addListener(Listener listener) {
            }
        };

        final JPanel variablesAndConditionsPanel =
                new MosaicVariablesAndConditionsPanel(appContext, bindingContext, expressionContext);

        addTab("I/O Parameters", ioPanel); /*I18N*/
        addTab("Map Projection Definition", mapProjectionPanel); /*I18N*/
        addTab("Variables & Conditions", variablesAndConditionsPanel);  /*I18N*/

        setEnabledAt(2, expressionContext.getProduct() != null);
        expressionContext.addListener(new ExpressionContext.Listener() {
            @Override
            public void contextChanged(ExpressionContext context) {
                setEnabledAt(2, context.getProduct() != null);
            }
        });
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
