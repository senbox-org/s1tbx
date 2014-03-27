package org.esa.beam.visat.actions.imgfilter;

import org.esa.beam.visat.actions.imgfilter.model.Filter;

import javax.swing.*;

/**
 * @author Norman
 */
public class FilterKernelFormTest {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        final JFrame frame = new JFrame(FilterKernelForm.class.getSimpleName());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Filter filter1 = Filter.create(7);
        filter1.setEditable(true);
        frame.setContentPane(new FilterKernelForm(filter1));
        frame.pack();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
            }
        });
    }}
