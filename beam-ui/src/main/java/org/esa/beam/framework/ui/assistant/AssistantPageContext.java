/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.framework.ui.assistant;

import java.awt.Window;

/**
 * Instances of this interface provide the context for
 * implementations of {@link AssistantPage}.
 */
public interface AssistantPageContext {

    /**
     * The window in which the {@link AssistantPage} is shown.
     *
     * @return The window.
     */
    Window getWindow();

    /**
     * Gets the currently displayed {@link AssistantPage page}.
     *
     * @return The current page.
     */
    AssistantPage getCurrentPage();

    /**
     * Sets the currently displayed {@link AssistantPage page}.
     * Should only be called by the framwoerk.
     *
     * @param page The current page.
     */
    void setCurrentPage(AssistantPage page);

    /**
     * Forces an updated of the state.
     */
    void updateState();

    /**
     * Shows an error dialog with the given message.
     *
     * @param message The error message to display.
     */
    void showErrorDialog(String message);
}
