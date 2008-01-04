package org.esa.beam.framework.ui.application;

import org.esa.beam.framework.ui.command.Command;

import javax.swing.*;
import java.awt.*;


/**
 * Metadata about a view; a view descriptor is effectively a singleton view
 * definition. A descriptor also acts as a factory which produces new instances
 * of a given view when requested, typically by a requesting application page. A
 * view descriptor can also produce a command which launches a view for display
 * on the page within the current active window.
 *
 * @author Marco Peters (original by Keith Donald of Spring RCP project)
 */
public interface DocViewDescriptor extends PageComponentDescriptor {

    /**
     * Create a command that when executed, will attempt to open the
     * page component described by this descriptor in the provided
     * application window.
     *
     * @param window The window
     * @return The show page component command.
     */
    public Command createOpenViewCommand(ApplicationWindow window);
}