package org.esa.beam.visat.actions.imgfilter;

import org.esa.beam.visat.actions.imgfilter.model.Filter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

public class FilterWindow implements FilterEditor {

    private Window parentWindow;
    private JDialog dialog;
    private FilterKernelForm kernelForm;
    private Filter filter;
    private FilterPropertiesForm propertiesForm;
    private Preferences preferences;

    public FilterWindow(Window parentWindow) {
        this.parentWindow = parentWindow;
        this.preferences = Preferences.userRoot().node("beam").node("filterWindow");
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public void setFilter(Filter filter) {
        this.filter = filter;
        if (kernelForm != null) {
            kernelForm.setFilter(filter);
        }
        if (propertiesForm != null) {
            propertiesForm.setFilter(filter);
        }
    }

    @Override
    public void show() {
        if (dialog == null) {
            kernelForm = new FilterKernelForm(filter);
            propertiesForm = new FilterPropertiesForm(filter);
            dialog = new JDialog(parentWindow, "Filter", Dialog.ModalityType.MODELESS);
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.addTab("Filter Kernel", kernelForm);
            tabbedPane.addTab("Filter Properties", propertiesForm);
            dialog.setContentPane(tabbedPane);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    preferences.putInt("x", e.getWindow().getX());
                    preferences.putInt("y", e.getWindow().getY());
                    preferences.putInt("width", e.getWindow().getWidth());
                    preferences.putInt("height", e.getWindow().getHeight());
                }
            });
            Dimension preferredSize = dialog.getPreferredSize();
            int x = preferences.getInt("x", 100);
            int y = preferences.getInt("y", 100);
            int w = preferences.getInt("width", preferredSize.width);
            int h = preferences.getInt("height", preferredSize.height);
            dialog.setBounds(x, y, w, h);
        }
        dialog.setVisible(true);
    }

    @Override
    public void hide() {
        if (dialog != null) {
            dialog.setVisible(false);
        }
    }

}
