package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueContainerFactory;
import com.bc.ceres.binding.swing.PropertyPane;
import com.bc.ceres.binding.swing.SwingBindingContext;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.TableLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.util.HashMap;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 * <p/>
 * <p>A default dialog for a registered {@link org.esa.beam.framework.gpf.Operator}.
 * The dialog
 * is limited to single source product and single target product.</p>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class DefaultSingleTargetProductDialog extends SingleTargetProductDialog {
    private final String operatorName;
    private SourceProductSelector sourceProductSelector;
    private HashMap<String, Object> parameterMap;
    private JPanel form;

    public static SingleTargetProductDialog createDefaultDialog(String operatorName, AppContext appContext) {
        return new DefaultSingleTargetProductDialog(operatorName, appContext, operatorName, null);
    }

    public DefaultSingleTargetProductDialog(String operatorName, AppContext appContext, String title, String helpID) {
        super(appContext, title, helpID);
        this.operatorName = operatorName;

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("operatorName");
        }


        ValueContainerFactory factory = new ValueContainerFactory(new ParameterDescriptorFactory());
        parameterMap = new HashMap<String, Object>();
        ValueContainer valueContainer = factory.createMapBackedValueContainer(operatorSpi.getOperatorClass(), parameterMap);
        SwingBindingContext context = new SwingBindingContext(valueContainer);

        sourceProductSelector = new SourceProductSelector(getAppContext());
        PropertyPane propertyPane = new PropertyPane(context);
        JPanel parametersPanel = propertyPane.createPanel();
        parametersPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(3, 3);

        form = new JPanel(tableLayout);
        form.add(sourceProductSelector.createDefaultPanel());
        form.add(getTargetProductSelector().createDefaultPanel());
        form.add(parametersPanel);
        form.add(tableLayout.createVerticalSpacer());
    }

    @Override
    public int show() {
        sourceProductSelector.initProducts();
        if (sourceProductSelector.getProductCount() > 0) {
            sourceProductSelector.setSelectedIndex(0);
        }
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        sourceProductSelector.releaseProducts();
        super.hide();
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        return GPF.createProduct(operatorName, parameterMap, sourceProductSelector.getSelectedProduct());  //To change body of implemented methods use File | Settings | File Templates.
    }
}
