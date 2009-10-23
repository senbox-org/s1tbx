package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.binding.ValueAccessor;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.swing.TableLayout;
import com.jidesoft.swing.ComboBoxSearchable;
import com.jidesoft.swing.SearchableUtils;
import org.esa.beam.framework.ui.AbstractDialog;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.ValueEditorsPane;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;
import org.geotools.referencing.AbstractIdentifiedObject;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.geotools.referencing.operation.projection.MapProjection.AbstractProvider;
import org.geotools.referencing.operation.projection.TransverseMercator;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Projection;

import javax.measure.unit.Unit;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @author Marco Peters
 * @author Marco Zühlke
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class ProjectionDefinitionForm extends JPanel {

    private static final String OPERATION_WRAPPER = "operationWrapper";
    private static final String DATUM = "datum";
    private static final String PARAMETERS = "parameters";

    private final List<GeodeticDatum> datumList;
    private final List<OperationMethodWrapper> operationMethodWrapperList;
    private ProjectionDefinitionForm.Model model;
    private ValueContainer vc;
    private final Window parent;
    private JComboBox operationComboBox;
    private JComboBox datumComboBox;
    private JButton paramButton;

    public ProjectionDefinitionForm(Window parent, List<OperationMethod> operationMethodList,
                                    List<GeodeticDatum> datumList) {
        this.parent = parent;

        this.datumList = new ArrayList<GeodeticDatum>(datumList);
        Collections.sort(this.datumList, AbstractIdentifiedObject.NAME_COMPARATOR);
        GeodeticDatum wgs84Datum = null;
        for (GeodeticDatum geodeticDatum : datumList) {
            if ("World Geodetic System 1984".equals(geodeticDatum.getName().getCode())) {
                wgs84Datum = geodeticDatum;
                break;
            }
        }

        this.operationMethodWrapperList = new ArrayList<OperationMethodWrapper>(operationMethodList.size() + 2);
        for (OperationMethod method : operationMethodList) {
            operationMethodWrapperList.add(new OperationMethodDelegate(method));
        }
        OperationMethodWrapper defaultMethod = new WGS84OperationMethod(wgs84Datum);
        operationMethodWrapperList.add(defaultMethod);
        operationMethodWrapperList.add(new UTMZonesOperationMethod(wgs84Datum));
        Collections.sort(this.operationMethodWrapperList, new OperationMethodWrapperComparator());


        model = new Model();
        model.operationWrapper = defaultMethod;
        model.datum = this.datumList.get(0);

        vc = ValueContainer.createObjectBacked(model);
        vc.addPropertyChangeListener(new UpdateListener());

        creatUI();
        updateModel(OPERATION_WRAPPER);
    }

    public CoordinateReferenceSystem getProcjetedCRS() throws FactoryException {
        return model.operationWrapper.getCRS(model.parameters, model.datum);
    }

    private void creatUI() {
        final TableLayout tableLayout = new TableLayout(2);
        setLayout(tableLayout);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);

        tableLayout.setColumnWeightX(0, 0.0);
        tableLayout.setColumnWeightX(1, 1.0);
        tableLayout.setCellColspan(2, 0, 2);
        tableLayout.setCellAnchor(2, 0, TableLayout.Anchor.EAST);
        tableLayout.setCellFill(2, 0, TableLayout.Fill.NONE);

        final JLabel transformLabel = new JLabel("Transform:");
        final JLabel datumLabel = new JLabel("Datum:");

        operationComboBox = new JComboBox(operationMethodWrapperList.toArray());
        operationComboBox.setEditable(false); // combobox searchable only works when combobox is not editable.
        final ComboBoxSearchable methodSearchable = new OperationMethodWrapperSearchable(operationComboBox);
        methodSearchable.installListeners();
        operationComboBox.setRenderer(new OperationMethodWrapperCellRenderer());

        datumComboBox = new JComboBox(datumList.toArray());
        datumComboBox.setEditable(false); // combobox searchable only works when combobox is not editable.
        SearchableUtils.installSearchable(datumComboBox);
        datumComboBox.setRenderer(new IdentifiedObjectCellRenderer());
        final ComboBoxSearchable datumSearchable = new IdentifiedObjectSearchable(datumComboBox);
        datumSearchable.installListeners();

        paramButton = new JButton("Parameters");
        paramButton.addActionListener(new ParameterButtonListener());
        add(transformLabel);
        add(operationComboBox);
        add(datumLabel);
        add(datumComboBox);
        add(paramButton);
        addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateEnableState((Boolean) evt.getNewValue());
            }
        });
        final BindingContext context = new BindingContext(vc);
        context.bind(OPERATION_WRAPPER, operationComboBox);
        context.bind(DATUM, datumComboBox);
    }

    private void updateEnableState(boolean componentEnabled) {
        operationComboBox.setEnabled(componentEnabled);
        datumComboBox.setEnabled(componentEnabled && model.operationWrapper.isDatumChangable());
        paramButton.setEnabled(componentEnabled && model.operationWrapper.hasParameters());
    }

    private void updateModel(String propertyName) {
        if (OPERATION_WRAPPER.equals(propertyName)) {
            GeodeticDatum defaultDatum = model.operationWrapper.getDefaultDatum();
            if (defaultDatum != null) {
                vc.setValue(DATUM, defaultDatum);
            }
        }
        boolean hasParameters = model.operationWrapper.hasParameters();
        if (hasParameters) {
            model.parameters = model.operationWrapper.getParameter();
            if (model.datum != null && hasParameter("semi_major") && hasParameter("semi_minor")) {
                Ellipsoid ellipsoid = model.datum.getEllipsoid();
                model.parameters.parameter("semi_major").setValue(ellipsoid.getSemiMajorAxis());
                model.parameters.parameter("semi_minor").setValue(ellipsoid.getSemiMinorAxis());
            }
        }
        updateEnableState(true);
    }

    private boolean hasParameter(String name) {
        try {
            model.parameters.parameter(name);
        } catch (ParameterNotFoundException ignored) {
            return false;
        }
        return true;
    }

    private class UpdateListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if (PARAMETERS.equals(propertyName)) {
                return;
            }
            updateModel(propertyName);
        }
    }

    public static void main(String[] args) {
        List<GeodeticDatum> datumList = createDatumList();
        List<OperationMethod> methodList = createProjectionMethodList();
        final JFrame frame = new JFrame("Projection Method Form Test");
        final ProjectionDefinitionForm definitionForm = new ProjectionDefinitionForm(frame, methodList, datumList);
        frame.setContentPane(definitionForm);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    static List<OperationMethod> createProjectionMethodList() {
        MathTransformFactory factory = ReferencingFactoryFinder.getMathTransformFactory(null);
        Set<OperationMethod> methods = factory.getAvailableMethods(Projection.class);
        return new ArrayList<OperationMethod>(methods);
    }

    static List<GeodeticDatum> createDatumList() {
        DatumAuthorityFactory factory = ReferencingFactoryFinder.getDatumAuthorityFactory("EPSG", null);
        List<String> datumCodes = retrieveCodes(GeodeticDatum.class, factory);
        List<GeodeticDatum> datumList = new ArrayList<GeodeticDatum>(datumCodes.size());
        for (String datumCode : datumCodes) {
            try {
                datumList.add(factory.createGeodeticDatum(datumCode));
            } catch (FactoryException ignored) {
            }
        }
        return datumList;
    }

    private static List<String> retrieveCodes(Class<? extends GeodeticDatum> crsType, AuthorityFactory factory) {
        try {
            Set<String> localCodes = factory.getAuthorityCodes(crsType);
            return new ArrayList<String>(localCodes);
        } catch (FactoryException ignore) {
            return Collections.emptyList();
        }
    }

    private static ValueContainer createValueContainer(ParameterValueGroup valueGroup) {
        final ValueContainer vc = new ValueContainer();

        final List<GeneralParameterValue> values = valueGroup.values();
        for (GeneralParameterValue value : values) {
            final GeneralParameterDescriptor descriptor = value.getDescriptor();
            final Class valueType;
            Set validValues = null;
            if (descriptor instanceof ParameterDescriptor) {
                ParameterDescriptor parameterDescriptor = (ParameterDescriptor) descriptor;
                valueType = parameterDescriptor.getValueClass();
                validValues = parameterDescriptor.getValidValues();
            } else {
                valueType = Double.TYPE;
            }

            final String paramName = descriptor.getName().getCode();
            final ValueDescriptor vd = new ValueDescriptor(paramName, valueType);
            final ParameterValue<?> parameterValue = valueGroup.parameter(paramName);
            if (parameterValue.getUnit() != null) {
                vd.setUnit(String.valueOf(parameterValue.getUnit()));
            }
            if (validValues != null) {
                vd.setValueSet(new ValueSet(validValues.toArray()));
            }

            vd.setDefaultConverter();
            final ValueModel valueModel = new ValueModel(vd, new ValueAccessor() {
                @Override
                public Object getValue() {
                    return parameterValue.getValue();
                }

                @Override
                public void setValue(Object value) {
                    parameterValue.setValue(value);
                }
            });
            vc.addModel(valueModel);
        }
        return vc;
    }

    private static class Model {

        private OperationMethodWrapper operationWrapper;
        private GeodeticDatum datum;
        private ParameterValueGroup parameters;
    }

    private class ParameterButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            final String operationName = model.operationWrapper.getName();
            final ModalDialog modalDialog = new ModalDialog(parent, operationName + " - Parameters",
                                                            ModalDialog.ID_OK_CANCEL, null);
            final ParameterValueGroup workCopy = model.parameters.clone();
            final ValueContainer valueContainer = createValueContainer(workCopy);
            modalDialog.setContent(new ValueEditorsPane(valueContainer).createPanel());
            if (modalDialog.show() == AbstractDialog.ID_OK) {
                vc.setValue("parameters", workCopy);
            }
        }
    }

    private static class IdentifiedObjectSearchable extends ComboBoxSearchable {

        private IdentifiedObjectSearchable(JComboBox operationComboBox) {
            super(operationComboBox);
        }

        @Override
        protected String convertElementToString(Object o) {
            if (o instanceof IdentifiedObject) {
                IdentifiedObject identifiedObject = (IdentifiedObject) o;
                return identifiedObject.getName().getCode();
            } else {
                return super.convertElementToString(o);
            }
        }
    }

    private interface OperationMethodWrapper {

        String getName();

        boolean hasParameters();

        boolean isDatumChangable();

        GeodeticDatum getDefaultDatum();

        ParameterValueGroup getParameter();

        CoordinateReferenceSystem getCRS(ParameterValueGroup parameter, GeodeticDatum datum) throws FactoryException;
    }

    private static class WGS84OperationMethod implements OperationMethodWrapper {

        private final GeodeticDatum wgs84Datum;

        private WGS84OperationMethod(GeodeticDatum wgs84Datum) {
            this.wgs84Datum = wgs84Datum;
        }

        @Override
        public String getName() {
            return "Geographic Lat/Lon (WGS 84)";
        }

        @Override
        public boolean hasParameters() {
            return false;
        }

        @Override
        public ParameterValueGroup getParameter() {
            return ParameterGroup.EMPTY;
        }

        @Override
        public CoordinateReferenceSystem getCRS(ParameterValueGroup parameter, GeodeticDatum datum) throws
                                                                                                    FactoryException {
            return DefaultGeographicCRS.WGS84;
        }

        @Override
        public GeodeticDatum getDefaultDatum() {
            return wgs84Datum;
        }

        @Override
        public boolean isDatumChangable() {
            return false;
        }
    }


    private static class OperationMethodDelegate implements OperationMethodWrapper {

        private OperationMethod delegate;

        OperationMethodDelegate(OperationMethod method) {
            this.delegate = method;
        }

        @Override
        public String getName() {
            return delegate.getName().getCode();
        }


        @Override
        public boolean hasParameters() {
            return true;
        }

        @Override
        public ParameterValueGroup getParameter() {
            return delegate.getParameters().createValue();
        }

        @Override
        public CoordinateReferenceSystem getCRS(ParameterValueGroup parameters, GeodeticDatum datum) throws
                                                                                                     FactoryException {
            final CRSFactory crsFactory = ReferencingFactoryFinder.getCRSFactory(null);
            final CoordinateOperationFactory coFactory = ReferencingFactoryFinder.getCoordinateOperationFactory(null);

            final HashMap<String, Object> projProperties = new HashMap<String, Object>();
            projProperties.put("name", getName() + " / " + datum.getName().getCode());
            final Conversion conversion = coFactory.createDefiningConversion(projProperties, delegate, parameters);
            final HashMap<String, Object> baseCrsProperties = new HashMap<String, Object>();
            baseCrsProperties.put("name", datum.getName().getCode());
            final GeographicCRS baseCrs = crsFactory.createGeographicCRS(baseCrsProperties, datum,
                                                                         DefaultEllipsoidalCS.GEODETIC_2D);
            return crsFactory.createProjectedCRS(projProperties, baseCrs, conversion, DefaultCartesianCS.PROJECTED);
        }

        @Override
        public GeodeticDatum getDefaultDatum() {
            return null;
        }

        @Override
        public boolean isDatumChangable() {
            return true;
        }

    }

    private static class UTMZonesOperationMethod implements OperationMethodWrapper {

        private static final int MIN_UTM_ZONE = 1;
        private static final int MAX_UTM_ZONE = 60;
        private static final String NAME = "Universal Transverse Mercator";
        private static final Citation BC = Citations.fromName("BC");
        private static final String NORTH_HEMISPHERE = "North";
        private static final String SOUTH_HEMISPHERE = "South";

        private static final ParameterDescriptor[] DESCRIPTORS = new ParameterDescriptor[]{
                new DefaultParameterDescriptor<Integer>(BC, "zone", Integer.class, null, 1,
                                                        MIN_UTM_ZONE, MAX_UTM_ZONE, Unit.ONE, true),
                new DefaultParameterDescriptor<String>("hemisphere", String.class,
                                                       new String[]{NORTH_HEMISPHERE, SOUTH_HEMISPHERE},
                                                       NORTH_HEMISPHERE)
        };

        private static final ParameterDescriptorGroup UTM_PARAMETERS = new DefaultParameterDescriptorGroup(NAME,
                                                                                                           DESCRIPTORS);
        private final GeodeticDatum wgs84Datum;

        UTMZonesOperationMethod(GeodeticDatum wgs84Datum) {
            this.wgs84Datum = wgs84Datum;
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean hasParameters() {
            return true;
        }

        @Override
        public ParameterValueGroup getParameter() {
            return UTM_PARAMETERS.createValue();
        }

        @Override
        public CoordinateReferenceSystem getCRS(ParameterValueGroup parameters, GeodeticDatum datum) throws
                                                                                                     FactoryException {
            final CRSFactory crsFactory = ReferencingFactoryFinder.getCRSFactory(null);
            final CoordinateOperationFactory coFactory = ReferencingFactoryFinder.getCoordinateOperationFactory(null);

            parameters = convertToTransverseMercator(parameters, datum);

            final HashMap<String, Object> projProperties = new HashMap<String, Object>();
            projProperties.put("name", getName() + " / " + datum.getName().getCode());
            final Conversion conversion = coFactory.createDefiningConversion(projProperties,
                                                                             new TransverseMercator.Provider(),
                                                                             parameters);
            final HashMap<String, Object> baseCrsProperties = new HashMap<String, Object>();
            baseCrsProperties.put("name", datum.getName().getCode());
            final GeographicCRS baseCrs = crsFactory.createGeographicCRS(baseCrsProperties, datum,
                                                                         DefaultEllipsoidalCS.GEODETIC_2D);
            return crsFactory.createProjectedCRS(projProperties, baseCrs, conversion, DefaultCartesianCS.PROJECTED);
        }

        private ParameterValueGroup convertToTransverseMercator(final ParameterValueGroup parameters,
                                                                GeodeticDatum datum) throws ParameterNotFoundException {
            int zoneIndex = parameters.parameter("zone").intValue();
            String hemisphere = parameters.parameter("hemisphere").stringValue();
            boolean south = (SOUTH_HEMISPHERE.equals(hemisphere));

            ParameterDescriptorGroup tmParameters = new TransverseMercator.Provider().getParameters();
            ParameterValueGroup tmValues = tmParameters.createValue();

            setValue(tmValues, AbstractProvider.SEMI_MAJOR, datum.getEllipsoid().getSemiMajorAxis());
            setValue(tmValues, AbstractProvider.SEMI_MINOR, datum.getEllipsoid().getSemiMinorAxis());
            setValue(tmValues, AbstractProvider.LATITUDE_OF_ORIGIN, 0.0);
            setValue(tmValues, AbstractProvider.CENTRAL_MERIDIAN, getCentralMeridian(zoneIndex));
            setValue(tmValues, AbstractProvider.SCALE_FACTOR, 0.9996);
            setValue(tmValues, AbstractProvider.FALSE_EASTING, 500000.0);
            setValue(tmValues, AbstractProvider.FALSE_NORTHING, south ? 10000000.0 : 0.0);
            return tmValues;
        }

        private static void setValue(ParameterValueGroup values, ParameterDescriptor<Double> descriptor, double value) {
            values.parameter(descriptor.getName().getCode()).setValue(value);
        }

        /**
         * Computes the central meridian from the given UTM zone index.
         *
         * @param zoneIndex the zone index in the range 1 to {@link #MAX_UTM_ZONE}.
         *
         * @return the central meridian in the range <code>-180</code> to <code>+180</code> degree.
         */
        private static double getCentralMeridian(int zoneIndex) {
            return (zoneIndex - 0.5) * 6.0 - 180.0;
        }

        @Override
        public GeodeticDatum getDefaultDatum() {
            return wgs84Datum;
        }

        @Override
        public boolean isDatumChangable() {
            return true;
        }

    }

    private static class OperationMethodWrapperComparator implements Comparator<OperationMethodWrapper> {

        @Override
        public int compare(OperationMethodWrapper o1, OperationMethodWrapper o2) {
            final String name1 = o1.getName();
            final String name2 = o2.getName();
            return name1.compareTo(name2);
        }
    }

    private static class OperationMethodWrapperCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            final Component component = super.getListCellRendererComponent(list, value, index,
                                                                           isSelected, cellHasFocus);
            JLabel label = (JLabel) component;
            if (value != null) {
                OperationMethodWrapper wrapper = (OperationMethodWrapper) value;
                label.setText(wrapper.getName());
            }

            return label;
        }
    }

    private static class OperationMethodWrapperSearchable extends ComboBoxSearchable {

        private OperationMethodWrapperSearchable(JComboBox operationComboBox) {
            super(operationComboBox);
        }

        @Override
        protected String convertElementToString(Object o) {
            if (o instanceof OperationMethodWrapper) {
                OperationMethodWrapper wrapper = (OperationMethodWrapper) o;
                return wrapper.getName();
            } else {
                return super.convertElementToString(o);
            }
        }
    }
}
