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

import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.util.Guardian;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;


/**
 * todo - Description of MultiPageProcessorUI
 * Description of MultiPageProcessorUI
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Norman Fomferra
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class MultiPageProcessorUI extends AbstractProcessorUI {

    private JTabbedPane _tabbedPane;
    private File _requestFile;
    private final String _requestType;
    private ArrayList<ParameterPage> _pageList;


    public MultiPageProcessorUI(final String requestType) {
        _requestType = requestType;
        _pageList = new ArrayList<ParameterPage>(2);
    }

    /**
     * Retrieves the base component for the processor specific user interface classes. This can be any Java Swing
     * containertype.
     */
    public JComponent getGuiComponent() {
         if (_tabbedPane == null) {
            createUI();
        }
        return _tabbedPane;
    }

    /**
     * Adds a {@link ParameterPage page} to this processor ui.
     *
     * @param page the {@link ParameterPage page}
     */
    public void addPage(final ParameterPage page) {
        _pageList.add(page);
    }

    /**
     * Retrieves the requests currently edited.
     *
     * @return a {@link Vector} of requests
     */
    public Vector getRequests() throws ProcessorException {
        final Vector<Request> requests = new Vector<Request>();
        requests.add(createRequest());
        return requests;
    }


    /**
     * Sets a new Request list to be edited.
     *
     * @param requests the request list to be edited must not be <code>null</code>.
     */
    public void setRequests(final Vector requests) throws ProcessorException {
        Guardian.assertNotNull("requests", requests);
        if (requests.size() > 0) {
            final Request request = (Request) requests.elementAt(0);
            _requestFile = request.getFile();
            for (final ParameterPage page : _pageList) {
                page.setUIFromRequest(request);
            }
        } else {
            setDefaultRequests();
        }
    }

    /**
     * Create a new default request for the sst processor and sets it to the UI
     */
    public void setDefaultRequests() throws ProcessorException {
        final Vector<Request> requests = new Vector<Request>();
        requests.add(createDefaultRequest());
        setRequests(requests);
    }

    /**
     * Sets the processor app for the UI
     *
     * @param app the {@link ProcessorApp processor app} associated with this ui
     */
    @Override
    public void setApp(final ProcessorApp app) {
        for (ParameterPage paramPage : _pageList) {
            paramPage.setApp(app);
        }
    }


    /**
     * Creates all user interface components of the sst user interface
     */
     private void createUI() {
        _tabbedPane = new JTabbedPane();
        for (final ParameterPage page : _pageList) {
            _tabbedPane.add(page.getTitle(), page.createUI());
        }

    }

    /**
     * Creates a request with all parameters set to their respective default values.
     *
     * @return the default request
     */
    private Request createDefaultRequest() throws ProcessorException {
        final Request request = new Request();
        for (final ParameterPage page : _pageList) {
            page.setUIDefaults();
            page.initRequestFromUI(request);
        }
        return request;
    }

    /**
     * Creates a request by using the current values of the UI components.
     *
     * @return the new request
     */
    private Request createRequest() throws ProcessorException {
        final Request request = new Request();
        request.setType(_requestType);
        request.setFile(_requestFile);
        for (final ParameterPage page : _pageList) {
            page.initRequestFromUI(request);
        }
        return request;
    }
}
