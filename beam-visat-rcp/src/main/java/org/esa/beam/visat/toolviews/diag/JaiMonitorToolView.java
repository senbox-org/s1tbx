package org.esa.beam.visat.toolviews.diag;

import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class JaiMonitorToolView extends AbstractToolView {
    private Timer timer;
    private JaiMonitor jaiMonitor;

    public JaiMonitorToolView() {
    }

    protected JComponent createControl() {
        jaiMonitor = new JaiMonitor();
        JPanel panel = jaiMonitor.createPanel();
        timer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jaiMonitor.updateState();
            }
        });
        timer.setRepeats(true);
        timer.start();
        return panel;
    }

    /**
     * The default implementation does nothing.
     * <p>Clients shall not call this method directly.</p>
     */
    @Override
    public void componentOpened() {
        timer.start();
    }

    /**
     * The default implementation does nothing.
     * <p>Clients shall not call this method directly.</p>
     */
    @Override
    public void componentClosed() {
        timer.stop();
    }
}
