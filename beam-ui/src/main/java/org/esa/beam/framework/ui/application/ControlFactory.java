package org.esa.beam.framework.ui.application;

import javax.swing.*;

/**
 * @author Marco Peters (original by Keith Donald of Spring RCP project)
 */
public interface ControlFactory {
    /**
     * Gets or creates (if not yet existing) the actual Swing UI control component.
     *
     * @return The page component.
     */
    JComponent getControl();
}