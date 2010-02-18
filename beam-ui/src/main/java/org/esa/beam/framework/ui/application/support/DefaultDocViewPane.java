package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.PageComponent;

import javax.swing.*;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;

/**
 * Uses a {@link JInternalFrame} as control.
 */
public class DefaultDocViewPane extends AbstractPageComponentPane {
    private JInternalFrame internalFrame;

    public DefaultDocViewPane(PageComponent pageComponent) {
        super(pageComponent);
    }

    @Override
    protected JComponent createControl() {
        JComponent pageComponentControl = getPageComponent().getControl();
        if (pageComponentControl.getName() == null) {
            nameComponent(pageComponentControl, "Control");
        }
        internalFrame = new JInternalFrame();
        configureControl();
        internalFrame.getContentPane().add(pageComponentControl, BorderLayout.CENTER);
        internalFrame.addInternalFrameListener(new InternalFrameHandler());
        nameComponent(internalFrame, "Pane");
        return internalFrame;
    }

    @Override
    protected void pageComponentChanged(PropertyChangeEvent evt) {
        configureControl();
    }

    private void configureControl() {
        internalFrame.setTitle(getPageComponent().getTitle());
        internalFrame.setFrameIcon(getPageComponent().getIcon());
    }

    private static class InternalFrameHandler implements InternalFrameListener {
        public void internalFrameOpened(InternalFrameEvent e) {
            // todo
        }

        public void internalFrameClosing(InternalFrameEvent e) {
            // todo
        }

        public void internalFrameClosed(InternalFrameEvent e) {
            // todo
        }

        public void internalFrameIconified(InternalFrameEvent e) {
            // todo
        }

        public void internalFrameDeiconified(InternalFrameEvent e) {
            // todo
        }

        public void internalFrameActivated(InternalFrameEvent e) {
            // todo
        }

        public void internalFrameDeactivated(InternalFrameEvent e) {
            // todo
        }
    }
}
