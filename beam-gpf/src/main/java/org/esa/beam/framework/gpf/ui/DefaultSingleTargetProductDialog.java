package org.esa.beam.framework.gpf.ui;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;
import org.esa.beam.framework.ui.application.SelectionChangeListener;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.swing.BindingContext;

// todo (mp, 2008/04/22) add abillity to set the ProductFilter to SourceProductSelectors

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
    private String targetProductNameSuffix;

    public static SingleTargetProductDialog createDefaultDialog(String operatorName, AppContext appContext) {
        return new DefaultSingleTargetProductDialog(operatorName, appContext, operatorName, null);
    }

    public DefaultSingleTargetProductDialog(String operatorName, AppContext appContext, String title, String helpID) {
        super(appContext, title, helpID);
        this.operatorName = operatorName;
        targetProductNameSuffix = "";

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("operatorName");
        }

        // Fetch source products
        initSourceProductSelectors(operatorSpi);
        if (sourceProductSelectorList.size() > 1) {
            setSourceProductSelectorLabels();
        }

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
        sourceProductSelectorList.get(0).addSelectionChangeListener(new SelectionChangeListener() {
            public void selectionChanged(SelectionChangeEvent event) {
                final Product selectedProduct = (Product) event.getSelection().getFirstElement();
                final TargetProductSelectorModel targetProductSelectorModel = getTargetProductSelector().getModel();
                targetProductSelectorModel.setProductName(selectedProduct.getName() + getTargetProductNameSuffix());
            }
        });


        this.form = new JTabbedPane();
        this.form.add("I/O Parameters", ioParametersPanel);

        ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
        parameterMap = new HashMap<String, Object>(17);
        final ValueContainer valueContainer = ValueContainer.createMapBacked(parameterMap, operatorSpi.getOperatorClass(), parameterDescriptorFactory);
        try {
            valueContainer.setDefaultValues();
        } catch (ValidationException e) {
            e.printStackTrace();
            showErrorDialog(e.getMessage());
        }
        if (valueContainer.getModels().length > 0) {
            BindingContext context = new BindingContext(valueContainer);
            ParametersPane parametersPane = new ParametersPane(context);
            final JPanel paremetersPanel = parametersPane.createPanel();
            paremetersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
            this.form.add("Processing Parameters", new JScrollPane(paremetersPanel));
            
            for (final Field field : sourceProductSelectorMap.keySet()) {
                final SourceProductSelector sourceProductSelector = sourceProductSelectorMap.get(field);
                final String sourceAlias = field.getAnnotation(SourceProduct.class).alias();
            
                for (ValueModel valueModel : valueContainer.getModels()) {
                    ValueDescriptor parameterDescriptor = valueModel.getDescriptor();
                    String sourceId = (String) parameterDescriptor.getProperty("sourceId");
                    if (sourceId != null && (sourceId.equals(field.getName()) || sourceId.equals(sourceAlias))) {
                        SelectionChangeListener valueSetUpdater = new ValueSetUpdater(parameterDescriptor);
                        sourceProductSelector.addSelectionChangeListener(valueSetUpdater);                        
                    }
                }
            }
        }

    }

    private void initSourceProductSelectors(OperatorSpi operatorSpi) {
        sourceProductSelectorList = new ArrayList<SourceProductSelector>(3);
        sourceProductSelectorMap = new HashMap<Field, SourceProductSelector>(3);
        final Field[] fields = operatorSpi.getOperatorClass().getDeclaredFields();
        for (Field field : fields) {
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (annot != null) {
                final ProductFilter productFilter = new AnnotatedSourceProductFilter(annot);
                SourceProductSelector sourceProductSelector = new SourceProductSelector(getAppContext());
                sourceProductSelector.setProductFilter(productFilter);
                sourceProductSelectorList.add(sourceProductSelector);
                sourceProductSelectorMap.put(field, sourceProductSelector);
            }
        }
    }

    private void setSourceProductSelectorLabels() {
        for (Field field : sourceProductSelectorMap.keySet()) {
            final SourceProductSelector selector = sourceProductSelectorMap.get(field);
            String label = null;
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (!annot.label().isEmpty()) {
                label = annot.label();
            }
            if (label == null && !annot.alias().isEmpty()) {
                label = annot.alias();
            }
            if (label == null) {
                String name = field.getName();
                if (!annot.alias().isEmpty()) {
                    name = annot.alias();
                }
                label = ParametersPane.createDisplayName(name);
            }
            if (!label.endsWith(":")) {
                label += ":";
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

    public String getTargetProductNameSuffix() {
        return targetProductNameSuffix;
    }

    public void setTargetProductNameSuffix(String suffix) {
        targetProductNameSuffix = suffix;
    }

    private static class AnnotatedSourceProductFilter implements ProductFilter {

        private final SourceProduct annot;

        public AnnotatedSourceProductFilter(SourceProduct annot) {
            this.annot = annot;
        }

        public boolean accept(Product product) {

            if (!annot.type().isEmpty() && !product.getProductType().matches(annot.type())) {
                return false;
            }

            for (String bandName : annot.bands()) {
                if (!product.containsBand(bandName)) {
                    return false;
                }
            }

            return true;
        }
    }
}
