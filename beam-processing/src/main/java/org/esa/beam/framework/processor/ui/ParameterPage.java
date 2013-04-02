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
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.util.Guardian;

import javax.swing.JComponent;

/**
 * This class serves as the base implementation of all derived parameter pages, which can be used
 * in the {@link MultiPageProcessorUI}.
 * <p/>
 * <p><b>Note:</b> Developers should derive {@link ProcessingParameterPage} and overwrite
 * {@link ProcessingParameterPage#addParameterToPanel(org.esa.beam.framework.param.Parameter, javax.swing.JPanel, java.awt.GridBagConstraints) addParameterToPanel()}
 * for their own UI implementation instead of deriving from this class.</p>
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Norman Fomferra
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public abstract class ParameterPage {

    private final ParamGroup _paramGroup;
    private ProcessorApp _app;
    private String _title;

    /**
     * Creates a new instance of this class.
     *
     * @param paramGroup the parameter group an which the UI is build
     */
    protected ParameterPage(final ParamGroup paramGroup) {
        Guardian.assertNotNull("paramGroup", paramGroup);
        _paramGroup = paramGroup;
    }

    /**
     * Sets the processor app for the UI.
     *
     * @param app the processor application
     */
    public void setApp(final ProcessorApp app) {
        _app = app;
        _app.markIODirChanges(_paramGroup);
    }

    /**
     * Gets the processor application associated with this parameter page.
     *
     * @return the processor application
     */
    public ProcessorApp getApp() {
        return _app;
    }

    /**
     * Gets the title of this page.
     * The title is used in the {@link MultiPageProcessorUI} as tab title.
     *
     * @return the title
     */
    public String getTitle() {
        return _title;
    }

    /**
     * Sets the title of this page.
     * The title is used in the {@link MultiPageProcessorUI} as tab title.
     *
     * @param title the title of this page
     */
    public void setTitle(final String title) {
        Guardian.assertNotNull("title", title);
        _title = title;
    }

    /**
     * Gets the {@link ParamGroup parameter group} of this page.
     *
     * @return the parameter group.
     */
    public ParamGroup getParamGroup() {
        return _paramGroup;
    }

    /**
     * Sets the UI to default values.<br/>
     * It is called by the {@link MultiPageProcessorUI#setDefaultRequests}.
     */
    public void setUIDefaults() {
        final ParamGroup paramGroup = getParamGroup();
        final int numGroupParameters = paramGroup.getNumParameters();
        for (int i = 0; i < numGroupParameters; i++) {
            paramGroup.getParameterAt(i).setDefaultValue();
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
    public abstract JComponent createUI();

    /**
     * Fills the given {@link Request request} with parameters.
     *
     * @param request the request to fill
     *
     * @throws ProcessorException if an error occurred
     */
    public abstract void initRequestFromUI(Request request) throws ProcessorException;

    /**
     * Sets the parameter values by these given with the {@link Request request}.
     *
     * @param request the request to obtain the parameters
     *
     * @throws ProcessorException if an error occurred
     */
    public abstract void setUIFromRequest(Request request) throws ProcessorException;
}
