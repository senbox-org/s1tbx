package org.esa.beam.framework.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.binding.BindingContext;

import javax.measure.unit.NonSI;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

/**
 * This user interface provides a world map and text fields to define region bounds.
 * The input values from the text fields and from the world map are reflected in the {@link BindingContext}, which can
 * be retrieved using {@link #getBindingContext()}.
 */
public class RegionBoundsInputUI {

    public final static String PROPERTY_NORTH_BOUND = RegionSelectableWorldMapPane.NORTH_BOUND;
    public final static String PROPERTY_EAST_BOUND = RegionSelectableWorldMapPane.EAST_BOUND;
    public final static String PROPERTY_SOUTH_BOUND = RegionSelectableWorldMapPane.SOUTH_BOUND;
    public final static String PROPERTY_WEST_BOUND = RegionSelectableWorldMapPane.WEST_BOUND;

    private final BindingContext bindingContext;
    private final JLabel northLabel;
    private final JFormattedTextField northLatField;
    private final JLabel northDegreeLabel;
    private final JLabel eastLabel;
    private final JFormattedTextField eastLonField;
    private final JLabel eastDegreeLabel;
    private final JLabel southLabel;
    private final JFormattedTextField southLatField;
    private final JLabel southDegreeLabel;
    private final JLabel westLabel;
    private final JFormattedTextField westLonField;
    private final JLabel westDegreeLabel;
    private final JPanel worldMapPaneUI;
    private JPanel ui;

    /**
     * Initializes a RegionBoundsInputUI.
     * This constructor creates the user interface and a binding context with default values.</br>
     * The created binding context can be retrieved via {@link #getBindingContext()}.
     */
    public RegionBoundsInputUI() {
        this(75, 30, 35, -15);
    }

    /**
     * Initializes a RegionBoundsInputUI with the given parameters.
     * If the parameters are valid geographic coordinates, they are used to initialize the user
     * interface and to create a binding context.</br>
     * If the values are invalid, default values will be used.</br>
     * The created binding context can be retrieved via {@link #getBindingContext()}.
     *
     * @param northBound The northern bounding latitude value
     * @param eastBound  The eastern bound longitude value
     * @param southBound The southern bound latitude value
     * @param westBound  The western bound longitude value
     */
    public RegionBoundsInputUI(final double northBound,
                               final double eastBound,
                               final double southBound,
                               final double westBound) {
        this(createBindingContext(northBound, eastBound, southBound, westBound));
    }

    /**
     * Initializes a RegionBoundsInputUI with the given {@link BindingContext bindingContext}.
     * The bindingContext has to contain four parameters: {@link #PROPERTY_NORTH_BOUND northBound} ,
     * {@link #PROPERTY_SOUTH_BOUND southBound}, {@link #PROPERTY_WEST_BOUND westBound} and
     * {@link #PROPERTY_EAST_BOUND eastBound}.</br>
     * If the bindingContext contains geographic coordinates, these coordinates are used to initialize the user
     * interface.</br>
     *
     * @param bindingContext The binding context which is needed for initialisation.
     */
    public RegionBoundsInputUI(final BindingContext bindingContext) {
        RegionSelectableWorldMapPane.ensureValidBindingContext(bindingContext);
        this.bindingContext = bindingContext;

        final WorldMapPaneDataModel worldMapPaneDataModel = new WorldMapPaneDataModel();
        final RegionSelectableWorldMapPane worldMapPane = new RegionSelectableWorldMapPane(worldMapPaneDataModel, bindingContext);
        worldMapPaneUI = worldMapPane.createUI();

        northLabel = new JLabel("North:");
        northDegreeLabel = createDegreeLabel();
        northLatField = createTextField();

        eastDegreeLabel = createDegreeLabel();
        eastLabel = new JLabel("East:");
        eastLonField = createTextField();

        southLabel = new JLabel("South:");
        southDegreeLabel = createDegreeLabel();
        southLatField = createTextField();

        westDegreeLabel = createDegreeLabel();
        westLabel = new JLabel("West:");
        westLonField = createTextField();

        bindingContext.bind(PROPERTY_WEST_BOUND, westLonField);
        bindingContext.bind(PROPERTY_EAST_BOUND, eastLonField);
        bindingContext.bind(PROPERTY_NORTH_BOUND, northLatField);
        bindingContext.bind(PROPERTY_SOUTH_BOUND, southLatField);
    }

