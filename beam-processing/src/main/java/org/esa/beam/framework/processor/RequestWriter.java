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
package org.esa.beam.framework.processor;

import org.esa.beam.framework.param.Parameter;
import org.esa.beam.util.Guardian;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * The request writer is a helper class that can write any number of <code>Requests</code> to an xml file so that the
 * request form a valid "Request file" that can be reread by the <code>RequestLoader</code>.
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class RequestWriter {

    /**
     * Constructs the object with default values.
     */
    public RequestWriter() {
    }

    /**
     * Writes the request passed in to the File supplied.
     *
     * @throws IllegalArgumentException when fed with <code>null</code>
     */
    public void write(final Request[] requests, final File requestFile) throws IOException {
        Guardian.assertNotNull("requests", requests);
        Guardian.assertNotNull("requstFile", requestFile);

        // create the appropriate writer
        ensureRequestFileExists(requestFile);
        final Writer writer = createWriter(requestFile);

        write(requests, writer);
    }

    /**
     * Writes the request passed in to the Writer supplied.
     *
     * @throws IllegalArgumentException when fed with <code>null</code>
     */
    public void write(final Request[] requests, final Writer writer) throws IOException {
        // create the DOM document
        Document dom = createDOM(requests);
        final Format prettyFormat = Format.getPrettyFormat();

        // following lines uses the old JDOM jar
//        xmlOut.setIndentSize(4);
//        xmlOut.setNewlines(true);
//        xmlOut.setEncoding("ISO-8859-1");
//        xmlOut.setLineSeparator(System.getProperty("line.separator"));
        prettyFormat.setEncoding("ISO-8859-1");
        prettyFormat.setIndent("    ");
        XMLOutputter xmlOut = new XMLOutputter(prettyFormat);

        // write out
        xmlOut.output(dom, writer);

        // and close finally
        close(writer);
    }

    /**
     * Closes the file. Sets the writer reference to <code>null</code>.
     */
    public void close(final Writer writer) throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PACKAGE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Checks whether the request file exists and if not, creates one.
     *
     * @param requestFile the <code>File</code> describing the request file
     */
    private void ensureRequestFileExists(File requestFile) throws IOException {
        // check whether the file exists. if not create
        if (!requestFile.exists()) {
            File parentFile = requestFile.getParentFile();
            if (parentFile != null) {
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
            }
            requestFile.createNewFile();
        }
    }

    /**
     * Creates the trace writer used to access the file
     *
     * @param requestFile the <code>File</code> to be written
     */
    private Writer createWriter(File requestFile) throws FileNotFoundException,
                                                       IOException {
        return new FileWriter(requestFile);
    }

    /**
     * Creates a DOM representation of the request passed in.
     *
     * @param requests the request to be represented
     */
    private Document createDOM(Request[] requests) {
        Element root = new Element(RequestTags.TAG_REQUEST_LIST);

        for (Request request : requests) {
            addRequestToDOM(root, request);
        }

        return new Document(root);
    }

    /**
     * Adds a request to the root element.
     *
     * @param root the Element getting a request added
     */
    private void addRequestToDOM(Element root, Request request) {
        Element reqElem = new Element(RequestTags.TAG_REQUEST);
        String reqType = request.getType();

        // check if request type should be set
        if ((reqType != null) && (reqType.length() > 0)) {
            reqElem.setAttribute(RequestTags.ATTRIB_TYPE, reqType);
        }

        addParameter(reqElem, request);
        addLogFileLocations(reqElem, request);
        addInputProducts(reqElem, request);
        addOutputProducts(reqElem, request);

        root.addContent(reqElem);
    }


    /**
     * Adds all parameters of the request to the element passed in
     *
     * @param reqElem the element where things get added
     * @param request the request containing the parameter
     */
    private void addParameter(Element reqElem, Request request) {
        Element paramElem;
        Parameter param;

        for (int n = 0; n < request.getNumParameters(); n++) {
            param = request.getParameterAt(n);
            paramElem = new Element(RequestTags.TAG_PARAMETER);
            paramElem.setAttribute(RequestTags.ATTRIB_NAME, param.getName());
            paramElem.setAttribute(RequestTags.ATTRIB_VALUE, param.getValueAsText());
            final Map properties = param.getProperties().getProperties(Request.PREFIX_QUALIFIER);
            final Set propertiesSet = properties.entrySet();
            for (Object element : propertiesSet) {
                Map.Entry entry = (Map.Entry) element;
                final String name = ((String) entry.getKey()).substring(Request.PREFIX_QUALIFIER.length());
                final String value = (String) entry.getValue();
                paramElem.setAttribute(name, value);
            }
            reqElem.addContent(paramElem);
        }
    }

    /**
     * Adds all logging file locations of the request to the element passed in
     *
     * @param reqElem the element where things get added
     * @param request the request containing the parameter
     */
    private void addLogFileLocations(Element reqElem, Request request) {
        for (int n = 0; n < request.getNumLogFileLocations(); n++) {
            Element logElem = new Element(RequestTags.TAG_LOG_LOCATION);
            logElem.setAttribute(RequestTags.ATTRIB_FILE, request.getLogFileLocationAt(n).toString());
            reqElem.addContent(logElem);
        }
    }

    /**
     * Adds all input products of the request to the element passed in
     *
     * @param reqElem the element where things get added
     * @param request the request containing the parameter
     */
    private void addInputProducts(Element reqElem, Request request) {
        for (int n = 0; n < request.getNumInputProducts(); n++) {
            ProductRef inputProd = request.getInputProductAt(n);
            Element inputElem = new Element(RequestTags.TAG_INPUT_PRODUCT);
            inputElem.setAttribute(RequestTags.ATTRIB_FILE, inputProd.getFile().getPath());
            setNotEmptyValueAttrib(RequestTags.ATTRIB_FILE_FORMAT, inputProd.getFileFormat(), inputElem);
            setNotEmptyValueAttrib(RequestTags.ATTRIB_FILE_TYPE_ID, inputProd.getTypeId(), inputElem);
            reqElem.addContent(inputElem);
        }
    }

    /**
     * Adds all output products of the request to the element passed in
     *
     * @param reqElem the element where things get added
     * @param request the request containing the parameter
     */
    private void addOutputProducts(Element reqElem, Request request) {
        for (int n = 0; n < request.getNumOutputProducts(); n++) {
            ProductRef outputProd = request.getOutputProductAt(n);
            Element outputElem = new Element(RequestTags.TAG_OUTPUT_PRODUCT);
            outputElem.setAttribute(RequestTags.ATTRIB_FILE, outputProd.getFile().getPath());
            setNotEmptyValueAttrib(RequestTags.ATTRIB_FILE_FORMAT, outputProd.getFileFormat(), outputElem);
            setNotEmptyValueAttrib(RequestTags.ATTRIB_FILE_TYPE_ID, outputProd.getTypeId(), outputElem);
            reqElem.addContent(outputElem);
        }
    }

    /**
     * If the given attribute and the given value are not null an not empty, this method sets an attribute with the
     * given name in the given element. Otherwise does nothing.
     *
     * @throws IllegalArgumentException if the element is null.
     */
    private void setNotEmptyValueAttrib(final String attributeName, final String attributeValue, Element element) {
        if (attributeName == null || attributeName.length() == 0) {
            return;
        }
        if (attributeValue == null || attributeValue.length() == 0) {
            return;
        }
        Guardian.assertNotNull("element", element);
        element.setAttribute(attributeName, attributeValue);
    }

}
