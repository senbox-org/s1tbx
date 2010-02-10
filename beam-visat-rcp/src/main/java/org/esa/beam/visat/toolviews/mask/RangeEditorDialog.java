package org.esa.beam.visat.toolviews.mask;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;

import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Insets;
import java.awt.Window;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class RangeEditorDialog extends ModalDialog {

    private String code;
    private PropertyContainer container;
    private DefaultComboBoxModel rasterModel;
    private Model model;

    public static void main(String[] args) {
        final String[] rasterNames = new String[]{"raster_1", "raster_2", "raster_3"};
        final RangeEditorDialog editorDialog = new RangeEditorDialog(null, new Model(rasterNames));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                editorDialog.show();
            }
        });
    }

    RangeEditorDialog(Window window, Model model) {
        super(window, "New Range Mask",
              ModalDialog.ID_OK_CANCEL | ModalDialog.ID_HELP, "rangeEditor");
        this.model = model;
        container = PropertyContainer.createObjectBacked(this.model);
        getJDialog().setResizable(false);
        rasterModel = new DefaultComboBoxModel(this.model.rasterNames);

        setContent(createUI());
    }

    Model getModel() {
        return model;
    }

    private JComponent createUI() {
        final TableLayout layout = new TableLayout(5);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(0.0);
        layout.setTablePadding(3, 3);
        layout.setColumnPadding(1, new Insets(3, 6, 3, 6));
        layout.setColumnPadding(3, new Insets(3, 6, 3, 6));
        layout.setColumnWeightX(1, 0.0);
        layout.setColumnWeightX(3, 0.0);
        final JPanel panel = new JPanel(layout);
        panel.add(new JLabel("Min value:"), new TableLayout.Cell(0, 0));
        panel.add(new JLabel("Raster:"), new TableLayout.Cell(0, 2));
        panel.add(new JLabel("Max value:"), new TableLayout.Cell(0, 4));

        final DoubleFormatter formatter = new DoubleFormatter("###0.0###");
        final JFormattedTextField minValueField = new JFormattedTextField(formatter);
        final JFormattedTextField maxValueField = new JFormattedTextField(formatter);
        final JComboBox rasterNameComboBox = new JComboBox(rasterModel);
        panel.add(minValueField);
        panel.add(new JLabel("<html><b>&lt;=</b>"));
        panel.add(rasterNameComboBox);
        panel.add(new JLabel("<html><b>&lt;=</b>"));
        panel.add(maxValueField);
        final BindingContext context = new BindingContext(container);
        context.bind("rasterName", rasterNameComboBox);
        context.bind("minValue", minValueField);
        context.bind("maxValue", maxValueField);
        return panel;
    }

    @Override
    protected boolean verifyUserInput() {
        String errorMsg = null;
        final boolean minGreaterMax = model.getMaxValue() <= model.getMinValue();
        if (minGreaterMax) {
            errorMsg = "The specified maximum is less or equal to the minimum.";
        }
        final boolean nameEmpty = StringUtils.isNullOrEmpty(model.getRasterName());
        if (nameEmpty) {
            errorMsg = "No raster selected.";
        }
        if (errorMsg != null) {
            JOptionPane.showMessageDialog(this.getJDialog(), errorMsg,
                                          "New Range Mask", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    static class Model {

        private String rasterName;
        private double minValue;
        private double maxValue;
        private final String[] rasterNames;

        Model(String[] rasterNames) {
            this.rasterNames = rasterNames;
        }

        public void setRasterName(String rasterName) {
            this.rasterName = rasterName;
        }

        public String getRasterName() {
            return rasterName;
        }

        public double getMinValue() {
            return minValue;
        }

        public void setMinValue(double minValue) {
            this.minValue = minValue;
        }

        public double getMaxValue() {
            return maxValue;
        }

        public void setMaxValue(double maxValue) {
            this.maxValue = maxValue;
        }

        public String[] getRasterNames() {
            return rasterNames;
        }
    }

    private static class DoubleFormatter extends JFormattedTextField.AbstractFormatter {

        private final DecimalFormat format;

        DoubleFormatter(String pattern) {
            final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
            format = new DecimalFormat(pattern, decimalFormatSymbols);

            format.setParseIntegerOnly(false);
            format.setParseBigDecimal(false);
            format.setDecimalSeparatorAlwaysShown(true);
        }

        @Override
        public Object stringToValue(String text) throws ParseException {
            return format.parse(text).doubleValue();
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (value == null) {
                return "";
            }
            return format.format(value);
        }
    }

}
