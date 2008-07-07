package com.bc.ceres.binding.swing;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.ValueRange;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

public class BindingContextUI {

    String name;
    int age;
    double height;

    Gender gender;

    String country;
    boolean healthy;

    enum Gender {
        male,
        female
    }


    public BindingContextUI() {
        name = "";
        age = 20;
        height = 1.50;
        gender = Gender.female;
        country = "Germany";
        healthy = true;

        final ValueContainer vc = ValueContainer.createObjectBacked(this);

        vc.getModel("age").getDescriptor().setValueRange(new ValueRange(1, 150));
        vc.getModel("height").getDescriptor().setValueRange(new ValueRange(0.5, 2.5));
        vc.getModel("gender").getDescriptor().setValueSet(new ValueSet(new Object[] {Gender.female, Gender.male}));
        vc.getModel("country").getDescriptor().setValueSet(new ValueSet(new Object[] {"France", "Italy", "Spain", "Germany", "United Kingdom", "United States"}));

        final BindingContext ctx = new BindingContext(vc);

        ctx.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                System.out.println("'" + evt.getPropertyName() +
                        "' changed from '" + evt.getOldValue() +
                        "' to '" + evt.getNewValue() + "'");
            }
        });

        ctx.bind("name", new JTextField(16));
        ctx.bind("age", new JTextField(4));
        ctx.bind("height", new JTextField(7));
        final JRadioButton r1 = new JRadioButton("Female");
        final JRadioButton r2 = new JRadioButton("Male");
        final ButtonGroup bg = new ButtonGroup();
        bg.add(r1);
        bg.add(r2);
        ctx.bind("gender", bg);
        ctx.bind("country", new JComboBox());
        ctx.bind("healthy", new JCheckBox("Yes, I am not ill"));

        final GridBagConstraints gbc = new GridBagConstraints();
        final JPanel panel = new JPanel(new GridBagLayout());

        gbc.anchor = GridBagConstraints.BASELINE;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy=0;
        gbc.gridx=0;
        panel.add(new JLabel("Name: "), gbc);
        gbc.gridx=1;
        panel.add(getPrimaryComponent(ctx, "name"), gbc);

        gbc.gridy++;
        gbc.gridx=0;
        panel.add(r1, gbc);
        gbc.gridx=1;
        panel.add(r2, gbc);

        gbc.gridy++;
        gbc.gridx=0;
        panel.add(new JLabel("Age: "), gbc);
        gbc.gridx=1;
        panel.add(getPrimaryComponent(ctx, "age"), gbc);

        gbc.gridy++;
        gbc.gridx=0;
        panel.add(new JLabel("Height: "), gbc);
        gbc.gridx=1;
        panel.add(getPrimaryComponent(ctx, "height"), gbc);

        gbc.gridy++;
        gbc.gridx=0;
        panel.add(new JLabel("Country: "), gbc);
        gbc.gridx=1;
        panel.add(getPrimaryComponent(ctx, "country"), gbc);

        gbc.gridy++;
        gbc.gridx=0;
        gbc.gridwidth = 2;
        panel.add(getPrimaryComponent(ctx, "healthy"), gbc);


        final JFrame jFrame = new JFrame(getClass().getSimpleName());
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.add(panel);
        jFrame.pack();
        jFrame.setVisible(true);
    }

    private JComponent getPrimaryComponent(BindingContext ctx, String s) {
        return ctx.getBinding(s).getComponents()[0];
    }

    public static void main(String[] args) {
        new BindingContextUI();
    }
}
