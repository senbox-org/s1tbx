package org.esa.beam.gpf.operators.standard;

import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.*;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.framework.param.*;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.util.Debug;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.gpf.operators.standard.BandMathsOp;

import javax.swing.*;

import java.util.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import com.bc.jexp.Namespace;
import com.bc.jexp.Parser;
import com.bc.jexp.ParseException;
import com.bc.jexp.impl.ParserImpl;

/**
User interface for BandMaths Operator
 */
public class BandMathsOpUI extends BaseOperatorUI {

    private static final String _PARAM_NAME_BAND = "targetBand";

    private Parameter paramBand = null;
    private Parameter paramBandUnit = null;
    private Parameter paramNoDataValue = null;
    private Parameter paramExpression = null;
    private Product targetProduct = null;
    private Band targetBand = null;
    private ProductNodeList<Product> productsList = null;
    private JButton editExpressionButton = null;
    private JComponent panel = null;
    private String errorText = "";

    private BandMathsOp.BandDescriptor bandDesc = new BandMathsOp.BandDescriptor();

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        initVariables();
        panel = createUI();
        initParameters();

        return panel;
    }

    @Override
    public void initParameters() {

        Object[] bandDescriptors = (Object[])paramMap.get("targetBands");
        if(bandDescriptors == null)
            bandDescriptors = (Object[])paramMap.get("targetBandDescriptors");
        if(bandDescriptors !=null && bandDescriptors.length > 0) {
            bandDesc = (BandMathsOp.BandDescriptor)bandDescriptors[0];
            bandDesc.type = ProductData.TYPESTRING_FLOAT32;

            try {
                paramBand.setValueAsText(bandDesc.name);
                paramBandUnit.setValueAsText(bandDesc.unit);
                paramNoDataValue.setValueAsText(bandDesc.noDataValue);
                paramExpression.setValueAsText(bandDesc.expression);
            } catch(Exception e) {
                //
            }
        }
        if(sourceProducts != null && sourceProducts.length > 0) {
            targetProduct = sourceProducts[0];

            targetBand = new Band(bandDesc.name, ProductData.TYPE_FLOAT32,
                    targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight());
            targetBand.setDescription("");
            //targetBand.setUnit(dialog.getNewBandsUnit());

            productsList = new ProductNodeList<Product>();
            for (Product prod : sourceProducts) {
                productsList.add(prod);
            }
        } else {
            targetProduct = null;
            targetBand = null;
        }
        updateUIState(paramBand.getName());
    }

    @Override
    public UIValidation validateParameters() {
        if(!(targetProduct == null || isValidExpression()))
            return new UIValidation(UIValidation.State.ERROR, "Expression is invalid. "+ errorText);
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        bandDesc.name = paramBand.getValueAsText();
        bandDesc.unit = paramBandUnit.getValueAsText();
        bandDesc.noDataValue = paramNoDataValue.getValueAsText();
        bandDesc.expression = paramExpression.getValueAsText();

        final BandMathsOp.BandDescriptor[] bandDescriptors = new BandMathsOp.BandDescriptor[1];
        bandDescriptors[0] = bandDesc;
        paramMap.put("targetBandDescriptors", bandDescriptors);
        paramMap.put("bandName", bandDesc.name);
        paramMap.put("bandUnit", bandDesc.unit);
        paramMap.put("bandNodataValue", bandDesc.noDataValue);
        paramMap.put("bandExpression", bandDesc.expression);
    }

    private void initVariables() {
        final ParamChangeListener paramChangeListener = createParamChangeListener();

        String bandName = (String)paramMap.get("bandName");
        if(bandName == null)
            bandName = "new_band";
        String bandUnit = (String)paramMap.get("bandUnit");
        if(bandUnit == null)
            bandUnit = "";
        String bandNodataValue = (String)paramMap.get("bandNodataValue");
        if(bandNodataValue == null)
            bandNodataValue = "";
        String expression = (String)paramMap.get("bandExpression");
        if(expression == null)
            expression = " ";

        paramBand = new Parameter(_PARAM_NAME_BAND, bandName);
        paramBand.getProperties().setValueSetBound(false);
        paramBand.getProperties().setLabel("Target Band"); /*I18N*/
        paramBand.addParamChangeListener(paramChangeListener);

        paramBandUnit = new Parameter("bandUnit", bandUnit);
        paramBandUnit.getProperties().setValueSetBound(false);
        paramBandUnit.getProperties().setLabel("Band Unit"); /*I18N*/
        paramBandUnit.addParamChangeListener(paramChangeListener);

        paramNoDataValue = new Parameter("bandNodataValue", bandNodataValue);
        paramNoDataValue.getProperties().setValueSetBound(false);
        paramNoDataValue.getProperties().setLabel("No-Data Value"); /*I18N*/
        paramNoDataValue.addParamChangeListener(paramChangeListener);

        paramExpression = new Parameter("arithmetikExpr", expression);
        paramExpression.getProperties().setLabel("Expression"); /*I18N*/
        paramExpression.getProperties().setDescription("Arithmetic expression"); /*I18N*/
        paramExpression.getProperties().setNumRows(5);
//        paramExpression.getProperties().setEditorClass(ArithmetikExpressionEditor.class);
//        paramExpression.getProperties().setValidatorClass(BandArithmeticExprValidator.class);

        setArithmetikValues();
    }

    private JComponent createUI() {

        editExpressionButton = new JButton("Edit Expression...");
        editExpressionButton.setName("editExpressionButton");
        editExpressionButton.addActionListener(createEditExpressionButtonListener());

        final JLabel infoLabel = new JLabel("Variables $Band0, $Band1, etc can be used in place of Band names for batch processing");

        final JPanel gridPanel = GridBagUtils.createPanel();
        int line = 0;
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(gridPanel, paramBand.getEditor().getLabelComponent(), gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(gridPanel, paramBand.getEditor().getComponent(), gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(gridPanel, paramBandUnit.getEditor().getLabelComponent(), gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(gridPanel, paramBandUnit.getEditor().getComponent(), gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(gridPanel, paramNoDataValue.getEditor().getLabelComponent(), gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=WEST");
        GridBagUtils.addToPanel(gridPanel, paramNoDataValue.getEditor().getComponent(), gbc,
                                "weightx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(gridPanel, paramExpression.getEditor().getLabelComponent(), gbc,
                                "weightx=0, insets.top=3, gridwidth=1, fill=HORIZONTAL, anchor=NORTHWEST");
        GridBagUtils.addToPanel(gridPanel, paramExpression.getEditor().getComponent(), gbc,
                                "weightx=1, weighty=1, insets.top=3, gridwidth=2, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(gridPanel, editExpressionButton, gbc,
                                "weighty=0, insets.top=3, gridwidth=3, fill=NONE, anchor=EAST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(gridPanel, infoLabel, gbc,
                                "gridx=1, insets.top=3, gridwidth=2, fill=HORIZONTAL, anchor=WEST");

        return gridPanel;
    }

    private void setArithmetikValues() {
        final ParamProperties props = paramExpression.getProperties();
        props.setPropertyValue(ParamProperties.COMP_PRODUCTS_FOR_BAND_ARITHMETHIK_KEY, getCompatibleProducts());
        props.setPropertyValue(ParamProperties.SEL_PRODUCT_FOR_BAND_ARITHMETHIK_KEY, targetProduct);
    }

     private ParamChangeListener createParamChangeListener() {
        return new ParamChangeListener() {

            public void parameterValueChanged(ParamChangeEvent event) {
                updateUIState(event.getParameter().getName());
            }
        };
    }

    private Product[] getCompatibleProducts() {
        if (targetProduct == null) {
            return null;
        }
        final Vector<Product> compatibleProducts = new Vector<Product>();
        compatibleProducts.add(targetProduct);
            final float geolocationEps = 180;
            Debug.trace("BandArithmetikDialog.geolocationEps = " + geolocationEps);
            Debug.trace("BandArithmetikDialog.getCompatibleProducts:");
            Debug.trace("  comparing: " + targetProduct.getName());
            for (int i = 0; i < productsList.size(); i++) {
                final Product product = productsList.getAt(i);
                if (targetProduct != product) {
                    Debug.trace("  with:      " + product.getDisplayName());
                    final boolean compatibleProduct = targetProduct.isCompatibleProduct(product, geolocationEps);
                    Debug.trace("  result:    " + compatibleProduct);
                    if (compatibleProduct) {
                        compatibleProducts.add(product);
                    }
                }
            }
        return compatibleProducts.toArray(new Product[compatibleProducts.size()]);
    }

    private ActionListener createEditExpressionButtonListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ProductExpressionPane pep = ProductExpressionPane.createGeneralExpressionPane(getCompatibleProducts(),
                        targetProduct, new PropertyMap());
                pep.setCode(paramExpression.getValueAsText());
                int status = pep.showModalDialog(SwingUtilities.getWindowAncestor(panel), "Arithmetic Expression Editor");
                if (status == ModalDialog.ID_OK) {
                    paramExpression.setValue(pep.getCode(), null);
                    Debug.trace("BandArithmetikDialog: expression is: " + pep.getCode());

                    bandDesc.expression = paramExpression.getValueAsText();
                }
                pep.dispose();
                pep = null;
            }
        };
    }

    private boolean isValidExpression() {
        errorText = "";
        final Product[] products = getCompatibleProducts();
        if (products == null || products.length == 0) {
            return false;
        }

        String expression = paramExpression.getValueAsText();
        if (expression == null || expression.length() == 0) {
            return false;
        }

        if(sourceProducts != null) {
            expression = BandMathsOp.SubstitutePlaceHolders(sourceProducts, expression);
        }

        final int defaultIndex = 0;//Arrays.asList(products).indexOf(_visatApp.getSelectedProduct());
        final Namespace namespace = BandArithmetic.createDefaultNamespace(products,
                                                                          defaultIndex == -1 ? 0 : defaultIndex);
        final Parser parser = new ParserImpl(namespace, false);
        try {
            parser.parse(expression);
        } catch (ParseException e) {
            errorText = e.getMessage();
            return false;
        }
        return true;
    }

    private void updateUIState(String parameterName) {

        if (parameterName == null) {
            return;
        }

        if (parameterName.equals(_PARAM_NAME_BAND)) {
            final boolean b = targetProduct != null;
            paramExpression.setUIEnabled(b);
            editExpressionButton.setEnabled(b);
            paramBand.setUIEnabled(b);
            paramBandUnit.setUIEnabled(b);
            paramNoDataValue.setUIEnabled(b);
            if (b) {
                setArithmetikValues();
            }

            final String selectedBandName = paramBand.getValueAsText();
            if (b) {
                if (selectedBandName != null && selectedBandName.length() > 0) {
                    targetBand = targetProduct.getBand(selectedBandName);
                }
            }
        }
    }

}