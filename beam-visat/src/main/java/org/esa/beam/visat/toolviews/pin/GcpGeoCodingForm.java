package org.esa.beam.visat.toolviews.pin;

import com.bc.ceres.binding.Factory;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDefinition;
import com.bc.ceres.binding.ValueDefinitionFactory;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.SwingBindingContext;
import com.jidesoft.dialog.JideOptionPane;
import org.esa.beam.framework.ui.TableLayout;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class GcpGeoCodingForm extends JPanel {

    private GcpGeoCodingFormModel formModel;
    private JComboBox transformationTypeComboBox;
    private JTextField rmseTextField;
    private JButton applyButton;


    public GcpGeoCodingForm() {
        formModel = new GcpGeoCodingFormModel();
        initComponents();
        bindComponents();
    }

    private void initComponents() {
        transformationTypeComboBox = new JComboBox();
        rmseTextField = new JTextField();
        applyButton = new JButton("Apply");
        applyButton.setName("applyButton");
        AbstractAction applyAction = new AbstractAction("Apply") {
            public void actionPerformed(ActionEvent e) {
                JideOptionPane.showConfirmDialog(getParent(), "Not Implemented!");
            }
        };
        applyButton.setAction(applyAction);

        setBorder(BorderFactory.createTitledBorder("GCP Geo-Coding"));

        TableLayout layout = new TableLayout(5);
        this.setLayout(layout);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableWeightX(0.0);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTablePadding(2,2);

        layout.setCellPadding(0, 1, new Insets(2,2,2,10));
        layout.setCellWeightX(0, 1, 0.8);

        layout.setCellPadding(0, 3, new Insets(2,2,2,10));
        layout.setCellWeightX(0, 3, 0.6);

        layout.setCellWeightX(0, 4, 0.6);
        layout.setCellAnchor(0, 4, TableLayout.Anchor.EAST);
        layout.setCellFill(0, 4, TableLayout.Fill.VERTICAL);

        add(new JLabel("Transformation:"));
        add(transformationTypeComboBox);
        add(new JLabel("RMSE:"));
        add(rmseTextField);
        add(applyButton);
    }

    private void bindComponents() {

        // todo - make this a SimpleSwingDefinitionFactory class
        // todo - in package com.bc.ceres.binding.swing or something
        ValueDefinitionFactory valueDefinitionFactory = new ValueDefinitionFactory() {

            public ValueDefinition createValueDefinition(Field field) {
                Class<?> type = field.getType();
                ValueDefinition valueDefinition = new ValueDefinition(field.getName(), type);
                if(type.isEnum()) {
                    valueDefinition.setValueSet(new ValueSet(type.getEnumConstants()));
                }
                return valueDefinition;
            }
        };
        Factory factory = new Factory(valueDefinitionFactory);

        ValueContainer valueContainer = factory.createObjectBackedValueContainer(formModel);
        SwingBindingContext binding = new SwingBindingContext(valueContainer);

        binding.bind(transformationTypeComboBox, "transformationType");
        binding.bind(rmseTextField, "geoCodingRmse");


    }

}
