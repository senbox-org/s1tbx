package org.esa.nest.dat.dialogs;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.ByteOrder;
import java.text.NumberFormat;

public class ComplexBinaryDialog extends ModalDialog {

    private final NumberFormat numFormat = NumberFormat.getNumberInstance();
    private final ProteryListener propListener = new ProteryListener();

    private int rasterWidth = 0;
    private int rasterHeight = 0;
    private int numBands = 1;
    private int dataType = ProductData.TYPE_UINT16;
    private int headerBytes = 0;
    private ByteOrder byteOrder = ByteOrder.nativeOrder();
    private String interleave = BSQ;

    private final JFormattedTextField rasterWidthField = DialogUtils.createFormattedTextField(numFormat, rasterWidth, propListener);
    private final JFormattedTextField rasterHeightField = DialogUtils.createFormattedTextField(numFormat, rasterHeight, propListener);
    private final JFormattedTextField numBandsField = DialogUtils.createFormattedTextField(numFormat, numBands, propListener);
    private final JFormattedTextField headerBytesField = DialogUtils.createFormattedTextField(numFormat, headerBytes, propListener);


    private final JComboBox dataTypeBox = new JComboBox(new String[]{
            ProductData.TYPESTRING_INT16,
            ProductData.TYPESTRING_INT32,
            ProductData.TYPESTRING_FLOAT32});

    private final JComboBox byteOrderBox = new JComboBox(new String[]{"BIG ENDIAN", "LITTLE ENDIAN"});

    private final JComboBox interleaveBox = new JComboBox(new String[]{BSQ});
    final static String BSQ = "BSQ";
    final static String BIP = "BIP";
    final static String BIL = "BIL";

    public ComplexBinaryDialog(Window parent, String helpID) {
        super(parent, "Complex Binary", ModalDialog.ID_OK_CANCEL_HELP, helpID); /* I18N */
    }

    @Override
    public int show() {
        dataTypeBox.addPropertyChangeListener("value", propListener);
        dataTypeBox.setSelectedItem(ProductData.getTypeString(dataType));

        createUI();
        return super.show();
    }

    public int getRasterWidth() {
        return rasterWidth;
    }

    public int getRasterHeight() {
        return rasterHeight;
    }

    public int getNumBands() {
        return numBands;
    }

    public int getDataType() {
        return dataType;
    }

    public int getHeaderBytes() {
        return headerBytes;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public String getInterleave() {
        return interleave;
    }

    private void createUI() {

        final DialogUtils.ComponentListPanel contentPane = new DialogUtils.ComponentListPanel();

        contentPane.addComponent("Width:", rasterWidthField);
        contentPane.addComponent("Height:", rasterHeightField);
        contentPane.addComponent("Number Of Bands:", numBandsField);
        contentPane.addComponent("Complex Data Type:", dataTypeBox);
        contentPane.addComponent("Header Bytes:", headerBytesField);
        contentPane.addComponent("Byte Order:", byteOrderBox);
        contentPane.addComponent("Interleave:", interleaveBox);

        setContent(contentPane);
    }

    @Override
    protected void onCancel() {
        super.onCancel();
    }

    @Override
    protected void onOK() {
        super.onOK();

        dataType = ProductData.getType((String) dataTypeBox.getSelectedItem());
        if (byteOrderBox.getSelectedItem().equals("BIG ENDIAN"))
            byteOrder = ByteOrder.BIG_ENDIAN;
        else
            byteOrder = ByteOrder.LITTLE_ENDIAN;

        interleave = (String) interleaveBox.getSelectedItem();
    }

    @Override
    protected boolean verifyUserInput() {
        boolean b = super.verifyUserInput();

        boolean valid = true;
        if (rasterWidth <= 0 || rasterHeight <= 0 || numBands <= 0)
            valid = false;

        return b && valid;
    }

    class ProteryListener implements PropertyChangeListener {
        /**
         * Called when a field's "value" property changes.
         */
        public void propertyChange(PropertyChangeEvent e) {
            final Object source = e.getSource();
            if (source == rasterWidthField) {
                rasterWidth = ((Number) rasterWidthField.getValue()).intValue();
            } else if (source == rasterHeightField) {
                rasterHeight = ((Number) rasterHeightField.getValue()).intValue();
            } else if (source == numBandsField) {
                numBands = ((Number) numBandsField.getValue()).intValue();
            } else if (source == headerBytesField) {
                headerBytes = ((Number) headerBytesField.getValue()).intValue();
            }
        }
    }

}
