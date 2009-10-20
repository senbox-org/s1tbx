package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.binding.ValueAccessor;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.swing.TableLayout;
import com.jidesoft.swing.ComboBoxSearchable;
import com.jidesoft.swing.SearchableUtils;
import org.esa.beam.framework.ui.AbstractDialog;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.ValueEditorsPane;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Projection;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
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

    private final List<GeodeticDatum> datumList;
    private final List<OperationMethod> operationMethodList;
    private ProjectionDefinitionForm.Model model;
    private ValueContainer vc;
    private final Window parent;
    private JComboBox operationComboBox;
    private JComboBox datumComboBox;
    private JButton paramButton;

    public ProjectionDefinitionForm(Window parent, List<OperationMethod> operationMethodList,
                                    List<GeodeticDatum> datumList) {
        this.parent = parent;
        this.operationMethodList = new ArrayList<OperationMethod>(operationMethodList);
        this.datumList = new ArrayList<GeodeticDatum>(datumList);
        Collections.sort(this.operationMethodList, new IdentifiedObjectComparator());
        Collections.sort(this.datumList, new IdentifiedObjectComparator());

        model = new Model();
        // todo - set Lat/Lon WGS84
        model.transformation = this.operationMethodList.get(0);
        model.datum = this.datumList.get(0);
        updateModel(model);

        vc = ValueContainer.createObjectBacked(model);
        vc.addPropertyChangeListener(new UpdateListener());

        creatUI();
    }

    public ProjectedCRS getProcjetedCRS() throws FactoryException {
        final CRSFactory crsFactory = ReferencingFactoryFinder.getCRSFactory(null);
        final CoordinateOperationFactory coFactory = ReferencingFactoryFinder.getCoordinateOperationFactory(null);

        final HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", model.transformation.getName().getCode() + " / " + model.datum.getName().getCode());
        final Conversion conversion = coFactory.createDefiningConversion(properties,
                                                                         model.transformation,
                                                                         model.parameters);
        return crsFactory.createProjectedCRS(properties, DefaultGeographicCRS.WGS84,
                                             conversion, DefaultCartesianCS.PROJECTED);
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

        operationComboBox = new JComboBox(operationMethodList.toArray());
        operationComboBox.setEditable(false); // combobox searchable only works when combobox is not editable.
        final ComboBoxSearchable methodSearchable = new IdentifiedObjectSearchable(operationComboBox);
        methodSearchable.installListeners();
        operationComboBox.setRenderer(new IdentifiedObjectCellRenderer());

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
        add(tableLayout.createVerticalSpacer());
        addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                operationComboBox.setEnabled((Boolean) evt.getNewValue());
                datumComboBox.setEnabled((Boolean) evt.getNewValue());
                paramButton.setEnabled((Boolean) evt.getNewValue());
            }
        });
        final BindingContext context = new BindingContext(vc);
        context.bind("transformation", operationComboBox);
        context.bind("datum", datumComboBox);

    }

    private void updateModel(final Model model) {
        model.parameters = model.transformation.getParameters().createValue();
        if (model.datum != null) {
            model.parameters.parameter("semi_major").setValue(model.datum.getEllipsoid().getSemiMajorAxis());
            model.parameters.parameter("semi_minor").setValue(model.datum.getEllipsoid().getSemiMinorAxis());
        }
    }

    private class UpdateListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("parameters".equals(evt.getPropertyName())) {
                return;
            }
            updateModel(model);
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
            if (descriptor instanceof ParameterDescriptor) {
                ParameterDescriptor parameterDescriptor = (ParameterDescriptor) descriptor;
                valueType = parameterDescriptor.getValueClass();
            } else {
                valueType = Double.TYPE;
            }

            final String paramName = descriptor.getName().getCode();
            final ValueDescriptor vd = new ValueDescriptor(paramName, valueType);
            final ParameterValue<?> parameterValue = valueGroup.parameter(paramName);
            if (parameterValue.getUnit() != null) {
                vd.setUnit(String.valueOf(parameterValue.getUnit()));
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

        private OperationMethod transformation;
        private GeodeticDatum datum;
        private ParameterValueGroup parameters;

    }

    private class ParameterButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            final String operationName = model.transformation.getName().getCode();
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
}
