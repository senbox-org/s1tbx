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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import com.bc.jexp.*;
import com.bc.jexp.impl.ParserImpl;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.*;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class BandArithmetikDialog extends ModalDialog {

    private static final String _PARAM_NAME_USE_NEW_PRODUCT = "useNewProduct";
    private static final String _PARAM_NAME_CREATE_VIRTUAL_BAND = "createVirtualBand";
    private static final String _PARAM_NAME_USE_NEW_BAND = "useNewBand";
    private static final String _PARAM_NAME_PRODUCT = "product";
    private static final String _PARAM_NAME_BAND = "targetBand";

    private final VisatApp _visatApp;
    private Parameter _paramUseNewBand;
    private Parameter _paramUseNewProduct;
    private Parameter _paramProduct;
    private Parameter _paramBand;
    private Parameter _paramExpression;
    private Parameter _paramNoDataValue;
    private Product _targetProduct;
    private Band _targetBand;
    private ProductNodeList<Product> _productsList;
    private JButton _newProductButton;
    private JButton _newBandButton;
    private String _oldSelectedProduct;
    private String _oldSelectedBand;
    private JButton _editExpressionButton;
    private Parameter _paramNoDataValueUsed;
    private Parameter _paramWarnOnErrors;
    private Parameter _paramCreateVirtualBand;

    public BandArithmetikDialog(final VisatApp visatApp, Product currentProduct, ProductNodeList<Product> productsList,
                                String helpId) {
        super(visatApp.getMainFrame(), "Band Arithmetic", ModalDialog.ID_OK_CANCEL_HELP, helpId); /* I18N */
        Guardian.assertNotNull("currentProduct", currentProduct);
        Guardian.assertNotNull("productsList", productsList);
        Guardian.assertGreaterThan("productsList must be not empty", productsList.size(), 0);
        _visatApp = visatApp;
        _targetProduct = currentProduct;
        _productsList = productsList;
        initParameter();
        createUI();
    }

    public Product getTargetProduct() {
        return _targetProduct;
    }

    public ProductNodeList getProductsList() {
        return _productsList;
    }

    public void setTargetProduct(Product targetProduct, ProductNodeList<Product> productsList) {
        Guardian.assertNotNull("targetProduct", targetProduct);
        Guardian.assertNotNull("productsList", productsList);
        Guardian.assertGreaterThan("productsList must be not empty", productsList.size(), 0);
        _targetProduct = targetProduct;
        _productsList = productsList;

        _paramProduct.getProperties().setValueSetBound(false);
        _paramProduct.setValueSet(_productsList.getDisplayNames());
        if ((Boolean) _paramUseNewProduct.getValue()) {
            _paramProduct.setValue("", null);
        } else {
            _paramProduct.setValue(_targetProduct.getDisplayName(), null);
            _paramProduct.getProperties().setValueSetBound(true);
        }

        _paramBand.setValueSet(getTargetBandNames());
        if (isUseNewBand()) {
            _targetBand = null;
            _paramBand.setValue("", null);
        }
        updateUIState(null);
    }

    @Override
    public int show() {
        updateUIState(null);
        return super.show();
    }

    public int showQuestionDialog(String title, String message) {
        return showQuestionDialog(title, message, false);
    }

    public int showQuestionDialog(String title, String message, boolean allowCancel) {
        return JOptionPane.showConfirmDialog(getJDialog(),
                                             message,
                                             title,
                                             allowCancel
                                                     ? JOptionPane.YES_NO_CANCEL_OPTION
                                                     : JOptionPane.YES_NO_OPTION,
                                             JOptionPane.QUESTION_MESSAGE);
    }

    @Override
    protected void onOK() {
        final boolean checkInvalids = (Boolean) _paramWarnOnErrors.getValue();
        final boolean noDataValueUsed = (Boolean) _paramNoDataValueUsed.getValue();
        final float noDataValue = (Float) _paramNoDataValue.getValue();

        _targetBand.setImageInfo(null);
        _targetBand.setGeophysicalNoDataValue(noDataValue);
        _targetBand.setNoDataValueUsed(noDataValueUsed);
        if (!_targetProduct.containsBand(_targetBand.getName())) {
            _targetProduct.addBand(_targetBand);
        }
        if (getCreateVirtualBand()) {
            final VirtualBand virtualBand = (VirtualBand) _targetBand;
            virtualBand.setExpression(_paramExpression.getValueAsText());
            virtualBand.setCheckInvalids(checkInvalids);
        }

        try {
            _targetBand.createCompatibleRasterData();
        } catch (OutOfMemoryError e) {
            _visatApp.showOutOfMemoryErrorDialog("The new band could not be created."); /*I18N*/
            BandArithmetikDialog.super.onOK();
        }

        final String expression = _paramExpression.getValueAsText();
        _targetBand.setSynthetic(true);

        SwingWorker swingWorker = new SwingWorker() {
            final ProgressMonitor pm = new DialogProgressMonitor(getJDialog(), "Band Arithmetic",
                                                                 Dialog.ModalityType.APPLICATION_MODAL);

            String _errorMessage;
            int _numInvalids;

            @Override
            protected Object doInBackground() throws Exception {
                _errorMessage = null;
                try {
                    _numInvalids = _targetBand.computeBand(expression,
                                                           getCompatibleProducts(),
                                                           checkInvalids,
                                                           noDataValueUsed,
                                                           noDataValue,
                                                           pm);
                    _targetBand.fireProductNodeDataChanged();
                } catch (IOException e) {
                    Debug.trace(e);
                    _errorMessage = "The band could not be created.\nAn I/O error occurred:\n" + e.getMessage();  /*I18N*/
                } catch (ParseException e) {
                    Debug.trace(e);
                    _errorMessage = "The band could not be created.\nAn expression parse error occurred:\n" + e.getMessage(); /*I18N*/
                } catch (EvalException e) {
                    Debug.trace(e);
                    _errorMessage = "The band could not be created.\nAn expression evaluation error occured:\n" + e.getMessage();/*I18N*/
                } finally {
                }
                return null;

            }

            @Override
            public void done() {
                boolean ok = true;
                if (_errorMessage != null) {
                    showErrorDialog(_errorMessage);
                    ok = false;
                } else if (pm.isCanceled()) {
                    showErrorDialog("Band arithmetic has been canceled.");/*I18N*/
                    ok = false;
                } else if (_numInvalids > 0 && checkInvalids) {
                    int numPixelsTotal = _targetBand.getRasterWidth() * _targetBand.getRasterHeight();
                    int percentage = MathUtils.floorInt(100.0 * _numInvalids / numPixelsTotal);
                    if (percentage == 0) {
                        percentage = 1;
                    }
                    String message = _numInvalids + " of " + numPixelsTotal + " pixels (< " + percentage + "%) are invalid due to arithmetic exceptions.\n"; /*I18N*/
                    if (noDataValueUsed) {
                        message += "These pixels have been set to " + noDataValue + ".\n\n";  /*I18N*/
                    } else {
                        message += "These pixels have been set to NaN (IEEE 754).\n\n";  /*I18N*/
                    }
                    message += "Do you still want to use the suspicious computed data?\n"      /*I18N*/
                            + "If you select \"No\", all computed data will be lost.";  /*I18N*/
                    int status = showQuestionDialog("Invalid Pixel Warning", message);
                    if (status != JOptionPane.YES_OPTION) {
                        ok = false;
                    }
                }

                finalOnOk(ok);
            }
        };

        swingWorker.execute();
    }

    private void finalOnOk(boolean ok) {
        if (ok) {
            BandArithmetikDialog.super.onOK();
            _targetBand.setModified(true);
            if (_visatApp.getPreferences().getPropertyBool(VisatApp.PROPERTY_KEY_AUTO_SHOW_NEW_BANDS, true)
                    && !_visatApp.hasRasterProductSceneView(_targetBand)) {
                _visatApp.openProductSceneView(_targetBand, null);
            } else {
                _visatApp.updateImages(new Band[]{_targetBand});
            }
        } else {
            if (isUseNewBand()) {
                _targetProduct.removeBand(_targetProduct.getBand(_targetBand.getName()));
            }
        }
    }

    @Override
    protected void onCancel() {
        super.onCancel();
        _targetProduct = null;
    }

    @Override
    protected boolean verifyUserInput() {
        if (_targetProduct == null) {
            showErrorDialog("Please either select or create a target product."); /*I18N*/
            return false;
        }

        if (_targetBand == null) {
            showErrorDialog("Please either select or create a target band.");  /*I18N*/
            return false;
        }

        if (!isValidExpression()) {
            showErrorDialog("Please check the expression you have entered.\nIt is not valid."); /*I18N*/
            return false;
        }

        if (isTargetBandReferencedInExpression()) {
            showErrorDialog("You cannot reference the target band '" + _targetBand.getName() +
                    "' within the expression.");
            return false;
        }

        if (!isUseNewBand()) {
            final String bandName = _targetBand.getName();
            final StringBuffer message = new StringBuffer();
            if (!ProductData.isFloatingPointType(_targetBand.getDataType())) {
                message.append("The band '" + bandName + "' has an integer data type.\n"); /*I18N*/
            } else if (_targetBand.isScalingApplied()) {
                message.append("The band '" + bandName + "' uses an internal scaling.\n"); /*I18N*/
            }
            message.append("Overwriting this band's raster data may result in a loss of accuracy or\n" +
                    "even a value range overflow!\n\n" +
                    "Do you really want to overwrite the existing target band?"); /*I18N*/

            final int status = JOptionPane.showConfirmDialog(_paramUseNewBand.getEditor().getComponent(),
                                                             message.toString(),
                                                             "Really Overwrite", /*I18N*/
                                                             JOptionPane.YES_NO_OPTION);
            if (status != JOptionPane.YES_OPTION) {
                return false;
            }
        }

        return super.verifyUserInput();
    }

    private boolean isUseNewBand() {
        return (Boolean) _paramUseNewBand.getValue();
    }

    private void initParameter() {
        final ParamChangeListener paramChangeListener = createParamChangeListener();

        _paramUseNewProduct = new Parameter(_PARAM_NAME_USE_NEW_PRODUCT, false);
        _paramUseNewProduct.getProperties().setLabel("Use new Product"); /*I18N*/
        _paramUseNewProduct.addParamChangeListener(paramChangeListener);

        _paramProduct = new Parameter(_PARAM_NAME_PRODUCT, _targetProduct.getDisplayName());
        _paramProduct.setValueSet(_productsList.getDisplayNames());
        _paramProduct.getProperties().setValueSetBound(true);
        _paramProduct.getProperties().setLabel("Target Product"); /*I18N*/
        _paramProduct.addParamChangeListener(paramChangeListener);

        _paramCreateVirtualBand = new Parameter(_PARAM_NAME_CREATE_VIRTUAL_BAND, false);
        _paramCreateVirtualBand.getProperties().setLabel("Create virtual Band"); /*I18N*/
        _paramCreateVirtualBand.addParamChangeListener(paramChangeListener);

        _paramUseNewBand = new Parameter(_PARAM_NAME_USE_NEW_BAND, true);
        _paramUseNewBand.getProperties().setLabel("Use new Band"); /*I18N*/
        _paramUseNewBand.addParamChangeListener(paramChangeListener);

        _paramBand = new Parameter(_PARAM_NAME_BAND, "");
        _paramBand.setValueSet(getTargetBandNames());
        _paramBand.getProperties().setValueSetBound(false);
        _paramBand.getProperties().setLabel("Target Band"); /*I18N*/
        _paramBand.addParamChangeListener(paramChangeListener);

        _paramExpression = new Parameter("arithmetikExpr", "");
        _paramExpression.getProperties().setLabel("Expression"); /*I18N*/
        _paramExpression.getProperties().setDescription("Arithmetic expression"); /*I18N*/
        _paramExpression.getProperties().setNumRows(3);
//        _paramExpression.getProperties().setEditorClass(ArithmetikExpressionEditor.class);
//        _paramExpression.getProperties().setValidatorClass(BandArithmeticExprValidator.class);

        _paramWarnOnErrors = new Parameter("warnOnArithmErrorParam", true);
        _paramWarnOnErrors.getProperties().setLabel("Warn, if any arithmetic exception is detected"); /*I18N*/
        _paramWarnOnErrors.addParamChangeListener(paramChangeListener);

        _paramNoDataValueUsed = new Parameter("noDataValueUsedParam", true);
        _paramNoDataValueUsed.getProperties().setLabel("No-data value to be used on arithmetic exceptions: "); /*I18N*/
        _paramNoDataValueUsed.addParamChangeListener(paramChangeListener);

        _paramNoDataValue = new Parameter("noDataValueParam", 0.0F);
        _paramNoDataValue.setUIEnabled(false);

        setArithmetikValues();
    }

    private void createUI() {
        _newProductButton = new JButton("New...");
        _newProductButton.setName("newProductButton");
        _newProductButton.addActionListener(createNewProductButtonListener());

        _newBandButton = new JButton("New...");
        _newBandButton.setName("newBandButton");
        _newBandButton.addActionListener(createNewBandButtonListener());

        _editExpressionButton = new JButton("Edit Expression...");
        _editExpressionButton.setName("editExpressionButton");
        _editExpressionButton.addActionListener(createEditExpressionButtonListener());

        final JPanel panel = GridBagUtils.createPanel();
        int line = 0;
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, _paramProduct.getEditor().getLabelComponent(), gbc,
                                "gridwidth=3, fill=BOTH, weightx=1");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, _paramProduct.getEditor().getComponent(), gbc,
                                "insets.top=3, gridwidth=3, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, _paramUseNewProduct.getEditor().getComponent(), gbc,
                                "fill=NONE, anchor=EAST, gridwidth=2");
        GridBagUtils.addToPanel(panel, _newProductButton, gbc, "weightx=0, gridwidth=1");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, _paramBand.getEditor().getLabelComponent(), gbc,
                                "insets.top=4, gridwidth=3, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, _paramBand.getEditor().getComponent(), gbc,
                                "insets.top=3, gridwidth=3, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, _paramCreateVirtualBand.getEditor().getComponent(), gbc,
                                "weightx=1, fill=NONE, gridwidth=1, anchor=EAST");
        GridBagUtils.addToPanel(panel, _paramUseNewBand.getEditor().getComponent(), gbc, "weightx=0");
        GridBagUtils.addToPanel(panel, _newBandButton, gbc);

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, _paramExpression.getEditor().getLabelComponent(), gbc,
                                "insets.top=4, gridwidth=3, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, _paramExpression.getEditor().getComponent(), gbc,
                                "weighty=1, insets.top=3, gridwidth=3, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, _editExpressionButton, gbc,
                                "weighty=0, insets.top=3, gridwidth=3, fill=NONE, anchor=EAST");

        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBorder(UIUtils.createGroupBorder("Exception Handling"));
        jPanel.add(_paramWarnOnErrors.getEditor().getComponent(), BorderLayout.NORTH);
        jPanel.add(_paramNoDataValueUsed.getEditor().getComponent(), BorderLayout.WEST);
        jPanel.add(_paramNoDataValue.getEditor().getComponent());

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, jPanel, gbc, "insets.top=10, gridwidth=3, fill=BOTH, anchor=WEST");

        setContent(panel);
    }

    private void updateUIState(String parameterName) {
        boolean useNewProduct = (Boolean) _paramUseNewProduct.getValue();
        final String productDisplayName = _paramProduct.getValueAsText();
        boolean productIsSelected = productDisplayName != null && productDisplayName.length() > 0;
        String[] bandValueSet = _paramBand.getProperties().getValueSet();
        boolean paramBandHasValidValueSet = bandValueSet != null && bandValueSet.length > 0;
        boolean createVirtualBand = getCreateVirtualBand();
        _newProductButton.setEnabled(!createVirtualBand && useNewProduct);
        _paramUseNewProduct.setUIEnabled(!createVirtualBand);
        _paramProduct.setUIEnabled(!useNewProduct);
        _paramUseNewBand.setUIEnabled(!createVirtualBand && productIsSelected && paramBandHasValidValueSet);
        _paramBand.setUIEnabled(!isUseNewBand() && productIsSelected);
        _newBandButton.setEnabled(isUseNewBand() && productIsSelected);
        _paramNoDataValue.setUIEnabled((Boolean) _paramNoDataValueUsed.getValue());
        if (parameterName == null) {
            return;
        }

        if (parameterName.equals(_PARAM_NAME_USE_NEW_PRODUCT)) {
            if (useNewProduct) {
                _oldSelectedProduct = _paramProduct.getValueAsText();
                _targetProduct = null;
            }
            _paramProduct.getProperties().setValueSetBound(!useNewProduct);
            _paramProduct.setValue(useNewProduct ? "" : _oldSelectedProduct, null);
        } else if (parameterName.equals(_PARAM_NAME_USE_NEW_BAND)) {
            if (isUseNewBand()) {
                _oldSelectedBand = _paramBand.getValueAsText();
                _targetBand = null;
            } else {
                if (_oldSelectedBand == null || _oldSelectedBand.trim().length() == 0 || !_targetProduct.containsBand(
                        _oldSelectedBand)) {
                    _oldSelectedBand = _paramBand.getProperties().getValueSet()[0];
                }
            }
            _paramBand.getProperties().setValueSetBound(!isUseNewBand());
            _paramBand.setValue(isUseNewBand() ? "" : _oldSelectedBand, null);
        } else if (parameterName.equals(_PARAM_NAME_PRODUCT)) {
            _oldSelectedBand = null;
            _paramUseNewBand.setValue(true, null);
            String selectedProdcutDisplayName = _paramProduct.getValueAsText();
            Product product = _productsList.getByDisplayName(selectedProdcutDisplayName);
            if (product != null) {
                if (useNewProduct) {
                    showErrorDialog("A product with the name '" + selectedProdcutDisplayName + "' already exists.\n"
                            + "Please choose a another one."); /*I18N*/
                    _paramProduct.setValue("", null);
                    _targetProduct = null;
                } else {
                    _targetProduct = product;
                }
            }
            boolean b = productIsSelected && _targetProduct != null;
            _paramExpression.setUIEnabled(b);
            _editExpressionButton.setEnabled(b);
            if (b) {
                _paramBand.setValueSet(getTargetBandNames());
                setArithmetikValues();
            }
        } else if (parameterName.equals(_PARAM_NAME_BAND)) {
            String selectedBandName = _paramBand.getValueAsText();
            Band band = null;
            if (_targetProduct != null) {
                if (selectedBandName != null && selectedBandName.length() > 0) {
                    band = _targetProduct.getBand(selectedBandName);
                }
            }
            if (band != null) {
                if (isUseNewBand()) {
                    showErrorDialog(
                            "A band with the name '" + selectedBandName + "' already exists in the selected product.\n"
                                    + "Please choose another one."); /*I18N*/
                    _targetBand = null;
                } else {
                    _targetBand = band;
                }
            }
        } else if (parameterName.equals(_PARAM_NAME_CREATE_VIRTUAL_BAND)) {
            if (createVirtualBand) {
                _paramUseNewBand.setValueAsText("true", null);
                _paramUseNewProduct.setValueAsText("false", null);
            }
            _targetBand = null;
            _paramBand.setValue("", null);
        }
    }

    private String[] getTargetBandNames() {
        final List<String> names = new ArrayList<String>();
        final Band[] bands = _targetProduct.getBands();
        for (final Band band : bands) {
            if (!(band instanceof VirtualBand)) {
                names.add(band.getName());
            }
        }
        return names.toArray(new String[names.size()]);
    }

    private void setArithmetikValues() {
        final ParamProperties props = _paramExpression.getProperties();
        props.setPropertyValue(ParamProperties.COMP_PRODUCTS_FOR_BAND_ARITHMETHIK_KEY
                , getCompatibleProducts());
        props.setPropertyValue(ParamProperties.SEL_PRODUCT_FOR_BAND_ARITHMETHIK_KEY
                , _targetProduct);
    }

    private Product[] getCompatibleProducts() {
        if (_targetProduct == null) {
            return null;
        }
        Vector<Product> compatibleProducts = new Vector<Product>();
        compatibleProducts.add(_targetProduct);
        if (!getCreateVirtualBand()) {
            final float geolocationEps = getGeolocationEps();
            Debug.trace("BandArithmetikDialog.geolocationEps = " + geolocationEps);
            Debug.trace("BandArithmetikDialog.getCompatibleProducts:");
            Debug.trace("  comparing: " + _targetProduct.getName());
            for (int i = 0; i < _productsList.size(); i++) {
                final Product product = _productsList.getAt(i);
                if (_targetProduct != product) {
                    Debug.trace("  with:      " + product.getDisplayName());
                    final boolean compatibleProduct = _targetProduct.isCompatibleProduct(product, geolocationEps);
                    Debug.trace("  result:    " + compatibleProduct);
                    if (compatibleProduct) {
                        compatibleProducts.add(product);
                    }
                }
            }
        }
        return compatibleProducts.toArray(new Product[compatibleProducts.size()]);
    }

    private ParamChangeListener createParamChangeListener() {
        return new ParamChangeListener() {

            public void parameterValueChanged(ParamChangeEvent event) {
                updateUIState(event.getParameter().getName());
            }
        };
    }

    private ActionListener createNewProductButtonListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                final Product product = _productsList.getByDisplayName(_oldSelectedProduct);
                final int selectedSourceIndex = _productsList.indexOf(product);

                final NewProductDialog dialog = new NewProductDialog(getJDialog(),
                                                                     _productsList,
                                                                     selectedSourceIndex,
                                                                     false,
                                                                     "arithm");

                if (dialog.show() == NewProductDialog.ID_OK) {
                    _targetProduct = dialog.getResultProduct();
                    _paramProduct.setValue(_targetProduct.getName(), null);
                }
            }
        };
    }

    private ActionListener createNewBandButtonListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                NewBandDialog dialog = new NewBandDialog(getJDialog(), _targetProduct);
                if (dialog.show() == NewBandDialog.ID_OK) {
                    int width = _targetProduct.getSceneRasterWidth();
                    int height = _targetProduct.getSceneRasterHeight();
                    if (getCreateVirtualBand()) {
                        _targetBand = new VirtualBand(dialog.getNewBandsName(), dialog.getNewBandsDataType(), width,
                                                      height, "0");
                    } else {
                        _targetBand = new Band(dialog.getNewBandsName(), dialog.getNewBandsDataType(), width, height);
                    }
                    _targetBand.setDescription(dialog.getNewBandsDesc());
                    _targetBand.setUnit(dialog.getNewBandsUnit());
                    _paramBand.setValue(_targetBand.getName(), null);
                }
            }
        };
    }

    private ActionListener createEditExpressionButtonListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ProductExpressionPane pep = ProductExpressionPane.createGeneralExpressionPane(getCompatibleProducts(),
                                                                                              _targetProduct,
                                                                                              _visatApp.getPreferences());
                pep.setCode(_paramExpression.getValueAsText());
                int status = pep.showModalDialog(getJDialog(), "Arithmetic Expression Editor");
                if (status == ModalDialog.ID_OK) {
                    _paramExpression.setValue(pep.getCode(), null);
                    Debug.trace("BandArithmetikDialog: expression is: " + pep.getCode());
                }
                pep.dispose();
                pep = null;
            }
        };
    }

    private boolean isValidExpression() {
        final Product[] products = getCompatibleProducts();
        if (products == null || products.length == 0) {
            return false;
        }

        final String expression = _paramExpression.getValueAsText();
        if (expression == null || expression.length() == 0) {
            return false;
        }

        final int defaultIndex = Arrays.asList(products).indexOf(_visatApp.getSelectedProduct());
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
        if (products == null || products.length == 0) {
            return false;
        }

        final String expression = _paramExpression.getValueAsText();
        if (expression == null || expression.trim().length() == 0) {
            return false;
        }

        final int defaultIndex = Arrays.asList(products).indexOf(_visatApp.getSelectedProduct());
        final Namespace namespace = BandArithmetic.createDefaultNamespace(products,
                                                                          defaultIndex == -1 ? 0 : defaultIndex);
        final Parser parser = new ParserImpl(namespace, false);
        try {
            final Term term = parser.parse(expression);
            final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
            final String targetBandName = _paramBand.getValueAsText();
            if (_targetProduct != null && _targetProduct.containsRasterDataNode(targetBandName)) {
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

    private boolean getCreateVirtualBand() {
        return (Boolean) _paramCreateVirtualBand.getValue();
    }

    private float getGeolocationEps() {
        return (float) _visatApp.getPreferences().getPropertyDouble(VisatApp.PROPERTY_KEY_GEOLOCATION_EPS,
                                                                    VisatApp.PROPERTY_DEFAULT_GEOLOCATION_EPS);
    }
}
