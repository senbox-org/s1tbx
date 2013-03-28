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
package org.esa.beam.framework.processor.ui;

import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Provides basic implementations for some {@link ProcessorUI} methods.
 * Also adds the {@link #showAboutBox(org.esa.beam.framework.processor.Processor, String) showAboutBox()} method to the
 * {@link ProcessorUI} interface.
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public abstract class AbstractProcessorUI implements ProcessorUI {
    private ProcessorApp _app;

    /**
     * Sets the processor application context.
     *
     * @see ProcessorUI#setApp(ProcessorApp)
     * @see #getApp
     */
    public void setApp(ProcessorApp app) {
        _app = app;
    }

    /**
     * Gets the processor application context.
     * @return the processor application context.
     */
    public ProcessorApp getApp() {
        return _app;
    }

    /**
     * Shows a simple "About" dialog box.
     * @param processor the processor
     * @param helpID the help ID, can be null
     */
    public void showAboutBox(final Processor processor, final String helpID) {
        final JPanel content = new JPanel(new GridLayout(4, 1));
        final JLabel nameLabel = new JLabel(getProcessorName(processor));
        final Font labelFont = nameLabel.getFont();
        if (labelFont != null) {
            nameLabel.setFont(labelFont.deriveFont(Font.BOLD, labelFont.getSize2D() + 2f));
        }
        content.add(nameLabel);
        content.add(new JLabel("  "));
        final String version = processor.getVersion();
        if (version != null) {
            content.add(new JLabel("Version " + version));
        }
        final String copyrightInformation = processor.getCopyrightInformation();
        if (copyrightInformation != null) {
            content.add(new JLabel(copyrightInformation));
        }
        final ModalDialog modalDialog = new ModalDialog(UIUtils.getRootWindow(getGuiComponent()),
                                                        "About - " + getProcessorName(processor),
                                                        ModalDialog.ID_OK,
                                                        helpID);
        modalDialog.setContent(content);
        modalDialog.show();
    }

    private String getProcessorName(final Processor processor) {
        String name = processor.getName();
        if (name == null) {
            name = processor.getClass().getName();
        }
        return name;
    }



}
