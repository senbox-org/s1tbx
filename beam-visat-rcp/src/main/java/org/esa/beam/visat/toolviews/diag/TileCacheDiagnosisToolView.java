package org.esa.beam.visat.toolviews.diag;

import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TileCacheDiagnosisToolView extends AbstractToolView {
    public static final String ID = TileCacheDiagnosisToolView.class.getName();
    private Timer timer;
    private TileCacheMonitor tileCacheMonitor;

    public TileCacheDiagnosisToolView() {
    }

    @Override
    protected JComponent createControl() {
        tileCacheMonitor = new TileCacheMonitor();
        JPanel panel = tileCacheMonitor.createPanel();
        timer = new Timer(2000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isVisible()) {
                    tileCacheMonitor.updateState();
                }
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
        timer.restart();
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
