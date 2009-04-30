/*
 * $Id: BandArithmetikDialog.java,v 1.1 2007/04/19 10:28:47 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.dialogs;

import com.bc.jexp.Namespace;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import com.bc.jexp.impl.ParserImpl;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeList;
import org.esa.beam.framework.datamodel.ProductNodeNameValidator;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.visat.VisatApp;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BandArithmetikDialog extends ModalDialog {

    private static final String _PARAM_NAME_PRODUCT = "product";

    private final VisatApp visatApp;
    private final ProductNodeList<Product> productsList;
    private Product targetProduct;
    
    private Parameter paramProduct;
    private Parameter paramExpression;
    private Parameter paramNoDataValue;
    private Parameter paramNoDataValueUsed;
    private Parameter paramWriteData;
    private Parameter paramNewBandName;
    private Parameter paramNewBandDesc;
    private Parameter paramNewBandUnit;
    
    private static int numNewBands = 0;

    public BandArithmetikDialog(final VisatApp visatApp, Product currentProduct, ProductNodeList<Product> productsList,
                                String helpId) {
        super(visatApp.getMainFrame(), "Band Arithmetic", ID_OK_CANCEL_HELP, helpId); /* I18N */
        Guardian.assertNotNull("currentProduct", currentProduct);
        Guardian.assertNotNull("productsList", productsList);
        Guardian.assertGreaterThan("productsList must be not empty", productsList.size(), 0);
        this.visatApp = visatApp;
        targetProduct = currentProduct;
        this.productsList = productsList;
        initParameter();
        createUI();
        updateUIState(null);
    }

    @Override
    protected void onOK() {
        final boolean writeData = (Boolean) paramWriteData.getValue();
        final boolean noDataValueUsed = (Boolean) paramNoDataValueUsed.getValue();
        final float noDataValue = (Float) paramNoDataValue.getValue();
        final String expression = paramExpression.getValueAsText();
        final String bandName = getBandName();
        final String bandDesc = paramNewBandDesc.getValueAsText();
        final String bandUnit = paramNewBandUnit.getValueAsText();
        final int width = targetProduct.getSceneRasterWidth();
        final int height = targetProduct.getSceneRasterHeight();
        
        VirtualBand targetBand =  new VirtualBand(bandName, ProductData.TYPE_FLOAT32, width, height, "0");
        targetBand.setDescription(bandDesc);
        targetBand.setUnit(bandUnit);
        targetBand.setGeophysicalNoDataValue(noDataValue);
        targetBand.setNoDataValueUsed(noDataValueUsed);
        targetBand.setExpression(expression);
        targetBand.setWriteData(writeData);

        try {
            Product[] products = getCompatibleProducts();
            int defaultProductIndex = Arrays.asList(products).indexOf(targetProduct);
            final String validMaskExpression = BandArithmetic.getValidMaskExpression(expression, products, defaultProductIndex, null);
            targetBand.setValidPixelExpression(validMaskExpression);
        } catch (ParseException e) {
            String errorMessage = "The band could not be created.\nAn expression parse error occurred:\n" + e.getMessage(); /*I18N*/
            visatApp.showErrorDialog(errorMessage);
            hide();
            return;
        }
        
        if (!targetProduct.containsBand(targetBand.getName())) {
            targetProduct.addBand(targetBand);
        }

        hide();
        targetBand.setModified(true);
        if (visatApp.getPreferences().getPropertyBool(VisatApp.PROPERTY_KEY_AUTO_SHOW_NEW_BANDS, true)) {
            visatApp.openProductSceneView(targetBand);
        }
    }

    @Override
    protected boolean verifyUserInput() {
        if (!isValidExpression()) {
            showErrorDialog("Please check the expression you have entered.\nIt is not valid."); /*I18N*/
            return false;
        }

        String bandName = getBandName();
        if (isTargetBandReferencedInExpression()) {
            showErrorDialog("You cannot reference the target band '" + bandName +
                    "' within the expression.");
            return false;
        }
        return super.verifyUserInput();
    }

    private void initParameter() {
        final ParamChangeListener paramChangeListener = createParamChangeListener();

        paramProduct = new Parameter(_PARAM_NAME_PRODUCT, targetProduct.getDisplayName());
        paramProduct.setValueSet(productsList.getDisplayNames());
        paramProduct.getProperties().setValueSetBound(true);
        paramProduct.getProperties().setLabel("Target Product"); /*I18N*/
        paramProduct.addParamChangeListener(paramChangeListener);
        
        ParamProperties paramProps;
        paramProps = new ParamProperties(String.class, "new_band_" + (++numNewBands));
        paramProps.setLabel("Name"); /* I18N */
        paramProps.setDescription("The name for the new band."); /*I18N*/
        paramProps.setNullValueAllowed(false);
        paramProps.setValidatorClass(ProductNodeNameValidator.class);
        paramNewBandName = new Parameter("bandName", paramProps);

        paramProps = new ParamProperties(String.class, "");
        paramProps.setLabel("Description"); /* I18N */
        paramProps.setDescription("The description for the new band.");  /*I18N*/
        paramProps.setNullValueAllowed(true);
        paramNewBandDesc = new Parameter("bandDesc", paramProps);

        paramProps = new ParamProperties(String.class, "");
        paramProps.setLabel("Unit"); /* I18N */
        paramProps.setDescription("The physical unit for the new band."); /*I18N*/
        paramProps.setNullValueAllowed(true);
        paramNewBandUnit = new Parameter("bandUnit", paramProps);

        paramExpression = new Parameter("arithmetikExpr", "");
        paramExpression.getProperties().setLabel("Expression"); /*I18N*/
        paramExpression.getProperties().setDescription("Arithmetic expression"); /*I18N*/
        paramExpression.getProperties().setNumRows(3);

        paramWriteData = new Parameter("writeDataParam", false);
        paramWriteData.getProperties().setLabel("Write data to disk"); /*I18N*/
        paramWriteData.addParamChangeListener(paramChangeListener);

        paramNoDataValueUsed = new Parameter("noDataValueUsedParam", true);
        paramNoDataValueUsed.getProperties().setLabel("No-data value to be used on arithmetic exceptions: "); /*I18N*/
        paramNoDataValueUsed.addParamChangeListener(paramChangeListener);

        paramNoDataValue = new Parameter("noDataValueParam", 0.0F);
        paramNoDataValue.setUIEnabled(false);

        setParameterProperties();
    }

    private void createUI() {
        JButton editExpressionButton = new JButton("Edit Expression...");
        editExpressionButton.setName("editExpressionButton");
        editExpressionButton.addActionListener(createEditExpressionButtonListener());

        final JPanel panel = GridBagUtils.createPanel();
        int line = 0;
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, paramProduct.getEditor().getLabelComponent(), gbc,
                                "gridwidth=3, fill=BOTH, weightx=1");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, paramProduct.getEditor().getComponent(), gbc,
                                "insets.top=3, gridwidth=3, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, paramNewBandName.getEditor().getLabelComponent(), gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, paramNewBandName.getEditor().getComponent(), gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");
        
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, paramNewBandDesc.getEditor().getLabelComponent(), gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, paramNewBandDesc.getEditor().getComponent(), gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");
        
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, paramNewBandUnit.getEditor().getLabelComponent(), gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(panel, paramNewBandUnit.getEditor().getComponent(), gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");
        
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, paramWriteData.getEditor().getComponent(), gbc,
                                "insets.top=3, gridwidth=3, fill=HORIZONTAL, anchor=EAST");
        
        gbc.gridy = ++line;
        JPanel nodataPanel = new JPanel(new BorderLayout());
        nodataPanel.add(paramNoDataValueUsed.getEditor().getComponent(), BorderLayout.WEST);
        nodataPanel.add(paramNoDataValue.getEditor().getComponent());
        GridBagUtils.addToPanel(panel, nodataPanel, gbc,
                                "weightx=1, insets.top=3, gridwidth=3, fill=HORIZONTAL, anchor=WEST");
        
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, paramExpression.getEditor().getLabelComponent(), gbc,
                                "insets.top=3, gridwidth=3, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, paramExpression.getEditor().getComponent(), gbc,
                                "weighty=1, insets.top=3, gridwidth=3, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, editExpressionButton, gbc,
                                "weighty=0, insets.top=3, gridwidth=3, fill=NONE, anchor=EAST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, new JLabel(""), gbc, "insets.top=10, weightx=1, weighty=1, gridwidth=3, fill=BOTH, anchor=WEST");

        setContent(panel);
    }

    private void updateUIState(String parameterName) {
        paramNoDataValue.setUIEnabled((Boolean) paramNoDataValueUsed.getValue());
        if (parameterName == null) {
            return;
        }
        
        if (parameterName.equals(_PARAM_NAME_PRODUCT)) {
            String selectedProductDisplayName = paramProduct.getValueAsText();
            targetProduct = productsList.getByDisplayName(selectedProductDisplayName);
            setParameterProperties();
        }
    }

    private void setParameterProperties() {
        ParamProperties props = paramExpression.getProperties();
        props.setPropertyValue(ParamProperties.COMP_PRODUCTS_FOR_BAND_ARITHMETHIK_KEY, getCompatibleProducts());
        props.setPropertyValue(ParamProperties.SEL_PRODUCT_FOR_BAND_ARITHMETHIK_KEY, targetProduct);
        
        props = paramNewBandName.getProperties();
        props.setPropertyValue(ProductNodeNameValidator.PRODUCT_PROPERTY_KEY, targetProduct);
    }

    private String getBandName() {
        String bandName = paramNewBandName.getValueAsText();
        return bandName.trim();
    }
    
    private Product[] getCompatibleProducts() {
        List<Product> compatibleProducts = new ArrayList<Product>(productsList.size());
        compatibleProducts.add(targetProduct);
        final float geolocationEps = getGeolocationEps();
        Debug.trace("BandArithmetikDialog.geolocationEps = " + geolocationEps);
        Debug.trace("BandArithmetikDialog.getCompatibleProducts:");
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

    private ParamChangeListener createParamChangeListener() {
        return new ParamChangeListener() {

            public void parameterValueChanged(ParamChangeEvent event) {
                updateUIState(event.getParameter().getName());
            }
        };
    }

    private ActionListener createEditExpressionButtonListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Product[] compatibleProducts = getCompatibleProducts();
                ProductExpressionPane pep = ProductExpressionPane.createGeneralExpressionPane(compatibleProducts,
                                                                                              targetProduct,
                                                                                              visatApp.getPreferences());
                pep.setCode(paramExpression.getValueAsText());
                int status = pep.showModalDialog(getJDialog(), "Arithmetic Expression Editor");
                if (status == ModalDialog.ID_OK) {
                    String expression = pep.getCode();
                    paramExpression.setValue(expression, null);
                    final boolean writeData = (Boolean) paramWriteData.getValue();
                    if (!writeData && compatibleProducts.length > 1) {
                        int defaultIndex = Arrays.asList(compatibleProducts).indexOf(targetProduct);
                        RasterDataNode[] rasters = null;
                        try {
                            rasters = BandArithmetic.getRefRasters(expression, compatibleProducts, defaultIndex);
                        } catch (ParseException e1) {
                        }
                        if (rasters != null && rasters.length > 0) {
                            Set<Product> externalProducts = new HashSet<Product>(compatibleProducts.length);
                            for (RasterDataNode rdn: rasters) {
                                Product product = rdn.getProduct();
                                if (product != targetProduct) {
                                    externalProducts.add(product);
                                }
                            }
                            if (externalProducts.size() > 0) {
                                showForeignProductWarning();
                            }
                        }
                    }
                }
                pep.dispose();
            }
        };
    }
    
    private void showForeignProductWarning() {
        visatApp.showWarningDialog("Your expressions references multiple products.\n"
                + "It will only usable as long as the the referenced products are available.\n"
                + "Think about enabling 'Write data to disk' to  preserve the data.");
    }

    private boolean isValidExpression() {
        final Product[] products = getCompatibleProducts();
        if (products == null || products.length == 0) {
            return false;
        }

        final String expression = paramExpression.getValueAsText();
        if (expression == null || expression.length() == 0) {
            return false;
        }

        final int defaultIndex = Arrays.asList(products).indexOf(visatApp.getSelectedProduct());
        final Namespace namespace = BandArithmetic.createDefaultNamespace(products,
                                                                          defaultIndex == -1 ? 0 : defaultIndex);
        final Parser parser = new ParserImpl(namespace, false);
        try {
            parser.parse(expression);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    private boolean isTargetBandReferencedInExpression() {
        final Product[] products = getCompatibleProducts();

        final String expression = paramExpression.getValueAsText();
        if (expression.trim().length() == 0) {
            return false;
        }

        final int defaultIndex = Arrays.asList(products).indexOf(visatApp.getSelectedProduct());
        final Namespace namespace = BandArithmetic.createDefaultNamespace(products,
                                                                          defaultIndex == -1 ? 0 : defaultIndex);
        final Parser parser = new ParserImpl(namespace, false);
        try {
            final Term term = parser.parse(expression);
            final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
            final String targetBandName = getBandName();
            if (targetProduct.containsRasterDataNode(targetBandName)) {
                for (final RasterDataSymbol refRasterDataSymbol : refRasterDataSymbols) {
                    final String refRasterName = refRasterDataSymbol.getRaster().getName();
                    if (targetBandName.equalsIgnoreCase(refRasterName)) {
                        return true;
                    }
                }
            }
        } catch (ParseException e) {
            return false;
        }
        return false;
    }
}
