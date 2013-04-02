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

import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.BooleanEditor;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.util.Debug;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;

/**
 * This parameter page creates a simple UI.
 * All parameters are arranged as a list in the same order
 * as they were added to the {@link ParamGroup parameter group}.
 * <p/>
 * <b>Note: </b> Developers should derive this class and overwrite only
 * {@link ProcessingParameterPage#addParameterToPanel(org.esa.beam.framework.param.Parameter, javax.swing.JPanel, java.awt.GridBagConstraints)}
 * addParameterToPanel()} for their own UI implementation.
 *
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Norman Fomferra
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class ProcessingParameterPage extends ParameterPage {

    /**
     * The default title of this page.
     */
    public static final String DEFAULT_PAGE_TITLE = "Processing Parameters";

    /**
     * Creates a new instance of this class, with a {@link #DEFAULT_PAGE_TITLE default title}.
     *
     * @param paramGroup the parameter group an which the UI is build
     */
    public ProcessingParameterPage(final ParamGroup paramGroup) {
        this(paramGroup, DEFAULT_PAGE_TITLE);
    }

    /**
     * Creates a new instance of this class, with the given <code>pageTitle</code>.
     *
     * @param paramGroup the parameter group an which the UI is build
     * @param pageTitle  the title of this page
     */
    public ProcessingParameterPage(final ParamGroup paramGroup, final String pageTitle) {
        super(paramGroup);
        setTitle(pageTitle);
    }


    /**
     * Sets the parameter values by these given with the {@link Request request}.
     *
     * @param request the request to obtain the parameters
     *
     * @throws ProcessorException if an error occurred
     */
    @Override
    public void setUIFromRequest(final Request request) throws ProcessorException {
        final ParamGroup paramGroup = getParamGroup();
        final int numGroupParameters = paramGroup.getNumParameters();
        for (int i = 0; i < numGroupParameters; i++) {
            final Parameter groupParam = paramGroup.getParameterAt(i);
            final Parameter requestParam = request.getParameter(groupParam.getName());

            if (requestParam != null) {
                groupParam.setValueAsText(requestParam.getValueAsText(), null);
            } else {
                Debug.trace("UI parameter '" + groupParam.getName() + "' is not contained in request");
            }
        }
    }

    /**
     * Fills the given {@link Request request} with parameters.
     *
     * @param request the request to fill
     *
     * @throws ProcessorException if an error occurred
     */
    @Override
    public void initRequestFromUI(final Request request) throws ProcessorException {
        final ParamGroup paramGroup = getParamGroup();
        final int numGroupParameters = paramGroup.getNumParameters();
        for (int i = 0; i < numGroupParameters; i++) {
            request.addParameter(paramGroup.getParameterAt(i));
        }
    }

    /**
     * It creates the UI by using the {@link ParamGroup parameter group} of this page.
     * <p/>
     * <p>It's only called once by the {@link MultiPageProcessorUI} during lifetime of an
     * instance of this class.</p>
     *
     * @return the UI component displayed as page of the {@link MultiPageProcessorUI}.
     */
    @Override
    public JComponent createUI() {
        final ParamGroup paramGroup = getParamGroup();

        final JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("");
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets.bottom = 2;
        gbc.gridy = 0;

        final int numParams = paramGroup.getNumParameters();

        for (int i = 0; i < numParams; i++) {
            final Parameter parameter = paramGroup.getParameterAt(i);
            if (!parameter.getProperties().isHidden()) {
                addParameterToPanel(parameter, panel, gbc);
            }
        }

        final JPanel panel2 = new JPanel(new BorderLayout());
        panel2.add(panel, BorderLayout.NORTH);
        panel2.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        final JScrollPane scrollPane = new JScrollPane(panel2);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        return scrollPane;
    }

    /**
     * This method adds the UI representation of the given {@link Parameter parameter} to
     * the given {@link JPanel panel}.
     * <p/>
     * <b>Note: </b> This method is the only one developers have to overwrite to create their
     * own user interface for their parameters.
     *
     * @param parameter the parameter to add the UI component for
     * @param panel     the panel to add the UI component to
     * @param gbc       the constraints for the layout, are not changed between two calls but do have
     *                  meaningful settings at the first call
     */
    public void addParameterToPanel(final Parameter parameter, final JPanel panel, final GridBagConstraints gbc) {
        final boolean boolEditor = parameter.getEditor() instanceof BooleanEditor;
        if (boolEditor) {
            final JComponent editorComponent = parameter.getEditor().getEditorComponent();
            gbc.gridwidth = 3;
            gbc.gridx = 0;
            gbc.weightx = 1;
            gbc.insets.right = 0;
            panel.add(editorComponent, gbc);
            gbc.gridy++;
        } else {
            final JComponent editorComponent = parameter.getEditor().getEditorComponent();
            final JLabel labelComponent = parameter.getEditor().getLabelComponent();
            final JLabel unitComponent = parameter.getEditor().getPhysUnitLabelComponent();

            gbc.gridwidth = 1;

            if (labelComponent != null) {
                gbc.gridx = 0;
                gbc.weightx = 0.3;
                gbc.insets.right = 2;
                panel.add(labelComponent, gbc);
            }

            gbc.gridx = 1;
            gbc.weightx = 0.7;
            gbc.insets.right = 2;
            panel.add(editorComponent, gbc);

            if (unitComponent != null) {
                gbc.gridx = 2;
                gbc.weightx = 0;
                gbc.insets.right = 0;
                panel.add(unitComponent, gbc);
            }

            gbc.gridy++;
        }
    }

}
