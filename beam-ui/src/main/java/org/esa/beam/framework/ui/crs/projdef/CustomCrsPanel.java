/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.ui.crs.projdef;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyAccessor;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyPane;
import com.jidesoft.swing.ComboBoxSearchable;
import com.jidesoft.swing.SearchableUtils;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.AbstractDialog;
import org.esa.beam.framework.ui.ModalDialog;
import org.geotools.referencing.AbstractIdentifiedObject;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.datum.DefaultGeodeticDatum;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Projection;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Marco Peters
 * @author Marco Zühlke
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class CustomCrsPanel extends JPanel {

    private static final String OPERATION_WRAPPER = "operationWrapper";
    private static final String DATUM = "datum";
    private static final String PARAMETERS = "parameters";

    private final Set<GeodeticDatum> datumSet;
    private final Set<AbstractCrsProvider> crsProviderSet;
    private final CustomCrsPanel.Model model;
    private final PropertyContainer vc;
    private final Window parent;
    private JComboBox<AbstractCrsProvider> projectionComboBox;
    private JComboBox<GeodeticDatum> datumComboBox;
    private JButton paramButton;
    private static final String SEMI_MAJOR_PARAM_NAME = "semi_major";
    private static final String SEMI_MINOR_PARAM_NAME = "semi_minor";

    /**
     *  @deprecated since BEAM 5.0, use {@link #CustomCrsPanel(java.awt.Window, java.util.Set, java.util.Set)} instead
     */
    @Deprecated
    public CustomCrsPanel(Window parent) {
        this(parent, CustomCrsPanel.createDatumSet(), CustomCrsPanel.createCrsProviderSet());
    }

    public CustomCrsPanel(Window parent, Set<GeodeticDatum> datumSet, Set<AbstractCrsProvider> crsProviderSet) {
        this.parent = parent;

        this.datumSet = datumSet;
        this.crsProviderSet = crsProviderSet;

        GeodeticDatum wgs84Datum = null;
        // This is necessary because DefaultGeodeticDatum.WGS84 is
        // not equal to the geodetic WGS84 datum from the database
        for (GeodeticDatum geodeticDatum : datumSet) {
            if (DefaultGeodeticDatum.isWGS84(geodeticDatum)) {
                wgs84Datum = geodeticDatum;
                break;
            }
        }
        AbstractCrsProvider defaultMethod = new WGS84CrsProvider(wgs84Datum);
        crsProviderSet.add(defaultMethod);
        crsProviderSet.add(new UTMZonesCrsProvider(wgs84Datum));
        crsProviderSet.add(new UTMAutomaticCrsProvider(wgs84Datum));

        model = new Model();
        model.operationWrapper = defaultMethod;
        model.datum = wgs84Datum;

        vc = PropertyContainer.createObjectBacked(model);
        vc.addPropertyChangeListener(new UpdateListener());

        createUI();
        updateModel(OPERATION_WRAPPER);
    }

    public CoordinateReferenceSystem getCRS(GeoPos referencePos) throws FactoryException {
        return model.operationWrapper.getCRS(referencePos, model.parameters, model.datum);
    }

    private void createUI() {
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

        final JLabel datumLabel = new JLabel("Geodetic datum:");
        final JLabel projectionLabel = new JLabel("Projection:");

        projectionComboBox = new JComboBox<>(crsProviderSet.toArray(new AbstractCrsProvider[crsProviderSet.size()]));
        projectionComboBox.setEditable(false); // combobox searchable only works when combobox is not editable.
        final ComboBoxSearchable methodSearchable = new CrsProviderSearchable(projectionComboBox);
        methodSearchable.installListeners();
        projectionComboBox.setRenderer(new CrsProviderCellRenderer());

        datumComboBox = new JComboBox<>(datumSet.toArray(new GeodeticDatum[datumSet.size()]));
        datumComboBox.setEditable(false); // combobox searchable only works when combobox is not editable.
        SearchableUtils.installSearchable(datumComboBox);
        datumComboBox.setRenderer(new IdentifiedObjectCellRenderer());
        final ComboBoxSearchable datumSearchable = new IdentifiedObjectSearchable(datumComboBox);
        datumSearchable.installListeners();

        paramButton = new JButton("Projection Parameters...");
        paramButton.addActionListener(new ParameterButtonListener());
        add(datumLabel);
        add(datumComboBox);
        add(projectionLabel);
        add(projectionComboBox);
        add(paramButton);
        addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateEnableState((Boolean) evt.getNewValue());
            }
        });
        final BindingContext context = new BindingContext(vc);
        context.bind(OPERATION_WRAPPER, projectionComboBox);
        context.bind(DATUM, datumComboBox);
    }

    private void updateEnableState(boolean componentEnabled) {
        projectionComboBox.setEnabled(componentEnabled);
        datumComboBox.setEnabled(model.operationWrapper.isDatumChangable() && componentEnabled);
        paramButton.setEnabled(model.operationWrapper.hasParameters() && componentEnabled);
    }

    private void updateModel(String propertyName) {
        if (OPERATION_WRAPPER.equals(propertyName)) {
            GeodeticDatum defaultDatum = model.operationWrapper.getDefaultDatum();
            if (defaultDatum != null) {
                vc.setValue(DATUM, defaultDatum);
            }

            Object oldParameterGroup;
            if (model.operationWrapper.hasParameters()) {
                oldParameterGroup = vc.getValue(PARAMETERS);
                ParameterValueGroup newParameters = model.operationWrapper.getParameter();
                if (oldParameterGroup instanceof ParameterValueGroup) {
                    ParameterValueGroup oldParameters = (ParameterValueGroup) oldParameterGroup;
                    List<GeneralParameterDescriptor> generalParameterDescriptors = newParameters.getDescriptor().descriptors();
                    List<GeneralParameterValue> oldValues = oldParameters.values();
                    for (GeneralParameterDescriptor newDescriptor : generalParameterDescriptors) {
                        String parameterName = newDescriptor.getName().getCode();
                        for (GeneralParameterValue oldParameterValue : oldValues) {
                            if (AbstractIdentifiedObject.nameMatches(oldParameterValue.getDescriptor(), newDescriptor)) {
                                Object old = ((ParameterValue)oldParameterValue).getValue();
                                newParameters.parameter(parameterName).setValue(old);
                            }
                        }
                    }
                }
                if (hasParameter(newParameters, SEMI_MAJOR_PARAM_NAME) && hasParameter(newParameters, SEMI_MINOR_PARAM_NAME)) {
                    Ellipsoid ellipsoid = model.datum.getEllipsoid();
                    ParameterValue<?> semiMajorParam = newParameters.parameter(SEMI_MAJOR_PARAM_NAME);
                    if (semiMajorParam.getValue() == null) {
                        semiMajorParam.setValue(ellipsoid.getSemiMajorAxis());
                    }
                    ParameterValue<?> semiMinorParam = newParameters.parameter(SEMI_MINOR_PARAM_NAME);
                    if (semiMinorParam.getValue() == null) {
                        semiMinorParam.setValue(ellipsoid.getSemiMinorAxis());
                    }
                }
                vc.setValue(PARAMETERS, newParameters);
            }
        }
        if (DATUM.equals(propertyName)) {
            if (model.datum != null && model.parameters != null && hasParameter(model.parameters, SEMI_MAJOR_PARAM_NAME) && hasParameter(model.parameters, SEMI_MINOR_PARAM_NAME)) {
                Ellipsoid ellipsoid = model.datum.getEllipsoid();
                model.parameters.parameter(SEMI_MAJOR_PARAM_NAME).setValue(ellipsoid.getSemiMajorAxis());
                model.parameters.parameter(SEMI_MINOR_PARAM_NAME).setValue(ellipsoid.getSemiMinorAxis());
            }
        }
        updateEnableState(true);
        firePropertyChange("crs", null, null);
    }

    private static boolean hasParameter(ParameterValueGroup parameterValueGroup, String name) {
        List<GeneralParameterDescriptor> generalParameterDescriptors = parameterValueGroup.getDescriptor().descriptors();
        for (GeneralParameterDescriptor descriptor : generalParameterDescriptors) {
             if (AbstractIdentifiedObject.nameMatches(descriptor, name)) {
                 return true;
             }
        }
        return false;
    }

    public void setCustom(GeodeticDatum geodeticDatum, OperationMethod operationMethod, ParameterValueGroup parameterValues) {
        String geodeticDatumName = geodeticDatum.getName().getCode();
        for (GeodeticDatum datum : datumSet) {
            if (datum.getName().getCode().equals(geodeticDatumName)) {
                vc.setValue(DATUM, datum);
                break;
            }
        }
        for (AbstractCrsProvider abstractCrsProvider : crsProviderSet) {
            if (abstractCrsProvider instanceof OperationMethodCrsProvider) {
                OperationMethodCrsProvider operationMethodCrsProvider = (OperationMethodCrsProvider) abstractCrsProvider;
                String operationMethodName = operationMethod.getName().getCode();
                if (operationMethodCrsProvider.delegate.getName().getCode().equals(operationMethodName)) {
                    vc.setValue(OPERATION_WRAPPER, abstractCrsProvider);
                    break;
                }
            }
        }
        vc.setValue(PARAMETERS, parameterValues);
    }

    private class UpdateListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateModel(evt.getPropertyName());
        }
    }

    public static void main(String[] args) {
        final JFrame frame = new JFrame("Projection Method Form Test");
        final CustomCrsPanel customCrsForm = new CustomCrsPanel(frame, CustomCrsPanel.createDatumSet(), CustomCrsPanel.createCrsProviderSet());
        frame.setContentPane(customCrsForm);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    private static Set<AbstractCrsProvider> createCrsProviderSet() {
        MathTransformFactory factory = ReferencingFactoryFinder.getMathTransformFactory(null);
        Set<OperationMethod> methods = factory.getAvailableMethods(Projection.class);

        TreeSet<AbstractCrsProvider> crsProviderSet = new TreeSet<>(new CrsProviderComparator());
        for (OperationMethod method : methods) {
            crsProviderSet.add(new OperationMethodCrsProvider(method));
        }

        return crsProviderSet;
    }

    private static Set<GeodeticDatum> createDatumSet() {
        DatumAuthorityFactory factory = ReferencingFactoryFinder.getDatumAuthorityFactory("EPSG", null);
        List<String> datumCodes = retrieveCodes(GeodeticDatum.class, factory);
        Set<GeodeticDatum> datumSet = new TreeSet<>(AbstractIdentifiedObject.NAME_COMPARATOR);
        for (String datumCode : datumCodes) {
            try {
                DefaultGeodeticDatum geodeticDatum = (DefaultGeodeticDatum) factory.createGeodeticDatum(datumCode);
                if (geodeticDatum.getBursaWolfParameters().length != 0 ||
                        DefaultGeodeticDatum.isWGS84(geodeticDatum)) {
                    datumSet.add(geodeticDatum);
                }
            } catch (FactoryException ignored) {
            }
        }
        return datumSet;
    }

    private static List<String> retrieveCodes(Class<? extends GeodeticDatum> crsType, AuthorityFactory factory) {
        try {
            Set<String> localCodes = factory.getAuthorityCodes(crsType);
            return new ArrayList<>(localCodes);
        } catch (FactoryException ignore) {
            return Collections.emptyList();
        }
    }

    private static PropertyContainer createValueContainer(ParameterValueGroup valueGroup) {
        final PropertyContainer vc = new PropertyContainer();

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
            final PropertyDescriptor vd = new PropertyDescriptor(paramName, valueType);
            final ParameterValue<?> parameterValue = valueGroup.parameter(paramName);
            if (parameterValue.getUnit() != null) {
                vd.setUnit(String.valueOf(parameterValue.getUnit()));
            }
            if (validValues != null) {
                vd.setValueSet(new ValueSet(validValues.toArray()));
            }

            vd.setDefaultConverter();
            final Property property = new Property(vd, new PropertyAccessor() {
                @Override
                public Object getValue() {
                    return parameterValue.getValue();
                }

                @Override
                public void setValue(Object value) {
                    parameterValue.setValue(value);
                }
            });
            vc.addProperty(property);
        }
        return vc;
    }

    private static class Model {

        private AbstractCrsProvider operationWrapper;
        private GeodeticDatum datum;
        @SuppressWarnings("UnusedDeclaration")
        private ParameterValueGroup parameters;
    }

    private class ParameterButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            final String operationName = model.operationWrapper.getName();
            final ModalDialog modalDialog = new ModalDialog(parent, operationName + " - Parameters",
                                                            ModalDialog.ID_OK_CANCEL, null);
            final ParameterValueGroup workCopy = model.parameters.clone();
            final PropertyContainer propertyContainer = createValueContainer(workCopy);
            modalDialog.setContent(new PropertyPane(propertyContainer).createPanel());
            if (modalDialog.show() == AbstractDialog.ID_OK) {
                vc.setValue(PARAMETERS, workCopy);
            }
        }
    }

    private static class CrsProviderComparator implements Comparator<AbstractCrsProvider> {

        @Override
        public int compare(AbstractCrsProvider o1, AbstractCrsProvider o2) {
            final String name1 = o1.getName();
            final String name2 = o2.getName();
            return name1.compareTo(name2);
        }
    }

    private static class IdentifiedObjectSearchable extends ComboBoxSearchable {

        private IdentifiedObjectSearchable(JComboBox<GeodeticDatum> operationComboBox) {
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

    private static class CrsProviderSearchable extends ComboBoxSearchable {

        private CrsProviderSearchable(JComboBox<AbstractCrsProvider> operationComboBox) {
            super(operationComboBox);
        }

        @Override
        protected String convertElementToString(Object o) {
            if (o instanceof AbstractCrsProvider) {
                AbstractCrsProvider wrapper = (AbstractCrsProvider) o;
                return wrapper.getName();
            } else {
                return super.convertElementToString(o);
            }
        }
    }
    private static class CrsProviderCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            final Component component = super.getListCellRendererComponent(list, value, index,
                                                                           isSelected, cellHasFocus);
            JLabel label = (JLabel) component;
            if (value != null) {
                AbstractCrsProvider wrapper = (AbstractCrsProvider) value;
                label.setText(wrapper.getName());
            }

            return label;
        }
    }

    private static class IdentifiedObjectCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            final Component component = super.getListCellRendererComponent(list, value, index,
                                                                           isSelected, cellHasFocus);
            JLabel label = (JLabel) component;
            if (value != null) {
                IdentifiedObject identifiedObject = (IdentifiedObject) value;
                label.setText(identifiedObject.getName().getCode());
            }

            return label;
        }
    }
}
