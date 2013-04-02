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

import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.logging.BeamLogManager;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

/**
 * This class is responsible for loading request files. It first parses the xml code, creates a <code>Request</code>
 * object and (optionally) verifies that all parameters in the request file are within predefined ranges.
 * <p/>
 * To enable the request loader to verify parameter ranges, each user has to supply a custom
 * <code>RequestElemetFactory</code> to the request loader. Initially all parameters in the request are assumed having
 * <code>String</code> data type and can contain anything.
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class RequestLoader {

    private File _file;
    private Vector<Request> _requests;
    private Request _currentRequest;
    private RequestElementFactory _elemFactory;

    /**
     * Constructs the object with default parameters.
     */
    public RequestLoader() {
        init();
    }

    /**
     * Constructs the object with a given request file.
     *
     * @param requestFile the XML file containing the requestlist
     *
     * @throws RequestElementFactoryException
     */
    public RequestLoader(final File requestFile) throws RequestElementFactoryException {
        init();
        setAndParseRequestFile(requestFile);
    }

    /**
     * Sets the request file to be parsed and parses it if possible
     *
     * @param requestFile the XML file containing the requestlist
     *
     * @throws RequestElementFactoryException
     */
    public void setAndParseRequestFile(final File requestFile) throws RequestElementFactoryException {
        setAndCheckRequestFile(requestFile);

        // clear vector of requests
        _requests.clear();

        try {
            parse();
        } catch (ParserConfigurationException e) {
            throw createREFException("unable to create appropriate XML parser - ", e);
        } catch (SAXException e) {
            throw createREFException("unable to parse XML file - ", e);
        } catch (IOException e) {
            throw createREFException("unable to read XML file ", e);
        } catch (IllegalArgumentException e) {
            throw createREFException("", e);
        }
    }

    private static RequestElementFactoryException createREFException(final String msgPrefix, final Exception e) {
        final RequestElementFactoryException exc = new RequestElementFactoryException(msgPrefix + e.getMessage());
        exc.initCause(e);
        return exc;
    }

    /**
     * Sets the element factory to be used.
     *
     * @param factory the request element factory to be used
     */
    public void setElementFactory(final RequestElementFactory factory) {
        _elemFactory = factory;
    }

    /**
     * Returns the number of requests parsed.
     */
    public int getNumRequests() {
        return _requests.size();
    }

    /**
     * Returns the request at the given index.
     *
     * @param index the index of the request to be returned
     *
     * @throws ArrayIndexOutOfBoundsException
     */
    public Request getRequestAt(final int index) {
        return _requests.elementAt(index);
    }

    /**
     * Returns all requests contained in the request file parsed
     */
    public Vector<Request> getAllRequests() {
        return _requests;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Initializes the object;
     */
    private void init() {
        _requests = new Vector<Request>();
        _currentRequest = null;
        _elemFactory = null;
    }
    /**
     * Checks whether the <code>URL</code> passed in contains a valid file and can be opened for reading. If so, the
     * <code>File</code> is set.
     */
    private void setAndCheckRequestFile(final File requestFile) throws RequestElementFactoryException {
        Guardian.assertNotNull("requestFile", requestFile);
        _file = requestFile;
        if ((!_file.exists()) || (!_file.isFile())) {
            _file = null;
            throw new RequestElementFactoryException(
                    "Unable to open processing request file '" + requestFile + "'.");
        }
    }

    /**
     * Parses the the XML file containing the requestlist.
     *
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private void parse() throws ParserConfigurationException,
                                SAXException,
                                IOException {
        // Use the default (non-validating) parser
        final SAXParserFactory factory = SAXParserFactory.newInstance();

        // Parse the input
        final SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(_file, new RequestElementHandler());
    }

    /**
     * Creates or configures a new request element.
     *
     * @param qName        qualified name
     * @param attrs        the element attributes
     */
    private void maybeCreateOrConfigureRequest(final String qName, final Attributes attrs) throws ProcessorException {
        if (qName.equalsIgnoreCase(RequestTags.TAG_REQUEST)) {
            startRequest(attrs);
            return;
        }

        if (qName.equalsIgnoreCase(RequestTags.TAG_INPUT_PRODUCT)) {
            startInputProduct(attrs);
        }

        if (qName.equalsIgnoreCase(RequestTags.TAG_OUTPUT_PRODUCT)) {
            startOutputProduct(attrs);
        }

        if (qName.equalsIgnoreCase(RequestTags.TAG_PARAMETER)) {
            startParameter(attrs);
        }

        if (qName.equalsIgnoreCase(RequestTags.TAG_LOG_LOCATION)) {
            startLogFileLocation(attrs);
        }
    }

    /**
     * Finishes a complete request element.
     *
     * @param qName        qualified name
     */
    private void maybeFinishRequest(final String qName) {
        if (qName.equalsIgnoreCase(RequestTags.TAG_REQUEST)) {
            finishRequest();
        }
    }

    /**
     * Starts to construct a request.
     *
     * @param attrs the attributes of the request
     */
    private void startRequest(final Attributes attrs) {
        if (_currentRequest != null) {
            // throw new IllegalStateException("_currentRequest != null");
        }
        _currentRequest = new Request();
        _currentRequest.setFile(_file);

        for (int n = 0; n < attrs.getLength(); n++) {
            if (attrs.getQName(n).equalsIgnoreCase(RequestTags.ATTRIB_TYPE)) {
                _currentRequest.setType(attrs.getValue(n));
            } else {
                _currentRequest.addParameter(new Parameter(attrs.getQName(n), attrs.getValue(n)));
            }
        }
    }

    /**
     * Finishes a requests. Adds it to the list of requests parsed yet.
     */
    private void finishRequest() {
        if (_currentRequest != null) {
            _requests.addElement(_currentRequest);
            _currentRequest = null;
        }
    }

    /**
     * Adds an input product to the request currently parsed
     *
     * @param attrs the attributes of the input product
     */
    private void startInputProduct(final Attributes attrs) throws RequestElementFactoryException {
        if (_currentRequest != null) {

            final ProdRefValues values = computeValues(attrs);

            // check if we have a factory to create the product or have to
            // do it manually
            if (values.file != null) {
                final ProductRef prod;
                if (_elemFactory != null) {
                    prod = _elemFactory.createInputProductRef(values.file, values.format, values.typeId);
                } else {
                    prod = new ProductRef(values.file, values.format, values.typeId);
                }
                _currentRequest.addInputProduct(prod);
            }
        }
    }

    /**
     * Adds an output product to the request currently parsed
     *
     * @param attrs the attributes of the input product
     */
    private void startOutputProduct(final Attributes attrs) throws RequestElementFactoryException {
        if (_currentRequest != null) {

            final ProdRefValues values = computeValues(attrs);

            // check if we have a factory to create the product or have to
            // do it manually
            if (values.file != null) {
                final ProductRef prod;
                if (_elemFactory != null) {
                    prod = _elemFactory.createOutputProductRef(values.file, values.format, values.typeId);
                } else {
                    prod = new ProductRef(values.file, values.format, values.typeId);
                }
                _currentRequest.addOutputProduct(prod);
            }
        }
    }

    /**
     * Adds a logging file location to the request currently parsed.
     *
     * @param attrs the attributes of the logging file location parameter
     */
    private void startLogFileLocation(final Attributes attrs) {
        if (_currentRequest != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                final String qname = attrs.getQName(i);
                if (qname.equalsIgnoreCase(RequestTags.ATTRIB_URL)) {
                    File file = createFileFromURL(attrs.getValue(i));
                    _currentRequest.addLogFileLocation(file);
                } else if (qname.equalsIgnoreCase(RequestTags.ATTRIB_FILE)
                        || qname.equalsIgnoreCase(RequestTags.ATTRIB_PATH)) {
                    File file = new File(attrs.getValue(i));
                    _currentRequest.addLogFileLocation(file);
                }
            }
        }
    }

    /**
     * Adds a parameter to the request currently parsed
     */
    private void startParameter(final Attributes attrs) throws ProcessorException {
        if (_currentRequest != null) {
            String name = null;
            String value = null;

            final HashMap<String,String> attribsMap = new HashMap<String, String>();
            for (int n = 0; n < attrs.getLength(); n++) {
                // get the parameter name - is an attribute named "name"
                final String qName = attrs.getQName(n);
                final String avalue = attrs.getValue(n);
                if (qName.equalsIgnoreCase(RequestTags.ATTRIB_NAME)) {
                    name = avalue;
                    continue;
                }

                // get the parameter value - is an attribute named "value"
                if (qName.equalsIgnoreCase(RequestTags.ATTRIB_VALUE)) {
                    value = avalue;
                    continue;
                }
                attribsMap.put(qName, avalue);
            }

            // set the parameter if we got both name and value. Reset name and value
            if (name != null && value != null) {
                Parameter param = null;
                if (_elemFactory != null) {
                    if (name.equalsIgnoreCase(ProcessorConstants.LOG_PREFIX_PARAM_NAME)) {
                        param = _elemFactory.createDefaultLogPatternParameter(value);
                    } else if (name.equalsIgnoreCase(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME)) {
                        try {
                            param = _elemFactory.createLogToOutputParameter(value);
                        } catch (ParamValidateException e) {
                            throw new RequestElementFactoryException(e.getMessage());
                        }
                    } else {
                        try {
                            param = _elemFactory.createParameter(name, value);
                        } catch (RequestElementFactoryException e) {
                            BeamLogManager.getSystemLogger().log(Level.WARNING, e.getMessage());
                            return;
                        }
                    }
                } else {
                    param = new Parameter(name, value);
                }

                if (attribsMap.size() > 0) {
                    final Set<Map.Entry<String,String>> set = attribsMap.entrySet();
                    for (Iterator<Map.Entry<String,String>> iterator = set.iterator(); iterator.hasNext();) {
                        final Map.Entry<String,String> entry = iterator.next();
                        final String key = entry.getKey();
                        final String sValue = entry.getValue();
                        param.getProperties().setPropertyValue(Request.PREFIX_QUALIFIER + key, sValue);
                    }
                }
                _currentRequest.addParameter(param);
            } else {
                // error in xml file
                String msg = ProcessorConstants.LOG_MSG_REQUEST_MALFORMED;
                if (name != null) {
                    msg += "\nParameter name: '" + name + "'";
                }
                if (value != null) {
                    msg += "\nParameter value: '" + value + "'";
                }
                throw new RequestElementFactoryException(msg);
            }
        }
    }

    /**
     * Creates an URL from the string passed in.
     *
     * @param urlString the string to be converted to a file
     */
    private static File createFileFromURL(String urlString) {
        if (urlString.startsWith("file:")) {
            if(!urlString.startsWith("file:/")) {
                urlString = urlString.replace("file:", "file:/");
            }
            try {
                return new File(new URI(urlString));
            } catch (URISyntaxException e) {
                try {
                    return new File(new URL(urlString).toURI());
                } catch (URISyntaxException e1) {
                    // ignore
                } catch (MalformedURLException e1) {
                    // ignore
                }
                return new File(urlString.substring("file:".length()));
            }
        } else {
            return new File(urlString);
        }
    }

    /**
     * Callback class for XML parser.
     */
    class RequestElementHandler extends DefaultHandler {

        RequestElementHandler() {
        }

        /**
         * Callback from parser. Is invoked each time the parser encounters a new element.
         *
         * @param nameSpaceURI
         * @param sName        simple (local) name
         * @param qName        qualified name
         * @param attrs        the element attributes
         */
        @Override
        public void startElement(final String nameSpaceURI, final String sName, final String qName,
                                 final Attributes attrs) throws SAXException {
            try {
                maybeCreateOrConfigureRequest(qName, attrs);
            } catch (ProcessorException e) {
                throw new SAXException(e.getMessage());
            }
        }

        /**
         * Callback from parser. Is invoked each time the parser finishes an element.
         *
         * @param nameSpaceURI
         * @param sName        simple (local) name
         * @param qName        qualified name
         */
        @Override
        public void endElement(final String nameSpaceURI, final String sName, final String qName) throws SAXException {
            maybeFinishRequest(qName);
        }
    }

    private class ProdRefValues {
        File file = null;
        String format = "";
        String typeId = "";
    }

    private ProdRefValues computeValues(Attributes attrs) {
        final ProdRefValues values = new ProdRefValues();

        for (int n = 0; n < attrs.getLength(); n++) {
            final String qname = attrs.getQName(n);

            // set the url for the file. If the attribute is not an URL,
            // an URL of type "file" is created
            if (qname.equalsIgnoreCase(RequestTags.ATTRIB_URL)) {
                values.file = createFileFromURL(attrs.getValue(n));
            } else if (qname.equalsIgnoreCase(RequestTags.ATTRIB_FILE)
                    || qname.equalsIgnoreCase(RequestTags.ATTRIB_PATH)) {
                values.file = new File(attrs.getValue(n));
            } else if (qname.equalsIgnoreCase(RequestTags.ATTRIB_FILE_FORMAT)) {
                values.format = attrs.getValue(n);
            } else if (qname.equalsIgnoreCase(RequestTags.ATTRIB_FILE_TYPE_ID)) {
                values.typeId = attrs.getValue(n);
            }
        }
        return values;
    }
}
