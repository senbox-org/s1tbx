package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueContainerFactory;
import com.bc.ceres.binding.swing.PropertyPane;
import com.bc.ceres.binding.swing.SwingBindingContext;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.TableLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class DefaultSingleTargetProductDialog extends SingleTargetProductDialog {
    private String operatorName;
    private List<SourceProductSelector> sourceProductSelectorList;
    private Map<Field, SourceProductSelector> sourceProductSelectorMap;
    private Map<String, Object> parameterMap;
    private JTabbedPane form;

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

        // Fetch source products
        initSourceProductSelectors(operatorSpi);
        if (sourceProductSelectorList.size() > 1) {
            setSourceProductSelectorLabels();
        }

        ValueContainerFactory factory = new ValueContainerFactory(new ParameterDescriptorFactory());
        parameterMap = new HashMap<String, Object>();
        ValueContainer valueContainer = factory.createMapBackedValueContainer(operatorSpi.getOperatorClass(), parameterMap);
        SwingBindingContext context = new SwingBindingContext(valueContainer);

        PropertyPane propertyPane = new PropertyPane(context);
        JComponent processingParametersPanel = new JScrollPane(propertyPane.createPanel());
        processingParametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(3, 3);

        JPanel ioParametersPanel = new JPanel(tableLayout);
        for (SourceProductSelector selector : sourceProductSelectorList) {
            ioParametersPanel.add(selector.createDefaultPanel());
        }
        ioParametersPanel.add(getTargetProductSelector().createDefaultPanel());
        ioParametersPanel.add(tableLayout.createVerticalSpacer());

        this.form = new JTabbedPane();
        this.form.add("I/O Parameters", ioParametersPanel);
        this.form.add("Processing Parameters", processingParametersPanel);
    }

    private void initSourceProductSelectors(OperatorSpi operatorSpi) {
        sourceProductSelectorList = new ArrayList<SourceProductSelector>(3);
        sourceProductSelectorMap = new HashMap<Field, SourceProductSelector>(3);
        final Field[] fields = operatorSpi.getOperatorClass().getDeclaredFields();
        for (Field field : fields) {
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (annot != null) {
                SourceProductSelector sourceProductSelector = new SourceProductSelector(getAppContext());
                sourceProductSelectorList.add(sourceProductSelector);
                sourceProductSelectorMap.put(field, sourceProductSelector);
            }
        }
    }

    private void setSourceProductSelectorLabels() {
        for (Field field : sourceProductSelectorMap.keySet()) {
            final SourceProductSelector selector = sourceProductSelectorMap.get(field);
            String label = field.getName();
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (!annot.alias().isEmpty()) {
                label = annot.alias();
            }
            selector.getProductNameLabel().setText(label);
        }
    }

    @Override
    public int show() {
        initSourceProductSelectors();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        releaseSourceProductSelectors();
        super.hide();
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final HashMap<String, Product> sourceProducts = createSourceProductsMap();
        return GPF.createProduct(operatorName, parameterMap, sourceProducts);
    }

    private void initSourceProductSelectors() {
        for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
            sourceProductSelector.initProducts();
            if (sourceProductSelector.getProductCount() > 0) {
                sourceProductSelector.setSelectedIndex(0);
            }
        }
    }

    private void releaseSourceProductSelectors() {
        for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
            sourceProductSelector.releaseProducts();
        }
    }

    private HashMap<String, Product> createSourceProductsMap() {
        final HashMap<String, Product> sourceProducts = new HashMap<String, Product>(8);
        for (Field field : sourceProductSelectorMap.keySet()) {
            final SourceProductSelector selector = sourceProductSelectorMap.get(field);
            String key = field.getName();
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (!annot.alias().isEmpty()) {
                key = annot.alias();
            }
            sourceProducts.put(key, selector.getSelectedProduct());
        }
        return sourceProducts;
    }
}