    /**
     * Enables or disables all child components.
     *
     * @param enabled -
     */
    public void setEnabled(final boolean enabled) {
        northLabel.setEnabled(enabled);
        northLatField.setEnabled(enabled);
        northDegreeLabel.setEnabled(enabled);

        eastLabel.setEnabled(enabled);
        eastLonField.setEnabled(enabled);
        eastDegreeLabel.setEnabled(enabled);

        southLabel.setEnabled(enabled);
        southLatField.setEnabled(enabled);
        southDegreeLabel.setEnabled(enabled);

        westLabel.setEnabled(enabled);
        westLonField.setEnabled(enabled);
        westDegreeLabel.setEnabled(enabled);

        worldMapPaneUI.setEnabled(enabled);
    }

    /**
     * @return a {@link JPanel} which contains the user interface elements
     */
    public JPanel getUI() {
        if (ui == null) {
            final JPanel fieldsPanel = GridBagUtils.createPanel();
            final GridBagConstraints fieldsGBC = GridBagUtils.createDefaultConstraints();
            fieldsGBC.anchor = GridBagConstraints.WEST;

            fieldsGBC.gridy = 0;
            GridBagUtils.addToPanel(fieldsPanel, northLabel, fieldsGBC, "gridx=3");
            GridBagUtils.addToPanel(fieldsPanel, northLatField, fieldsGBC, "gridx=4");
            GridBagUtils.addToPanel(fieldsPanel, northDegreeLabel, fieldsGBC, "gridx=5");

            fieldsGBC.gridy = 1;
            GridBagUtils.addToPanel(fieldsPanel, westLabel, fieldsGBC, "gridx=0");
            GridBagUtils.addToPanel(fieldsPanel, westLonField, fieldsGBC, "gridx=1");
            GridBagUtils.addToPanel(fieldsPanel, westDegreeLabel, fieldsGBC, "gridx=2");
            GridBagUtils.addToPanel(fieldsPanel, eastLabel, fieldsGBC, "gridx=6");
            GridBagUtils.addToPanel(fieldsPanel, eastLonField, fieldsGBC, "gridx=7");
            GridBagUtils.addToPanel(fieldsPanel, eastDegreeLabel, fieldsGBC, "gridx=8");

            fieldsGBC.gridy = 2;
            GridBagUtils.addToPanel(fieldsPanel, southLabel, fieldsGBC, "gridx=3");
            GridBagUtils.addToPanel(fieldsPanel, southLatField, fieldsGBC, "gridx=4");
            GridBagUtils.addToPanel(fieldsPanel, southDegreeLabel, fieldsGBC, "gridx=5");

            ui = GridBagUtils.createPanel();

            final GridBagConstraints mainGBC = GridBagUtils.createDefaultConstraints();
            mainGBC.fill = GridBagConstraints.HORIZONTAL;

            mainGBC.gridy = 0;
            GridBagUtils.addToPanel(ui, fieldsPanel, mainGBC);
            mainGBC.gridy = 1;
            GridBagUtils.addToPanel(ui, worldMapPaneUI, mainGBC, "insets.top=10");
        }

        return ui;
    }

    /**
     * Returns the binding context which contains property values {@link #PROPERTY_NORTH_BOUND northBound} ,
     * {@link #PROPERTY_SOUTH_BOUND southBound}, {@link #PROPERTY_WEST_BOUND westBound}, and
     * {@link #PROPERTY_EAST_BOUND eastBound}. This method should be used to get the bounds set by the UI components.
     * It is needed when no binding context has been passed to the RegionBoundsInputUI initially.
     *
     * @return the binding context.
     */
    public BindingContext getBindingContext() {
        return bindingContext;
    }

    private static BindingContext createBindingContext(double northBound, double eastBound, double southBound, double westBound) {
        final Bounds bounds = new Bounds(northBound, eastBound, southBound, westBound);
        final PropertyContainer container = PropertyContainer.createObjectBacked(bounds);
        return new BindingContext(container);
    }

    private JFormattedTextField createTextField() {
        final int fieldWidth = 60;
        final DoubleFormatter textFormatter = new DoubleFormatter("###0.0##");

        final JFormattedTextField textField = new JFormattedTextField(textFormatter);
        textField.setHorizontalAlignment(JTextField.RIGHT);

        final int defaultHeight = textField.getPreferredSize().height;
        final Dimension size = new Dimension(fieldWidth, defaultHeight);

        textField.setMinimumSize(size);
        textField.setPreferredSize(size);
        textField.setMaximumSize(size);

        return textField;
    }

    private JLabel createDegreeLabel() {
        return new JLabel(NonSI.DEGREE_ANGLE.toString());
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

    private static class Bounds {

        double northBound;
        double eastBound;
        double southBound;
        double westBound;

        private Bounds(double northBound, double eastBound, double southBound, double westBound) {
            this.northBound = northBound;
            this.eastBound = eastBound;
            this.southBound = southBound;
            this.westBound = westBound;
        }
    }
}
