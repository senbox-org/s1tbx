/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.visat.dialogs;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.internal.CheckBoxEditor;
import com.bc.ceres.swing.binding.internal.NumericEditor;
import com.bc.ceres.swing.binding.internal.SingleSelectionEditor;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;
import com.bc.ceres.swing.binding.internal.TextFieldEditor;
import com.bc.jexp.Namespace;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import com.bc.jexp.impl.ParserImpl;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeList;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.visat.VisatApp;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BandMathsDialog extends ModalDialog {

    private static final String PROPERTY_NAME_PRODUCT = "productName";
    private static final String PROPERTY_NAME_EXPRESSION = "expression";
    private static final String PROPERTY_NAME_NO_DATA_VALUE = "noDataValue";
    private static final String PROPERTY_NAME_NO_DATA_VALUE_USED = "noDataValueUsed";
    private static final String PROPERTY_NAME_SAVE_EXPRESSION_ONLY = "saveExpressionOnly";
    private static final String PROPERTY_NAME_BAND_NAME = "bandName";
    private static final String PROPERTY_NAME_BAND_DESC = "bandDescription";
    private static final String PROPERTY_NAME_BAND_UNIT = "bandUnit";
    private static final String PROPERTY_NAME_BAND_WAVELENGTH = "bandWavelength";

    private final VisatApp visatApp;
    private final ProductNodeList<Product> productsList;
    private final BindingContext bindingContext;
    private Product targetProduct;

    private String productName;
    private String expression = "";
    private double noDataValue;
    private boolean noDataValueUsed;
    private boolean saveExpressionOnly;
    private String bandName;
    private String bandDescription = "";
    private String bandUnit = "";
    private float bandWavelength = 0;

    private static int numNewBands = 0;

    public BandMathsDialog(final VisatApp visatApp, Product currentProduct, ProductNodeList<Product> productsList,
                                String helpId) {
        super(visatApp.getMainFrame(), "Band Maths", ID_OK_CANCEL_HELP, helpId); /* I18N */
        Guardian.assertNotNull("currentProduct", currentProduct);
        Guardian.assertNotNull("productsList", productsList);
        Guardian.assertGreaterThan("productsList must be not empty", productsList.size(), 0);
        this.visatApp = visatApp;
        targetProduct = currentProduct;
        this.productsList = productsList;
        bindingContext = createBindingContext();
        makeUI();
    }

    @Override
    protected void onOK() {
        final String validMaskExpression;
        try {
            Product[] products = getCompatibleProducts();
            int defaultProductIndex = Arrays.asList(products).indexOf(targetProduct);
            validMaskExpression = BandArithmetic.getValidMaskExpression(getExpression(), products, defaultProductIndex, null);
        } catch (ParseException e) {
            String errorMessage = "The band could not be created.\nAn parse error occurred:\n" + e.getMessage(); /*I18N*/
            visatApp.showErrorDialog(errorMessage);
            hide();
            return;
        }

        final int width = targetProduct.getSceneRasterWidth();
        final int height = targetProduct.getSceneRasterHeight();

        Band band;
        if (saveExpressionOnly) {
            band = new VirtualBand(getBandName(), ProductData.TYPE_FLOAT32, width, height, getExpression());
            setBandProperties(band, validMaskExpression);
        } else {
            band = new Band(getBandName(), ProductData.TYPE_FLOAT32, width, height);
            setBandProperties(band, "");
        }

        targetProduct.addBand(band);

        if (!saveExpressionOnly) {
            String expression = getExpression();
            if (validMaskExpression != null && !validMaskExpression.isEmpty()) {
                expression = "(" + validMaskExpression + ") ? (" + expression + ") : NaN";
            }
            band.setSourceImage(VirtualBand.createVirtualSourceImage(band, expression));
        } else {
            checkExpressionForExternalReferences(getExpression());
        }

        hide();
        band.setModified(true);
        if (visatApp.getPreferences().getPropertyBool(VisatApp.PROPERTY_KEY_AUTO_SHOW_NEW_BANDS, true)) {
            visatApp.openProductSceneView(band);
        }
    }

    private void setBandProperties(Band band, String validMaskExpression) {
        band.setDescription(bandDescription);
        band.setUnit(bandUnit);
        band.setSpectralWavelength(bandWavelength);
        band.setGeophysicalNoDataValue(noDataValue);
        band.setNoDataValueUsed(noDataValueUsed);
        band.setValidPixelExpression(validMaskExpression);
    }

    @Override
    protected boolean verifyUserInput() {
        if (!isValidExpression()) {
            showErrorDialog("Please check the band maths expression you have entered.\nIt is not valid."); /*I18N*/
            return false;
        }

        if (isTargetBandReferencedInExpression()) {
            showErrorDialog("You cannot reference the target band '" + getBandName() +
                            "' within the expression.");
            return false;
        }
        return super.verifyUserInput();
    }

    private void makeUI() {
        JButton editExpressionButton = new JButton("Edit Expression...");
        editExpressionButton.setName("editExpressionButton");
        editExpressionButton.addActionListener(createEditExpressionButtonListener());

        final JPanel panel = GridBagUtils.createPanel();
        int line = 0;
        GridBagConstraints gbc = new GridBagConstraints();

        JComponent[] components = createComponents(PROPERTY_NAME_PRODUCT, SingleSelectionEditor.class);
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, components[1], gbc, "gridwidth=3, fill=BOTH, weightx=1");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, components[0], gbc, "insets.top=3, gridwidth=3, fill=BOTH, anchor=WEST");

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_BAND_NAME, TextFieldEditor.class);
        GridBagUtils.addToPanel(panel, components[1], gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, components[0], gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_BAND_DESC, TextFieldEditor.class);
        GridBagUtils.addToPanel(panel, components[1], gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, components[0], gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_BAND_UNIT, TextFieldEditor.class);
        GridBagUtils.addToPanel(panel, components[1], gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, components[0], gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_BAND_WAVELENGTH, TextFieldEditor.class);
        GridBagUtils.addToPanel(panel, components[1], gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, components[0], gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        components = createComponents(PROPERTY_NAME_SAVE_EXPRESSION_ONLY, CheckBoxEditor.class);
        GridBagUtils.addToPanel(panel, components[0], gbc, "insets.top=3, gridwidth=3, fill=HORIZONTAL, anchor=EAST");

        gbc.gridy = ++line;
        JPanel nodataPanel = new JPanel(new BorderLayout());
        components = createComponents(PROPERTY_NAME_NO_DATA_VALUE_USED, CheckBoxEditor.class);
        nodataPanel.add(components[0], BorderLayout.WEST);
        components = createComponents(PROPERTY_NAME_NO_DATA_VALUE, NumericEditor.class);
        nodataPanel.add(components[0]);
        GridBagUtils.addToPanel(panel, nodataPanel, gbc,
                                "weightx=1, insets.top=3, gridwidth=3, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;

        JLabel expressionLabel = new JLabel("Band maths expression:");
        JTextArea expressionArea = new JTextArea();
        expressionArea.setRows(3);
        TextComponentAdapter textComponentAdapter = new TextComponentAdapter(expressionArea);
        bindingContext.bind(PROPERTY_NAME_EXPRESSION, textComponentAdapter);
        components = createComponents(PROPERTY_NAME_EXPRESSION, TextFieldEditor.class);
        GridBagUtils.addToPanel(panel, expressionLabel, gbc, "insets.top=3, gridwidth=3, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, expressionArea, gbc,
                                "weighty=1, insets.top=3, gridwidth=3, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, editExpressionButton, gbc,
                                "weighty=0, insets.top=3, gridwidth=3, fill=NONE, anchor=EAST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, new JLabel(""), gbc,
                                "insets.top=10, weightx=1, weighty=1, gridwidth=3, fill=BOTH, anchor=WEST");

        setContent(panel);
    }

    private JComponent[] createComponents(String propertyName, Class<? extends PropertyEditor> editorClass) {
        PropertyDescriptor descriptor = bindingContext.getPropertySet().getDescriptor(propertyName);
        PropertyEditor editor = PropertyEditorRegistry.getInstance().getPropertyEditor(editorClass.getName());
        return editor.createComponents(descriptor, bindingContext);
    }

    private BindingContext createBindingContext() {
        final PropertyContainer container = PropertyContainer.createObjectBacked(this);
        final BindingContext context = new BindingContext(container);

        container.addPropertyChangeListener(PROPERTY_NAME_PRODUCT, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                targetProduct = productsList.getByDisplayName(productName);
            }
        }
        );

        productName = targetProduct.getDisplayName();
        PropertyDescriptor descriptor = container.getDescriptor(PROPERTY_NAME_PRODUCT);
        descriptor.setValueSet(new ValueSet(productsList.getDisplayNames()));
        descriptor.setDisplayName("Target product");

        descriptor = container.getDescriptor(PROPERTY_NAME_BAND_NAME);
        descriptor.setDisplayName("Name");
        descriptor.setDescription("The name for the new band.");
        descriptor.setNotEmpty(true);
        descriptor.setValidator(new ProductNodeNameValidator());
        while(targetProduct.containsRasterDataNode("new_band_" + (++numNewBands))) {
            // loop
        }
        descriptor.setDefaultValue("new_band_" + (numNewBands));

        descriptor = container.getDescriptor(PROPERTY_NAME_BAND_DESC);
        descriptor.setDisplayName("Description");
        descriptor.setDescription("The description for the new band.");

        descriptor = container.getDescriptor(PROPERTY_NAME_BAND_UNIT);
        descriptor.setDisplayName("Unit");
        descriptor.setDescription("The physical unit for the new band.");

        descriptor = container.getDescriptor(PROPERTY_NAME_BAND_WAVELENGTH);
        descriptor.setDisplayName("Spectral wavelength");
        descriptor.setDescription("The physical unit for the new band.");

        descriptor = container.getDescriptor(PROPERTY_NAME_EXPRESSION);
        descriptor.setDisplayName("Band maths expression");
        descriptor.setDescription("Band maths expression");
        descriptor.setNotEmpty(true);

        descriptor = container.getDescriptor(PROPERTY_NAME_SAVE_EXPRESSION_ONLY);
        descriptor.setDisplayName("Virtual (save expression only, don't write data)");
        descriptor.setDefaultValue(Boolean.TRUE);

        descriptor = container.getDescriptor(PROPERTY_NAME_NO_DATA_VALUE_USED);
        descriptor.setDisplayName("Replace NaN and infinity results by");
        descriptor.setDefaultValue(Boolean.TRUE);

        descriptor = container.getDescriptor(PROPERTY_NAME_NO_DATA_VALUE);
        descriptor.setDefaultValue(Double.NaN);

        container.setDefaultValues();

        context.addPropertyChangeListener(PROPERTY_NAME_SAVE_EXPRESSION_ONLY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final boolean saveExpressionOnly = (Boolean) context.getBinding(
                        PROPERTY_NAME_SAVE_EXPRESSION_ONLY).getPropertyValue();
                if (!saveExpressionOnly) {
                    context.getBinding(PROPERTY_NAME_NO_DATA_VALUE_USED).setPropertyValue(true);
                }
            }
        });
        context.bindEnabledState(PROPERTY_NAME_NO_DATA_VALUE_USED, false, PROPERTY_NAME_SAVE_EXPRESSION_ONLY,
                                 Boolean.FALSE);
        context.bindEnabledState(PROPERTY_NAME_NO_DATA_VALUE, true, PROPERTY_NAME_NO_DATA_VALUE_USED, Boolean.TRUE);

        return context;
    }

    private String getBandName() {
        return bandName.trim();
    }

    private String getExpression() {
        return expression.trim();
    }

    private Product[] getCompatibleProducts() {
        List<Product> compatibleProducts = new ArrayList<Product>(productsList.size());
        compatibleProducts.add(targetProduct);
        final float geolocationEps = getGeolocationEps();
        Debug.trace("BandMathsDialog.geolocationEps = " + geolocationEps);
        Debug.trace("BandMathsDialog.getCompatibleProducts:");
        Debug.trace("  comparing: " + targetProduct.getName());
        for (int i = 0; i < productsList.size(); i++) {
            final Product product = productsList.getAt(i);
            if (targetProduct != product) {
                Debug.trace("  with:      " + product.getDisplayName());
                final boolean isCompatibleProduct = targetProduct.isCompatibleProduct(product, geolocationEps);
                Debug.trace("  result:    " + isCompatibleProduct);
                if (isCompatibleProduct) {
                    compatibleProducts.add(product);
                }
            }
        }
        return compatibleProducts.toArray(new Product[compatibleProducts.size()]);
    }

    private float getGeolocationEps() {
        return (float) visatApp.getPreferences().getPropertyDouble(VisatApp.PROPERTY_KEY_GEOLOCATION_EPS,
                                                                   VisatApp.PROPERTY_DEFAULT_GEOLOCATION_EPS);
    }

    private ActionListener createEditExpressionButtonListener() {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Product[] compatibleProducts = getCompatibleProducts();
                ProductExpressionPane pep = ProductExpressionPane.createGeneralExpressionPane(compatibleProducts,
                                                                                              targetProduct,
                                                                                              visatApp.getPreferences());
                pep.setCode(getExpression());
                int status = pep.showModalDialog(getJDialog(), "Band Maths Expression Editor");
                if (status == ModalDialog.ID_OK) {
                    bindingContext.getBinding(PROPERTY_NAME_EXPRESSION).setPropertyValue(pep.getCode());
                }
                pep.dispose();
            }
        };
    }

    private void checkExpressionForExternalReferences(String expression) {
        final Product[] compatibleProducts = getCompatibleProducts();
        if (compatibleProducts.length > 1) {
            int defaultIndex = Arrays.asList(compatibleProducts).indexOf(targetProduct);
            RasterDataNode[] rasters = null;
            try {
                rasters = BandArithmetic.getRefRasters(expression, compatibleProducts, defaultIndex);
            } catch (ParseException ignored) {
            }
            if (rasters != null && rasters.length > 0) {
                Set<Product> externalProducts = new HashSet<Product>(compatibleProducts.length);
                for (RasterDataNode rdn : rasters) {
                    Product product = rdn.getProduct();
                    if (product != targetProduct) {
                        externalProducts.add(product);
                    }
                }
                if (!externalProducts.isEmpty()) {
                    visatApp.showWarningDialog("The entered maths expression references multiple products.\n"
                                               + "It will cause problems unless the session is restored as is.\n\n"
                                               + "Note: You can save the session from the file menu.");
                }
            }
        }
    }

    private boolean isValidExpression() {
        final Product[] products = getCompatibleProducts();
        if (products.length == 0 || getExpression().isEmpty()) {
            return false;
        }

        final int defaultIndex = Arrays.asList(products).indexOf(targetProduct);
        final Namespace namespace = BandArithmetic.createDefaultNamespace(products,
                                                                          defaultIndex == -1 ? 0 : defaultIndex);
        final Parser parser = new ParserImpl(namespace, false);
        try {
            parser.parse(getExpression());
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    private boolean isTargetBandReferencedInExpression() {
        final Product[] products = getCompatibleProducts();

        final int defaultIndex = Arrays.asList(products).indexOf(visatApp.getSelectedProduct());
        final Namespace namespace = BandArithmetic.createDefaultNamespace(products,
                                                                          defaultIndex == -1 ? 0 : defaultIndex);
        final Parser parser = new ParserImpl(namespace, false);
        try {
            final Term term = parser.parse(getExpression());
            final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
            String bName = getBandName();
            if (targetProduct.containsRasterDataNode(bName)) {
                for (final RasterDataSymbol refRasterDataSymbol : refRasterDataSymbols) {
                    final String refRasterName = refRasterDataSymbol.getRaster().getName();
                    if (bName.equalsIgnoreCase(refRasterName)) {
                        return true;
                    }
                }
            }
        } catch (ParseException e) {
            return false;
        }
        return false;
    }

    private class ProductNodeNameValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            final String name = (String) value;
            if (!ProductNode.isValidNodeName(name)) {
                final String message = MessageFormat.format("The band name ''{0}'' is not valid.\n\n"
                                                            + "Names must not start with a dot and must not\n"
                                                            + "contain any of the following characters: \\/:*?\"<>|",
                                                            name);
                throw new ValidationException(message);
            }
            if (targetProduct.containsRasterDataNode(name)) {
                throw new ValidationException("The band name must be unique within the product scope.\n"
                                              + "The scope comprises bands and tie-point grids.");
            }
        }
    }
}
