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

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * The request class is a memory image of a single request in a request file. It is used to trigger processors and
 * supply the information neede to perform the processing.
 * <p/>
 * A request can contain the following entities: <ul> <li>any number of input products</li> <li>any number of output
 * products</li> <li>any number logging file locations</li> <li>any number parameters</li> <li>a type string</li> <li>an
 * associated request file</li> </ul>
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class Request implements Serializable {

    public static final String METADATA_ELEM_NAME_PROCESSING_REQUEST = "processing_request";
    public static final String METADATA_ELEM_NAME_INPUT_PRODUCTS = "input_products";
    public static final String METADATA_ATTRIB_NAME_PREFIX_INPUT_PRODUCT = "input_product.";
    public static final String METADATA_ATTRIB_NAME_PREFIX_OUTPUT_PRODUCT = "output_product.";
    public static final String METADATA_ELEM_NAME_OUTPUT_PRODUCTS = "output_products";
    public static final String METADATA_ELEM_NAME_PARAMETERS = "parameters";

    public static final String PREFIX_QUALIFIER = "param_qualifier.";
    public static final String METADATA_ATTRIB_NAME_VALUE = "value";

    private Vector<ProductRef> _inputProducts = new Vector<ProductRef>();
    private Vector<ProductRef> _outputProducts = new Vector<ProductRef>();
    private Vector<Parameter> _parameters = new Vector<Parameter>();
    private Vector<File> _logFileLocations = new Vector<File>();
    private String _type;
    private File _requestFile;

    /**
     * Constructs a new and empty request.
     */
    public Request() {
        _type = "";
    }

    /**
     * Constructs a new request with the given input products, output products and processing parameters.
     *
     * @param inputProducts  array of input products
     * @param outputProducts array of output products
     * @param parameters     array of parameters
     */
    public Request(ProductRef[] inputProducts,
                   ProductRef[] outputProducts,
                   Parameter[] parameters) {
        this();
        if (inputProducts != null) {
            _inputProducts.addAll(Arrays.asList(inputProducts));
        }

        if (outputProducts != null) {
            _outputProducts.addAll(Arrays.asList(outputProducts));
        }

        if (parameters != null) {
            _parameters.addAll(Arrays.asList(parameters));
        }
    }

    /**
     * Adds an input product to the request.
     *
     * @param product the product to be added
     *
     * @throws IllegalArgumentException when null is added
     */
    public void addInputProduct(ProductRef product) {
        Guardian.assertNotNull("product", product);
        _inputProducts.add(product);
    }

    /**
     * Removes an input product from the request.
     *
     * @param product the product to be removed
     */
    public void removeInputProduct(ProductRef product) {
        _inputProducts.remove(product);
    }

    /**
     * Removes all input products from the request
     */
    public void clearInputProducts() {
        _inputProducts.clear();
    }

    /**
     * Returns the number of input products contained in the request.
     */
    public int getNumInputProducts() {
        return _inputProducts.size();
    }

    /**
     * Retrieves the input product at a given index.
     *
     * @param index the product index
     *
     * @throws ArrayIndexOutOfBoundsException
     */
    public ProductRef getInputProductAt(int index) {
        return _inputProducts.get(index);
    }

    /**
     * Adds an output product to the request.
     *
     * @param product the product to be added
     *
     * @throws IllegalArgumentException when null is added
     */
    public void addOutputProduct(ProductRef product) {
        Guardian.assertNotNull("product", product);
        _outputProducts.add(product);
    }

    /**
     * Removes an output product from the request.
     *
     * @param product the product to be removed
     */
    public void removeOutputProduct(ProductRef product) {
        _outputProducts.remove(product);
    }

    /**
     * Returns the number of output products contained in the request.
     */
    public int getNumOutputProducts() {
        return _outputProducts.size();
    }

    /**
     * Retrieves the output product at a given index.
     *
     * @param index the product index
     *
     * @throws ArrayIndexOutOfBoundsException
     */
    public ProductRef getOutputProductAt(int index) {
        return _outputProducts.get(index);
    }

    /**
     * Adds a logging file location to the curreent request.
     *
     * @param logFile the logging file location.
     *
     * @throws IllegalArgumentException on null argument
     */
    public void addLogFileLocation(File logFile) {
        Guardian.assertNotNull("logFile", logFile);
        _logFileLocations.add(logFile);
    }

    /**
     * Removes a logging file location from the request.
     *
     * @param logFile the logging file location to be removed
     */
    public void removeLogFileLocation(File logFile) {
        _logFileLocations.remove(logFile);
    }

    /**
     * Removes all logging file locations set in the request
     */
    public void clearLogFileLocations() {
        _logFileLocations.clear();
    }

    /**
     * Returns the number of logging file locations contained in the request.
     */
    public int getNumLogFileLocations() {
        return _logFileLocations.size();
    }


    /**
     * Retrieves the logging file location at a given index.
     *
     * @param index the logging file location index
     *
     * @throws ArrayIndexOutOfBoundsException
     */
    public File getLogFileLocationAt(int index) {
        return _logFileLocations.get(index);
    }

    /**
     * Adds a parameter to the request.
     *
     * @param parameter the parameter to be added
     *
     * @throws IllegalArgumentException when adding <code>null</code> parameter
     */
    public void addParameter(Parameter parameter) {
        Guardian.assertNotNull("parameter", parameter);
        _parameters.add(parameter);
    }

    /**
     * Removes a parameter from the request.
     *
     * @param parameter the parameter to be removed
     */
    public void removeParameter(Parameter parameter) {
        _parameters.remove(parameter);
    }

    /**
     * Returns the number of parameters contained in the request.
     */
    public int getNumParameters() {
        return _parameters.size();
    }

    /**
     * Retrieves the parameter at a given index.
     *
     * @param index the parameter index
     *
     * @throws ArrayIndexOutOfBoundsException
     */
    public Parameter getParameterAt(int index) {
        return _parameters.get(index);
    }

    /**
     * Retrieves a parameter by name.
     *
     * @param name the parameter name to be found. Must not be null or empty
     *
     * @return a parameter with the given name or <code>null</code> if no parameter with the given name is found.
     */
    public Parameter getParameter(String name) {
        Guardian.assertNotNullOrEmpty("name", name);
        for (final Parameter param : _parameters) {
            if (ObjectUtils.equalObjects(param.getName(), name)) {
                return param;
            }
        }
        return null;
    }

    /**
     * Retrieves an array of all the parameters of this request.
     *
     * @return an array of parameter. Returns never null.
     */
    public Parameter[] getAllParameters() {
        final int numParameters = getNumParameters();
        final Parameter[] parameters = new Parameter[numParameters];
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = getParameterAt(i);
        }
        return parameters;
    }

    /**
     * Retrieves the type of request.
     */
    public String getType() {
        return _type;
    }

    /**
     * Sets the type of request.
     *
     * @param type the request type
     *
     * @throws IllegalArgumentException when passing <code>null</code> as argument
     */
    public void setType(String type) {
        Guardian.assertNotNull("type", type);
        _type = type;
    }

    /**
     * Retrieves the request file from where this request originates. Or null when the file information is not set yet.
     */
    public File getFile() {
        return _requestFile;
    }

    /**
     * Sets the request file where this request originates
     *
     * @param requestFile the file
     */
    public void setFile(File requestFile) {
        _requestFile = requestFile;
    }

    /**
     * Checks if the given processing request is of the given request type and throws a processor exception if not.
     *
     * @param request     the request for checking type
     * @param requestType the request type for check
     *
     * @throws ProcessorException       if the given request to check is null
     * @throws ProcessorException       on invalid request type
     * @throws IllegalArgumentException if the given requestType is <code>null</code> or empty
     */
    public static void checkRequestType(Request request, String requestType) throws ProcessorException {
        Guardian.assertNotNullOrEmpty("requestType", requestType);
        if (request == null) {
            throw new ProcessorException("Request must not be null.");
        }
        if (!request.isRequestType(requestType)) {
            throw new ProcessorException("Invalid request type: " + request.getType());
        }
    }

    /**
     * Tests whether or not this request is for the given request type.
     *
     * @param requestType the request type for check
     *
     * @return true if so, otherwise false.
     */
    public boolean isRequestType(String requestType) {
        Guardian.assertNotNullOrEmpty("requestType", requestType);
        return requestType.equalsIgnoreCase(getType());
    }

    /**
     * Converts this request to a metadata element.
     *
     * @return a metadata element representation of this processing request, never <code>null</code>.
     */
    public MetadataElement convertToMetadata() {
        MetadataElement requestElem = new MetadataElement(METADATA_ELEM_NAME_PROCESSING_REQUEST);
        addMetadataAttribute(requestElem, "path", getFile() != null ? getFile().getPath() : null);
        addMetadataAttribute(requestElem, "type", getType());
        for (int i = 0; i < getNumLogFileLocations(); i++) {
            addMetadataAttribute(requestElem, "log_file." + (i + 1), getLogFileLocationAt(i).getPath());
        }

        if (getNumInputProducts() > 0) {
            MetadataElement inputProductsElem = new MetadataElement(METADATA_ELEM_NAME_INPUT_PRODUCTS);
            requestElem.addElement(inputProductsElem);
            for (int i = 0; i < getNumInputProducts(); i++) {
                final String inpProdAttribName = METADATA_ATTRIB_NAME_PREFIX_INPUT_PRODUCT + (i + 1);
                ProductRef productRef = getInputProductAt(i);
                addProductAttribs(inputProductsElem, inpProdAttribName, productRef);
            }
        }

        if (getNumOutputProducts() > 0) {
            MetadataElement outputProductsElem = new MetadataElement(METADATA_ELEM_NAME_OUTPUT_PRODUCTS);
            requestElem.addElement(outputProductsElem);
            for (int i = 0; i < getNumOutputProducts(); i++) {
                ProductRef productRef = getOutputProductAt(i);
                final String outProdAttribName = METADATA_ATTRIB_NAME_PREFIX_OUTPUT_PRODUCT + (i + 1);
                addProductAttribs(outputProductsElem, outProdAttribName, productRef);
            }
        }

        if (getNumParameters() > 0) {
            MetadataElement paramsElement = new MetadataElement(METADATA_ELEM_NAME_PARAMETERS);
            requestElem.addElement(paramsElement);
            for (int i = 0; i < getNumParameters(); i++) {
                final Parameter parameter = getParameterAt(i);
                final Object value = parameter.getValue();
                ProductData productData;
                if (value instanceof String) {
                    productData = ProductData.createInstance((String) value);
                } else if (value instanceof Double) {
                    productData = ProductData.createInstance(new double[]{(Double) value});
                } else if (value instanceof Float) {
                    productData = ProductData.createInstance(new float[]{(Float) value});
                } else if (value instanceof Integer) {
                    productData = ProductData.createInstance(new int[]{(Integer) value});
                } else {
                    productData = ProductData.createInstance(parameter.getValueAsText());
                }
                if (productData != null) {
                    final String name = parameter.getName();
                    final Map qualifierProperties = parameter.getProperties().getProperties(PREFIX_QUALIFIER);
                    if (qualifierProperties.size() == 0) {
                        final MetadataAttribute attribute = new MetadataAttribute(name, productData, true);
                        final String physUnit = parameter.getProperties().getPhysicalUnit();
                        attribute.setUnit(physUnit);
                        final String description = parameter.getProperties().getDescription();
                        attribute.setDescription(description);
                        paramsElement.addAttribute(attribute);
                    } else {
                        final MetadataElement paramElement = new MetadataElement(name);
                        final MetadataAttribute valueAttribute = new MetadataAttribute(METADATA_ATTRIB_NAME_VALUE,
                                                                                       productData, true);
                        final String physUnit = parameter.getProperties().getPhysicalUnit();
                        valueAttribute.setUnit(physUnit);
                        final String description = parameter.getProperties().getDescription();
                        valueAttribute.setDescription(description);
                        paramElement.addAttribute(valueAttribute);
                        final Set qualifierSet = qualifierProperties.entrySet();
                        for (Object aQualifierSet : qualifierSet) {
                            Map.Entry qualifier = (Map.Entry) aQualifierSet;
                            String qualifierName = (String) qualifier.getKey();
                            qualifierName = qualifierName.substring(PREFIX_QUALIFIER.length());
                            final ProductData pd = ProductData.createInstance((String) qualifier.getValue());
                            paramElement.addAttribute(new MetadataAttribute(qualifierName, pd, true));
                        }
                        paramsElement.addElement(paramElement);
                    }
                }
            }
        }
        return requestElem;
    }

    public static void addProductAttribs(MetadataElement inputProductsElem, final String s, ProductRef productRef) {
        addMetadataAttribute(inputProductsElem, s + ".path", productRef.getFilePath());
        addMetadataAttribute(inputProductsElem, s + ".format", productRef.getFileFormat());
        addMetadataAttribute(inputProductsElem, s + ".typeID", productRef.getTypeId());
    }

    private static void addMetadataAttribute(MetadataElement element, String name, String value) {
        if (value != null) {
            element.addAttribute(new MetadataAttribute(name,
                                                       ProductData.createInstance(value),
                                                       true));
        }
    }
}


