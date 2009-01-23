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
import com.bc.jexp.EvalException;
import com.bc.jexp.Namespace;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import com.bc.jexp.impl.ParserImpl;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeList;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.NewBandDialog;
import org.esa.beam.framework.ui.NewProductDialog;
import org.esa.beam.framework.ui.UIUtils;
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

    private final VisatApp visatApp;
    private Parameter paramUseNewBand;
    private Parameter paramUseNewProduct;
    private Parameter paramProduct;
    private Parameter paramBand;
    private Parameter paramExpression;
    private Parameter paramNoDataValue;
    private Product targetProduct;
    private Band targetBand;
    private ProductNodeList<Product> productsList;
    private JButton newProductButton;
    private JButton newBandButton;
    private String oldSelectedProduct;
    private String oldSelectedBand;
    private JButton editExpressionButton;
    private Parameter paramNoDataValueUsed;
    private Parameter paramWarnOnErrors;
    private Parameter paramCreateVirtualBand;

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
    }

    public Product getTargetProduct() {
        return targetProduct;
    }

    public ProductNodeList getProductsList() {
        return productsList;
    }

    public void setTargetProduct(Product targetProduct, ProductNodeList<Product> productsList) {
        Guardian.assertNotNull("targetProduct", targetProduct);
        Guardian.assertNotNull("productsList", productsList);
        Guardian.assertGreaterThan("productsList must be not empty", productsList.size(), 0);
        this.targetProduct = targetProduct;
        this.productsList = productsList;

        paramProduct.getProperties().setValueSetBound(false);
        paramProduct.setValueSet(this.productsList.getDisplayNames());
        if ((Boolean) paramUseNewProduct.getValue()) {
            paramProduct.setValue("", null);
        } else {
            paramProduct.setValue(this.targetProduct.getDisplayName(), null);
            paramProduct.getProperties().setValueSetBound(true);
        }

        paramBand.setValueSet(getTargetBandNames());
        if (isUseNewBand()) {
            targetBand = null;
            paramBand.setValue("", null);
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
        final boolean checkInvalids = (Boolean) paramWarnOnErrors.getValue();
        final boolean noDataValueUsed = (Boolean) paramNoDataValueUsed.getValue();
        final float noDataValue = (Float) paramNoDataValue.getValue();
        final String expression = paramExpression.getValueAsText();

        targetBand.setImageInfo(null);
        targetBand.setGeophysicalNoDataValue(noDataValue);
        targetBand.setNoDataValueUsed(noDataValueUsed);
        if (getCreateVirtualBand()) {
            final String validMaskExpression;
            try {
                validMaskExpression = BandArithmetic.getValidMaskExpression(expression, new Product[]{targetProduct}, 0, null);
            } catch (ParseException e) {
                String errorMessage = "The band could not be created.\nAn expression parse error occurred:\n" + e.getMessage(); /*I18N*/
                visatApp.showErrorDialog(errorMessage);
                hide();
                return;
            }

            final VirtualBand virtualBand = (VirtualBand) targetBand;
            virtualBand.setExpression(expression);
            virtualBand.setValidPixelExpression(validMaskExpression);
            virtualBand.setCheckInvalids(checkInvalids);
        } else {
            if (!targetBand.hasRasterData()) {
                final int rasterSize = targetBand.getRasterWidth() * targetBand.getRasterHeight();
                final int type = targetBand.getDataType();
                final int requiredMemory = rasterSize * ProductData.getElemSize(type);
                final Runtime runtime = Runtime.getRuntime();
                final long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                final long availableMemory = runtime.maxMemory() - usedMemory;

                final float megabyte = 1024.0f * 1024.0f;
                if (requiredMemory > availableMemory) {
                    String message = "Can not create the new band.\n" +
                            "The amount of required memory is equal or greater than the available memory.\n\n" +
                            String.format("Available memory: %.1f MB\n", availableMemory / megabyte) +
                            String.format("Required memory: %.1f MB", requiredMemory / megabyte);
                    visatApp.showErrorDialog(message); /*I18N*/
                    hide();
                    return;
                }
                if (requiredMemory * 1.3 > availableMemory) {
                    String message = "Creating the new band will cause the system to reach its memory limit.\n" +
                            "This can cause the system to slow down.\n" +
                            String.format("Available memory: %.1f MB\n", availableMemory / megabyte) +
                            String.format("Required memory: %.1f MB\n\n", requiredMemory / megabyte) +
                            "Do you really want to create the image?";
                    final int answer = visatApp.showQuestionDialog(message, null);/*I18N*/
                    if (answer != JOptionPane.YES_OPTION) {
                        hide();
                        return;
                    }
                }
            }
        }
        if (!targetProduct.containsBand(targetBand.getName())) {
            targetProduct.addBand(targetBand);
        }

        targetBand.setSynthetic(true);

        SwingWorker swingWorker = new SwingWorker() {
            private final ProgressMonitor pm = new DialogProgressMonitor(getJDialog(), "Band Arithmetic",
                                                                         Dialog.ModalityType.APPLICATION_MODAL);

            private String errorMessage;
            private int numInvalids;

            @Override
            protected Object doInBackground() throws Exception {
                errorMessage = null;
                try {
                    if (!getCreateVirtualBand()) {
                        final Product[] products = getCompatibleProducts();
                        targetBand.setValidPixelExpression(BandArithmetic.getValidMaskExpression(expression, products, 0, null));
                        numInvalids = targetBand.computeBand(expression,
                                                             products,
                                                             checkInvalids,
                                                             noDataValueUsed,
                                                             noDataValue,
                                                             pm);
                        targetBand.fireProductNodeDataChanged();
                    }

                } catch (IOException e) {
                    Debug.trace(e);
                    errorMessage = "The band could not be created.\nAn I/O error occurred:\n" + e.getMessage();  /*I18N*/
                } catch (ParseException e) {
                    Debug.trace(e);
                    errorMessage = "The band could not be created.\nAn expression parse error occurred:\n" + e.getMessage(); /*I18N*/
                } catch (EvalException e) {
                    Debug.trace(e);
                    errorMessage = "The band could not be created.\nAn expression evaluation error occured:\n" + e.getMessage();/*I18N*/
                } catch (Exception e) {
                    Debug.trace(e);
                    errorMessage = "The band could not be created.:\n" + e.getMessage();/*I18N*/
                }
                return null;

            }

            @Override
            public void done() {
                boolean ok = true;
                if (errorMessage != null) {
                    visatApp.showErrorDialog(errorMessage);
                    ok = false;
                } else if (pm.isCanceled()) {
                    visatApp.showErrorDialog("Band arithmetic has been canceled.");/*I18N*/
                    ok = false;
                } else if (numInvalids > 0 && checkInvalids) {
                    int numPixelsTotal = targetBand.getRasterWidth() * targetBand.getRasterHeight();
                    int percentage = MathUtils.floorInt(100.0 * numInvalids / numPixelsTotal);
                    if (percentage == 0) {
                        percentage = 1;
                    }
                    String message = numInvalids + " of " + numPixelsTotal + " pixels (< " + percentage + "%) are invalid due to arithmetic exceptions.\n"; /*I18N*/
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
            targetBand.setModified(true);
            if (visatApp.getPreferences().getPropertyBool(VisatApp.PROPERTY_KEY_AUTO_SHOW_NEW_BANDS, true)
                    && !visatApp.hasRasterProductSceneView(targetBand)) {
                visatApp.openProductSceneView(targetBand);
            } else {
                visatApp.updateImages(new Band[]{targetBand});
            }
        } else {
            if (isUseNewBand()) {
                targetProduct.removeBand(targetProduct.getBand(targetBand.getName()));
            }
        }
    }

    @Override
    protected void onCancel() {
        super.onCancel();
        targetProduct = null;
    }

    @Override
    protected boolean verifyUserInput() {
        if (targetProduct == null) {
            showErrorDialog("Please either select or create a target product."); /*I18N*/
            return false;
        }

        if (targetBand == null) {
            showErrorDialog("Please either select or create a target band.");  /*I18N*/
            return false;
        }

        if (!isValidExpression()) {
            showErrorDialog("Please check the expression you have entered.\nIt is not valid."); /*I18N*/
            return false;
        }

        if (isTargetBandReferencedInExpression()) {
            showErrorDialog("You cannot reference the target band '" + targetBand.getName() +
                    "' within the expression.");
            return false;
        }

        if (!isUseNewBand()) {
            final String bandName = targetBand.getName();
            final StringBuffer message = new StringBuffer();
            if (!ProductData.isFloatingPointType(targetBand.getDataType())) {
                message.append("The band '" + bandName + "' has an integer data type.\n"); /*I18N*/
            } else if (targetBand.isScalingApplied()) {
                message.append("The band '" + bandName + "' uses an internal scaling.\n"); /*I18N*/
            }
            message.append("Overwriting this band's raster data may result in a loss of accuracy or\n" +
                    "even a value range overflow!\n\n" +
                    "Do you really want to overwrite the existing target band?"); /*I18N*/

            final int status = JOptionPane.showConfirmDialog(paramUseNewBand.getEditor().getComponent(),
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
        return (Boolean) paramUseNewBand.getValue();
    }

    private void initParameter() {
        final ParamChangeListener paramChangeListener = createParamChangeListener();

        paramUseNewProduct = new Parameter(_PARAM_NAME_USE_NEW_PRODUCT, false);
        paramUseNewProduct.getProperties().setLabel("Use new Product"); /*I18N*/
        paramUseNewProduct.addParamChangeListener(paramChangeListener);

        paramProduct = new Parameter(_PARAM_NAME_PRODUCT, targetProduct.getDisplayName());
        paramProduct.setValueSet(productsList.getDisplayNames());
        paramProduct.getProperties().setValueSetBound(true);
        paramProduct.getProperties().setLabel("Target Product"); /*I18N*/
        paramProduct.addParamChangeListener(paramChangeListener);

        paramCreateVirtualBand = new Parameter(_PARAM_NAME_CREATE_VIRTUAL_BAND, false);
        paramCreateVirtualBand.getProperties().setLabel("Create virtual Band"); /*I18N*/
        paramCreateVirtualBand.addParamChangeListener(paramChangeListener);

        paramUseNewBand = new Parameter(_PARAM_NAME_USE_NEW_BAND, true);
        paramUseNewBand.getProperties().setLabel("Use new Band"); /*I18N*/
        paramUseNewBand.addParamChangeListener(paramChangeListener);

        paramBand = new Parameter(_PARAM_NAME_BAND, "");
        paramBand.setValueSet(getTargetBandNames());
        paramBand.getProperties().setValueSetBound(false);
        paramBand.getProperties().setLabel("Target Band"); /*I18N*/
        paramBand.addParamChangeListener(paramChangeListener);

        paramExpression = new Parameter("arithmetikExpr", "");
        paramExpression.getProperties().setLabel("Expression"); /*I18N*/
        paramExpression.getProperties().setDescription("Arithmetic expression"); /*I18N*/
        paramExpression.getProperties().setNumRows(3);
//        paramExpression.getProperties().setEditorClass(ArithmetikExpressionEditor.class);
//        paramExpression.getProperties().setValidatorClass(BandArithmeticExprValidator.class);

        paramWarnOnErrors = new Parameter("warnOnArithmErrorParam", true);
        paramWarnOnErrors.getProperties().setLabel("Warn, if any arithmetic exception is detected"); /*I18N*/
        paramWarnOnErrors.addParamChangeListener(paramChangeListener);

        paramNoDataValueUsed = new Parameter("noDataValueUsedParam", true);
        paramNoDataValueUsed.getProperties().setLabel("No-data value to be used on arithmetic exceptions: "); /*I18N*/
        paramNoDataValueUsed.addParamChangeListener(paramChangeListener);

        paramNoDataValue = new Parameter("noDataValueParam", 0.0F);
        paramNoDataValue.setUIEnabled(false);

        setArithmetikValues();
    }

    private void createUI() {
        newProductButton = new JButton("New...");
        newProductButton.setName("newProductButton");
        newProductButton.addActionListener(createNewProductButtonListener());

        newBandButton = new JButton("New...");
        newBandButton.setName("newBandButton");
        newBandButton.addActionListener(createNewBandButtonListener());

        editExpressionButton = new JButton("Edit Expression...");
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
        GridBagUtils.addToPanel(panel, paramUseNewProduct.getEditor().getComponent(), gbc,
                                "fill=NONE, anchor=EAST, gridwidth=2");
        GridBagUtils.addToPanel(panel, newProductButton, gbc, "weightx=0, gridwidth=1");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, paramBand.getEditor().getLabelComponent(), gbc,
                                "insets.top=4, gridwidth=3, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, paramBand.getEditor().getComponent(), gbc,
                                "insets.top=3, gridwidth=3, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, paramCreateVirtualBand.getEditor().getComponent(), gbc,
                                "weightx=1, fill=NONE, gridwidth=1, anchor=EAST");
        GridBagUtils.addToPanel(panel, paramUseNewBand.getEditor().getComponent(), gbc, "weightx=0");
        GridBagUtils.addToPanel(panel, newBandButton, gbc);

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, paramExpression.getEditor().getLabelComponent(), gbc,
                                "insets.top=4, gridwidth=3, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, paramExpression.getEditor().getComponent(), gbc,
                                "weighty=1, insets.top=3, gridwidth=3, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, editExpressionButton, gbc,
                                "weighty=0, insets.top=3, gridwidth=3, fill=NONE, anchor=EAST");

        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBorder(UIUtils.createGroupBorder("Exception Handling"));
        jPanel.add(paramWarnOnErrors.getEditor().getComponent(), BorderLayout.NORTH);
        jPanel.add(paramNoDataValueUsed.getEditor().getComponent(), BorderLayout.WEST);
        jPanel.add(paramNoDataValue.getEditor().getComponent());

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, jPanel, gbc, "insets.top=10, gridwidth=3, fill=BOTH, anchor=WEST");

        setContent(panel);
    }

    private void updateUIState(String parameterName) {
        boolean useNewProduct = (Boolean) paramUseNewProduct.getValue();
        final String productDisplayName = paramProduct.getValueAsText();
        boolean productIsSelected = productDisplayName != null && productDisplayName.length() > 0;
        String[] bandValueSet = paramBand.getProperties().getValueSet();
        boolean paramBandHasValidValueSet = bandValueSet != null && bandValueSet.length > 0;
        boolean createVirtualBand = getCreateVirtualBand();
        newProductButton.setEnabled(!createVirtualBand && useNewProduct);
        paramUseNewProduct.setUIEnabled(!createVirtualBand);
        paramProduct.setUIEnabled(!useNewProduct);
        paramUseNewBand.setUIEnabled(!createVirtualBand && productIsSelected && paramBandHasValidValueSet);
        paramBand.setUIEnabled(!isUseNewBand() && productIsSelected);
        newBandButton.setEnabled(isUseNewBand() && productIsSelected);
        paramNoDataValue.setUIEnabled((Boolean) paramNoDataValueUsed.getValue());
        if (parameterName == null) {
            return;
        }

        if (parameterName.equals(_PARAM_NAME_USE_NEW_PRODUCT)) {
            if (useNewProduct) {
                oldSelectedProduct = paramProduct.getValueAsText();
                targetProduct = null;
            }
            paramProduct.getProperties().setValueSetBound(!useNewProduct);
            paramProduct.setValue(useNewProduct ? "" : oldSelectedProduct, null);
        } else if (parameterName.equals(_PARAM_NAME_USE_NEW_BAND)) {
            if (isUseNewBand()) {
                oldSelectedBand = paramBand.getValueAsText();
                targetBand = null;
            } else {
                if (oldSelectedBand == null || oldSelectedBand.trim().length() == 0 || !targetProduct.containsBand(
                        oldSelectedBand)) {
                    oldSelectedBand = paramBand.getProperties().getValueSet()[0];
                }
            }
            paramBand.getProperties().setValueSetBound(!isUseNewBand());
            paramBand.setValue(isUseNewBand() ? "" : oldSelectedBand, null);
        } else if (parameterName.equals(_PARAM_NAME_PRODUCT)) {
            oldSelectedBand = null;
            paramUseNewBand.setValue(true, null);
            String selectedProductDisplayName = paramProduct.getValueAsText();
            Product product = productsList.getByDisplayName(selectedProductDisplayName);
            if (product != null) {
                if (useNewProduct) {
                    showErrorDialog("A product with the name '" + selectedProductDisplayName + "' already exists.\n"
                            + "Please choose a another one."); /*I18N*/
                    paramProduct.setValue("", null);
                    targetProduct = null;
                } else {
                    targetProduct = product;
                }
            }
            if (targetBand != null && targetProduct != null) {
                targetBand = createTargetBand(targetBand.getName(), targetBand.getDataType(), targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight());
            }
            boolean b = productIsSelected && targetProduct != null;
            paramExpression.setUIEnabled(b);
            editExpressionButton.setEnabled(b);
            if (b) {
                paramBand.setValueSet(getTargetBandNames());
                setArithmetikValues();
            }
        } else if (parameterName.equals(_PARAM_NAME_BAND)) {
            String selectedBandName = paramBand.getValueAsText();
            Band band = null;
            if (targetProduct != null) {
                if (selectedBandName != null && selectedBandName.length() > 0) {
                    band = targetProduct.getBand(selectedBandName);
                }
            }
            if (band != null) {
                if (isUseNewBand()) {
                    showErrorDialog(
                            "A band with the name '" + selectedBandName + "' already exists in the selected product.\n"
                                    + "Please choose another one."); /*I18N*/
                    targetBand = null;
                } else {
                    targetBand = band;
                }
            }
        } else if (parameterName.equals(_PARAM_NAME_CREATE_VIRTUAL_BAND)) {
            if (createVirtualBand) {
                paramUseNewBand.setValueAsText("true", null);
                paramUseNewProduct.setValueAsText("false", null);
            }
            targetBand = null;
            paramBand.setValue("", null);
        }
    }

    private String[] getTargetBandNames() {
        final List<String> names = new ArrayList<String>();
        final Band[] bands = targetProduct.getBands();
        for (final Band band : bands) {
            if (!(band instanceof VirtualBand)) {
                names.add(band.getName());
            }
        }
        return names.toArray(new String[names.size()]);
    }

    private void setArithmetikValues() {
        final ParamProperties props = paramExpression.getProperties();
        props.setPropertyValue(ParamProperties.COMP_PRODUCTS_FOR_BAND_ARITHMETHIK_KEY
                , getCompatibleProducts());
        props.setPropertyValue(ParamProperties.SEL_PRODUCT_FOR_BAND_ARITHMETHIK_KEY
                , targetProduct);
    }

    private Product[] getCompatibleProducts() {
        if (targetProduct == null) {
            return null;
        }
        Vector<Product> compatibleProducts = new Vector<Product>();
        compatibleProducts.add(targetProduct);
        if (!getCreateVirtualBand()) {
            final float geolocationEps = getGeolocationEps();
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
                final Product product = productsList.getByDisplayName(oldSelectedProduct);
                final int selectedSourceIndex = productsList.indexOf(product);

                final NewProductDialog dialog = new NewProductDialog(getJDialog(),
                                                                     productsList,
                                                                     selectedSourceIndex,
                                                                     false,
                                                                     "arithm");

                if (dialog.show() == NewProductDialog.ID_OK) {
                    targetProduct = dialog.getResultProduct();
                    paramProduct.setValue(targetProduct.getName(), null);
                }
            }
        };
    }

    private ActionListener createNewBandButtonListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                NewBandDialog dialog = new NewBandDialog(getJDialog(), targetProduct);
                if (dialog.show() == NewBandDialog.ID_OK) {
                    int width = targetProduct.getSceneRasterWidth();
                    int height = targetProduct.getSceneRasterHeight();
                    targetBand = createTargetBand(dialog.getNewBandsName(), dialog.getNewBandsDataType(), width, height);
                    targetBand.setDescription(dialog.getNewBandsDesc());
                    targetBand.setUnit(dialog.getNewBandsUnit());
                    paramBand.setValue(targetBand.getName(), null);
                }
            }
        };
    }

    private Band createTargetBand(String newBandName, int newBandDataType, int width, int height) {
        final Band band;
        if (getCreateVirtualBand()) {
            band = new VirtualBand(newBandName, newBandDataType, width, height, "0");
        } else {
            band = new Band(newBandName, newBandDataType, width, height);
        }
        return band;
    }

    private ActionListener createEditExpressionButtonListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ProductExpressionPane pep = ProductExpressionPane.createGeneralExpressionPane(getCompatibleProducts(),
                                                                                              targetProduct,
                                                                                              visatApp.getPreferences());
                pep.setCode(paramExpression.getValueAsText());
                int status = pep.showModalDialog(getJDialog(), "Arithmetic Expression Editor");
                if (status == ModalDialog.ID_OK) {
                    paramExpression.setValue(pep.getCode(), null);
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
        if (products == null || products.length == 0) {
            return false;
        }

        final String expression = paramExpression.getValueAsText();
        if (expression == null || expression.trim().length() == 0) {
            return false;
        }

        final int defaultIndex = Arrays.asList(products).indexOf(visatApp.getSelectedProduct());
        final Namespace namespace = BandArithmetic.createDefaultNamespace(products,
                                                                          defaultIndex == -1 ? 0 : defaultIndex);
        final Parser parser = new ParserImpl(namespace, false);
        try {
            final Term term = parser.parse(expression);
            final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
            final String targetBandName = paramBand.getValueAsText();
            if (targetProduct != null && targetProduct.containsRasterDataNode(targetBandName)) {
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
        return (Boolean) paramCreateVirtualBand.getValue();
    }

    private float getGeolocationEps() {
        return (float) visatApp.getPreferences().getPropertyDouble(VisatApp.PROPERTY_KEY_GEOLOCATION_EPS,
                                                                   VisatApp.PROPERTY_DEFAULT_GEOLOCATION_EPS);
    }
}
