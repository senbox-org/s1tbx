package org.esa.beam.visat.actions.imgfilter;

import org.esa.beam.visat.actions.imgfilter.model.Filter;

import javax.swing.*;


/**
 * @author Norman
 */
public class FilterWindowTest {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Filter filter = Filter.create(5);
                filter.setEditable(true);
                FilterWindow window = new FilterWindow(null);
                window.setFilter(filter);
                window.show();
            }
        });
    }}
