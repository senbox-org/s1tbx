package org.esa.beam.framework.ui;

import org.esa.beam.framework.ui.command.CommandUIFactory;

public interface CommandUIFactoryAware {
    /**
     * Gets the command UI factory used to create the context dependent menu items for the context menu associated with
     * this view.
     *
     * @return the command UI factory
     */
    CommandUIFactory getCommandUIFactory();

    /**
     * Sets the command UI factory used to create the context dependent menu items for the context menu associated with
     * this view.
     *
     * @param commandUIFactory the command UI factory
     */
    void setCommandUIFactory(CommandUIFactory commandUIFactory);
}
